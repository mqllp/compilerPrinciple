import instruction.*;
import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;
import instruction.InstructionHandler;

public class ModuleProcessor {
    private final LLVMModuleRef module;
    private final CodeGenerator codeGenerator;
    private final RegisterManager registerManager;
    private final MemoryManager memoryManager;
    private final LifetimeAnalyzer lifetimeAnalyzer;
    private final InstructionHandlerFactory handlerFactory;

    private static final String SYSCALL_EXIT = "93";

    public ModuleProcessor(LLVMModuleRef module, CodeGenerator codeGenerator,
                           RegisterManager registerManager, MemoryManager memoryManager,
                           LifetimeAnalyzer lifetimeAnalyzer) {
        this.module = module;
        this.codeGenerator = codeGenerator;
        this.registerManager = registerManager;
        this.memoryManager = memoryManager;
        this.lifetimeAnalyzer = lifetimeAnalyzer;
        this.handlerFactory = new InstructionHandlerFactory(this);
    }

    public void processGlobalVariables() {
        codeGenerator.emitDataSection();

        for (LLVMValueRef global = LLVMGetFirstGlobal(module);
             global != null;
             global = LLVMGetNextGlobal(global)) {

            String name = LLVMGetValueName(global).getString();
            codeGenerator.emitLabel(name);

            LLVMValueRef initializer = LLVMGetInitializer(global);
            long value = LLVMConstIntGetSExtValue(initializer);
            codeGenerator.emitWord(String.valueOf(value));
        }

        codeGenerator.emitNewLine();
    }

    public void processFunctions() {
        codeGenerator.emitTextSection();

        for (LLVMValueRef function = LLVMGetFirstFunction(module);
             function != null;
             function = LLVMGetNextFunction(function)) {

            String funcName = LLVMGetValueName(function).getString();
            if (funcName.isEmpty()) continue;

            codeGenerator.emitGlobal(funcName);
            codeGenerator.emitLabel(funcName);

            // 分析变量生命周期
            lifetimeAnalyzer.analyze(function);

            // 计算栈大小
            int maxLive = lifetimeAnalyzer.getMaxConcurrentLiveVariables();
            int requiredStackSize = Math.max((maxLive - registerManager.registerPool.size() + 3) * 4, 0);

            // 分配栈空间
            codeGenerator.emit3("addi", "sp", "sp", String.valueOf(-requiredStackSize));

            // 处理函数体
            int lineNumber = 0;
            for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(function);
                 bb != null;
                 bb = LLVMGetNextBasicBlock(bb)) {

                codeGenerator.emitLabel(LLVMGetBasicBlockName(bb).getString());

                for (LLVMValueRef inst = LLVMGetFirstInstruction(bb);
                     inst != null;
                     inst = LLVMGetNextInstruction(inst)) {

                    processInstruction(inst);
                    codeGenerator.emitNewLine();

                    // 释放不再需要的寄存器
                    registerManager.releaseDead(lineNumber);
                    memoryManager.releaseDeadVariables(lineNumber, lifetimeAnalyzer);

                    lineNumber++;
                }
            }
        }
    }

    public void processInstruction(LLVMValueRef instruction) {
        int opcode = LLVMGetInstructionOpcode(instruction);
        InstructionHandler handler = handlerFactory.getHandler(opcode);

        if (handler != null) {
            handler.process(instruction);
        } else {
            codeGenerator.emitComment("Unsupported instruction: " + opcode);
        }
    }

    // 指令处理方法，被InstructionHandlerFactory调用
    public void handleReturn(LLVMValueRef inst) {
        int operandCount = LLVMGetNumOperands(inst);
        if (operandCount > 0) {
            LLVMValueRef retVal = LLVMGetOperand(inst, 0);

            if (registerManager.isConstant(retVal)) {
                // 返回常量值
                long value = LLVMConstIntGetSExtValue(retVal);
                codeGenerator.emit2("li", "a0", String.valueOf(value));
            } else {
                // 返回变量值
                String varName = LLVMGetValueName(retVal).getString();
                String reg = registerManager.get(varName);
                codeGenerator.emit2("mv", "a0", reg);
            }
        }

        // 恢复栈指针并退出程序
        int stackSize = memoryManager.calculateStackSize();
        if (stackSize > 0) {
            codeGenerator.emit3("addi", "sp", "sp", String.valueOf(stackSize));
        } else {
            // 即使是 0 也要显示输出
            codeGenerator.emit3("addi", "sp", "sp", "0");
        }

        codeGenerator.emit2("li", "a7", SYSCALL_EXIT);
        codeGenerator.emit("ecall");
    }

    public void handleBranch(LLVMValueRef inst) {
        int operandCount = LLVMGetNumOperands(inst);

        if (operandCount == 1) {
            // 无条件分支
            LLVMValueRef target = LLVMGetOperand(inst, 0);
            String label = LLVMGetBasicBlockName(LLVMValueAsBasicBlock(target)).getString();
            codeGenerator.emit1("j", label);
        } else if (operandCount == 3) {
            // 条件分支
            LLVMValueRef condition = LLVMGetOperand(inst, 0);
            LLVMValueRef trueBlock = LLVMGetOperand(inst, 1);
            LLVMValueRef falseBlock = LLVMGetOperand(inst, 2);

            String condReg;
            if (registerManager.isConstant(condition)) {
                condReg = "t0";
                long value = LLVMConstIntGetSExtValue(condition);
                codeGenerator.emit2("li", condReg, String.valueOf(value));
            } else {
                String varName = LLVMGetValueName(condition).getString();
                condReg = registerManager.get(varName);
            }

            String trueLabel = LLVMGetBasicBlockName(LLVMValueAsBasicBlock(trueBlock)).getString();
            String falseLabel = LLVMGetBasicBlockName(LLVMValueAsBasicBlock(falseBlock)).getString();

            // 使用RISC-V的条件分支指令
            codeGenerator.emit2("beqz", condReg, trueLabel);  // 如果为0，跳转到假分支
            codeGenerator.emit1("j", falseLabel);               // 否则跳转到真分支
        }
    }

    public void handleBinaryOperation(LLVMValueRef inst, String operation) {
        String resultVar = LLVMGetValueName(inst).getString();
        LLVMValueRef op1 = LLVMGetOperand(inst, 0);
        LLVMValueRef op2 = LLVMGetOperand(inst, 1);

        String resultReg = registerManager.allocate(resultVar);
        String op1Reg, op2Reg;

        // 处理第一个操作数
        if (registerManager.isConstant(op1)) {
            op1Reg = resultReg.equals("t0") ? "t3" : "t0";
            long value = LLVMConstIntGetSExtValue(op1);
            codeGenerator.emit2("li", op1Reg, String.valueOf(value));
        } else {
            String varName = LLVMGetValueName(op1).getString();
            op1Reg = registerManager.get(varName);
            if (op1Reg == null || op1Reg.equals("null")) {
                op1Reg = "t0";
            }
        }

        // 处理第二个操作数
        if (registerManager.isConstant(op2)) {
            op2Reg = resultReg.equals("t1") ? "t4" : "t1";
            long value = LLVMConstIntGetSExtValue(op2);
            codeGenerator.emit2("li", op2Reg, String.valueOf(value));
        } else {
            String varName = LLVMGetValueName(op2).getString();
            op2Reg = registerManager.get(varName);
            if (op2Reg == null || op2Reg.equals("null")) {
                op2Reg = "t1";
            }
        }

        // 执行操作
        codeGenerator.emit3(operation, resultReg, op1Reg, op2Reg);
    }

    public void handleAlloca(LLVMValueRef inst) {
        // 为局部变量分配寄存器
        String varName = LLVMGetValueName(inst).getString();
        registerManager.allocate(varName);
    }

    public void handleLoad(LLVMValueRef inst) {
        String destVar = LLVMGetValueName(inst).getString();
        LLVMValueRef source = LLVMGetOperand(inst, 0);
        String destReg = registerManager.allocate(destVar);

        if (LLVMIsAGlobalValue(source) != null) {
            // 从全局变量加载
            String globalName = LLVMGetValueName(source).getString();
            codeGenerator.emit2("la", "t0", globalName);
            codeGenerator.emit2("lw", destReg, "0(t0)");
        } else {
            // 从局部变量加载
            String sourceVar = LLVMGetValueName(source).getString();
            String sourceReg = registerManager.get(sourceVar);
            codeGenerator.emit2("mv", destReg, sourceReg);
        }
    }

    public void handleStore(LLVMValueRef inst) {
        LLVMValueRef value = LLVMGetOperand(inst, 0);
        LLVMValueRef pointer = LLVMGetOperand(inst, 1);

        // 准备要存储的值
        String valueReg;
        if (registerManager.isConstant(value)) {
            valueReg = "t0";
            long constValue = LLVMConstIntGetSExtValue(value);
            codeGenerator.emit2("li", valueReg, String.valueOf(constValue));
        } else {
            String valueName = LLVMGetValueName(value).getString();
            valueReg = registerManager.get(valueName);
        }

        // 执行存储操作
        if (LLVMIsAGlobalValue(pointer) != null) {
            // 存储到全局变量
            String globalName = LLVMGetValueName(pointer).getString();
            codeGenerator.emit2("la", "t1", globalName);
            codeGenerator.emit2("sw", valueReg, "0(t1)");
        } else {
            // 存储到局部变量
            String pointerName = LLVMGetValueName(pointer).getString();
            String destReg = registerManager.allocate(pointerName);
            codeGenerator.emit2("mv", destReg, valueReg);
        }
    }

    public void handleICmp(LLVMValueRef inst) {
        String resultVar = LLVMGetValueName(inst).getString();
        LLVMValueRef lhs = LLVMGetOperand(inst, 0);
        LLVMValueRef rhs = LLVMGetOperand(inst, 1);
        String resultReg = registerManager.allocate(resultVar);

        // 获取操作数寄存器
        String lhsReg, rhsReg;
        if (registerManager.isConstant(lhs)) {
            lhsReg = "t0";
            long value = LLVMConstIntGetSExtValue(lhs);
            codeGenerator.emit2("li", lhsReg, String.valueOf(value));
        } else {
            String varName = LLVMGetValueName(lhs).getString();
            lhsReg = registerManager.get(varName);
        }

        if (registerManager.isConstant(rhs)) {
            rhsReg = "t1";
            long value = LLVMConstIntGetSExtValue(rhs);
            codeGenerator.emit2("li", rhsReg, String.valueOf(value));
        } else {
            String varName = LLVMGetValueName(rhs).getString();
            rhsReg = registerManager.get(varName);
        }

        // 处理比较谓词
        int predicate = LLVMGetICmpPredicate(inst);
        switch (predicate) {
            case LLVMIntEQ:  // ==
                codeGenerator.emit3("xor", resultReg, lhsReg, rhsReg);
                codeGenerator.emit2("seqz", resultReg, resultReg);
                break;
            case LLVMIntNE:  // !=
                codeGenerator.emit3("xor", resultReg, lhsReg, rhsReg);
                codeGenerator.emit2("snez", resultReg, resultReg);
                break;
            case LLVMIntSGT:  // >
                codeGenerator.emit3("slt", resultReg, rhsReg, lhsReg);
                break;
            case LLVMIntSGE:  // >=
                codeGenerator.emit3("slt", resultReg, lhsReg, rhsReg);
                codeGenerator.emit3("xori", resultReg, resultReg, "1");
                break;
            case LLVMIntSLT:  // <
                codeGenerator.emit3("slt", resultReg, lhsReg, rhsReg);
                break;
            case LLVMIntSLE:  // <=
                codeGenerator.emit3("slt", resultReg, rhsReg, lhsReg);
                codeGenerator.emit3("xori", resultReg, resultReg, "1");
                break;
            default:
                codeGenerator.emitComment("Unsupported comparison predicate: " + predicate);
        }
    }

    public void handleZExt(LLVMValueRef inst) {
        String resultVar = LLVMGetValueName(inst).getString();
        LLVMValueRef operand = LLVMGetOperand(inst, 0);
        String resultReg = registerManager.allocate(resultVar);

        if (registerManager.isConstant(operand)) {
            // 处理常量零扩展
            long value = LLVMConstIntGetSExtValue(operand);
            codeGenerator.emit2("li", resultReg, String.valueOf(value));
        } else {
            // 处理变量零扩展
            String srcVar = LLVMGetValueName(operand).getString();
            String srcReg = registerManager.get(srcVar);
            // 使用掩码确保高位为0
            codeGenerator.emit3("andi", resultReg, srcReg, "1");
        }
    }
}