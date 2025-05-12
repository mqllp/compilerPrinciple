import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;
import java.io.*;
import java.util.*;

public class RISCVGenerator {
    private LLVMModuleRef module;
    private String output;
    private AsmBuilder asmBuilder;
    private RegisterAllocator regAllocator;
    private Map<String, Integer> funcStackSize;

    // RISC-V寄存器名称
    private static final String[] REGISTERS = {
            "zero", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
            "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
            "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
            "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
    };

    public RISCVGenerator(LLVMModuleRef module, String output) {
        this.module = module;
        this.output = output;
        this.asmBuilder = new AsmBuilder();
        this.regAllocator = new RegisterAllocator();
        this.funcStackSize = new HashMap<>();
    }

    public void generate() {
        // 执行寄存器分配
        regAllocator.buildIntervals(module);
        regAllocator.allocate();

        // 生成数据段
        generateDataSection();

        // 生成代码段
        asmBuilder.op(".text");
        generateFunctions();

        // 写入输出文件
        writeToFile();
    }

    private void generateDataSection() {
        boolean hasGlobals = false;

        for (LLVMValueRef global = LLVMGetFirstGlobal(module);
             global != null;
             global = LLVMGetNextGlobal(global)) {

            if (!hasGlobals) {
                asmBuilder.op(".data");
                hasGlobals = true;
            }

            String name = LLVMGetValueName(global).getString();
            asmBuilder.op(".globl " + name);
            asmBuilder.buildLabel(name);

            // 处理初始化值
            LLVMValueRef initVal = LLVMGetInitializer(global);
            if (initVal != null && LLVMIsConstant(initVal) == 1) {
                long value = LLVMConstIntGetSExtValue(initVal);
                asmBuilder.op(".word " + value);
            } else {
                asmBuilder.op(".word 0");
            }
        }

        if (hasGlobals) {
            asmBuilder.newline();
        }
    }

    private void generateFunctions() {
        for (LLVMValueRef func = LLVMGetFirstFunction(module);
             func != null;
             func = LLVMGetNextFunction(func)) {

            // 跳过声明
            if (LLVMCountBasicBlocks(func) == 0) {
                continue;
            }

            String funcName = LLVMGetValueName(func).getString();
            asmBuilder.op(".globl " + funcName);
            asmBuilder.buildLabel(funcName);

            // 生成函数序言
            generateFunctionProlog(func, funcName);

            // 生成基本块
            generateBasicBlocks(func);
        }
    }

    private void generateFunctionProlog(LLVMValueRef func, String funcName) {
        // 计算函数需要的栈空间
        int stackSize = regAllocator.getStackSize();
        stackSize = (stackSize + 15) & ~15; // 16字节对齐
        funcStackSize.put(funcName, stackSize);

        // 即使栈大小为0也生成指令
        asmBuilder.op2("addi", "sp", "sp", "-" + stackSize);

        // 如果有栈空间需要保存返回地址和帧指针
        if (stackSize > 0) {
            asmBuilder.op2("sw", "ra", stackSize - 4 + "(sp)", "");
            asmBuilder.op2("sw", "s0", stackSize - 8 + "(sp)", "");
            asmBuilder.op2("addi", "s0", "sp", String.valueOf(stackSize));
        }
    }

    private void generateFunctionEpilog(String funcName) {
        int stackSize = funcStackSize.getOrDefault(funcName, 0);

        if (stackSize > 0) {
            // 恢复被调用者保存的寄存器（如有必要）
            asmBuilder.op1("lw", "ra", stackSize - 4 + "(sp)");
            asmBuilder.op1("lw", "s0", stackSize - 8 + "(sp)");
        }

        // 释放栈空间
        asmBuilder.op2("addi", "sp", "sp", String.valueOf(stackSize));

        // main函数特殊处理 - 使用ecall而不是ret
        if (funcName.equals("main")) {
            asmBuilder.op1("li", "a7", "93");  // exit系统调用号
            asmBuilder.op("ecall");
        } else {
            asmBuilder.op("ret");
        }
    }

    private void generateBasicBlocks(LLVMValueRef func) {
        String funcName = LLVMGetValueName(func).getString();

        for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(func);
             bb != null;
             bb = LLVMGetNextBasicBlock(bb)) {

            String bbName = LLVMGetBasicBlockName(bb).getString();
            asmBuilder.buildLabel(bbName);

            // 生成指令
            for (LLVMValueRef inst = LLVMGetFirstInstruction(bb);
                 inst != null;
                 inst = LLVMGetNextInstruction(inst)) {



                // 检查是否是返回指令

                // 在generateBasicBlocks方法中修改处理ret指令的部分
                if (LLVMGetInstructionOpcode(inst) == LLVMRet) {
                    if (LLVMGetNumOperands(inst) > 0) {
                        // 处理返回值
                        LLVMValueRef retVal = LLVMGetOperand(inst, 0);

                        if (LLVMIsConstant(retVal) == 1) {
                            // 常量返回值
                            long constVal = LLVMConstIntGetSExtValue(retVal);
                            asmBuilder.op1("li", "a0", String.valueOf(constVal));
                        } else {
                            // 处理变量返回值
                            Location retLoc = regAllocator.getLocation(retVal);
                            if (retLoc != null) {
                                if (retLoc.isRegister()) {
                                    asmBuilder.op1("mv", "a0", "x" + retLoc.getRegister());
                                } else if (retLoc.isStack()) {
                                    asmBuilder.op1("lw", "a0", retLoc.getOffset() + "(sp)");
                                } else if (retLoc.isGlobal()) {
                                    asmBuilder.op1("la", "t0", retLoc.getName());
                                    asmBuilder.op1("lw", "a0", "0(t0)");
                                }
                            }
                        }
                    }

                    // 生成函数结尾
                    generateFunctionEpilog(funcName);
                    continue; // 跳过下面的generateInstruction调用
                }

                generateInstruction(inst);
            }
        }
    }

    private void generateInstruction(LLVMValueRef inst) {
        int opcode = LLVMGetInstructionOpcode(inst);

        switch (opcode) {
            case LLVMAlloca:
                // 已由寄存器分配器处理栈分配
                break;

            case LLVMLoad:
                generateLoad(inst);
                break;

            case LLVMStore:
                generateStore(inst);
                break;

            case LLVMAdd:
                generateBinaryOp(inst, "add");
                break;

            case LLVMSub:
                generateBinaryOp(inst, "sub");
                break;

            case LLVMMul:
                generateBinaryOp(inst, "mul");
                break;

            case LLVMSDiv:
                generateBinaryOp(inst, "div");
                break;

            case LLVMSRem:
                generateBinaryOp(inst, "rem");
                break;

            case LLVMICmp:
                generateICmp(inst);
                break;

            case LLVMCall:
                generateCall(inst);
                break;

            case LLVMBr:
                generateBranch(inst);
                break;

            // 其他指令...
        }
    }

    private void generateLoad(LLVMValueRef inst) {
        LLVMValueRef ptr = LLVMGetOperand(inst, 0);
        Location destLoc = regAllocator.getLocation(inst);
        Location ptrLoc = regAllocator.getLocation(ptr);

        if (destLoc != null && ptrLoc != null) {
            int destReg = ensureRegister(destLoc);

            if (ptrLoc.isStack()) {
                // 从栈上加载
                asmBuilder.op1("lw", "x" + destReg, ptrLoc.getOffset() + "(sp)");
            } else if (ptrLoc.isGlobal()) {
                // 从全局变量加载
                asmBuilder.op1("la", "t0", ptrLoc.getName());
                asmBuilder.op1("lw", "x" + destReg, "0(t0)");
            } else if (ptrLoc.isRegister()) {
                // 从寄存器指向的内存加载
                asmBuilder.op1("lw", "x" + destReg, "0(x" + ptrLoc.getRegister() + ")");
            }
        }
    }

    private void generateStore(LLVMValueRef inst) {
        LLVMValueRef val = LLVMGetOperand(inst, 0);
        LLVMValueRef ptr = LLVMGetOperand(inst, 1);

        Location valLoc = regAllocator.getLocation(val);
        Location ptrLoc = regAllocator.getLocation(ptr);

        if (valLoc != null && ptrLoc != null) {
            int valReg;

            if (LLVMIsConstant(val) == 1) {
                // 常数值
                long constVal = LLVMConstIntGetSExtValue(val);
                asmBuilder.op1("li", "t0", String.valueOf(constVal));
                valReg = 5; // t0
            } else if (valLoc.isRegister()) {
                valReg = valLoc.getRegister();
            } else if (valLoc.isStack()) {
                asmBuilder.op1("lw", "t0", valLoc.getOffset() + "(sp)");
                valReg = 5; // t0
            } else {
                asmBuilder.op1("la", "t1", valLoc.getName());
                asmBuilder.op1("lw", "t0", "0(t1)");
                valReg = 5; // t0
            }

            if (ptrLoc.isStack()) {
                asmBuilder.op1("sw", "x" + valReg, ptrLoc.getOffset() + "(sp)");
            } else if (ptrLoc.isGlobal()) {
                asmBuilder.op1("la", "t1", ptrLoc.getName());
                asmBuilder.op1("sw", "x" + valReg, "0(t1)");
            } else if (ptrLoc.isRegister()) {
                asmBuilder.op1("sw", "x" + valReg, "0(x" + ptrLoc.getRegister() + ")");
            }
        }
    }

    private void generateBinaryOp(LLVMValueRef inst, String op) {
        LLVMValueRef lhs = LLVMGetOperand(inst, 0);
        LLVMValueRef rhs = LLVMGetOperand(inst, 1);

        Location destLoc = regAllocator.getLocation(inst);

        if (destLoc != null) {
            int destReg = ensureRegister(destLoc);
            int lhsReg = loadOperandToRegister(lhs, 6); // t1
            int rhsReg = loadOperandToRegister(rhs, 7); // t2

            asmBuilder.op2(op, "x" + destReg, "x" + lhsReg, "x" + rhsReg);

            if (destLoc.isStack()) {
                // 结果需要存回栈
                asmBuilder.op1("sw", "x" + destReg, destLoc.getOffset() + "(sp)");
            }
        }
    }

    private void generateICmp(LLVMValueRef inst) {
        LLVMValueRef lhs = LLVMGetOperand(inst, 0);
        LLVMValueRef rhs = LLVMGetOperand(inst, 1);

        Location destLoc = regAllocator.getLocation(inst);
        if (destLoc != null) {
            int destReg = ensureRegister(destLoc);
            int lhsReg = loadOperandToRegister(lhs, 6); // t1
            int rhsReg = loadOperandToRegister(rhs, 7); // t2

            // 获取比较类型
            int predicate = LLVMGetICmpPredicate(inst);

            switch (predicate) {
                case LLVMIntEQ:  // ==
                    asmBuilder.op2("xor", "x" + destReg, "x" + lhsReg, "x" + rhsReg);
                    asmBuilder.op1("seqz", "x" + destReg, "x" + destReg);
                    break;
                case LLVMIntNE:  // !=
                    asmBuilder.op2("xor", "x" + destReg, "x" + lhsReg, "x" + rhsReg);
                    asmBuilder.op1("snez", "x" + destReg, "x" + destReg);
                    break;
                case LLVMIntSGT:  // >
                    asmBuilder.op2("slt", "x" + destReg, "x" + rhsReg, "x" + lhsReg);
                    break;
                case LLVMIntSGE:  // >=
                    asmBuilder.op2("slt", "x" + destReg, "x" + lhsReg, "x" + rhsReg);
                    asmBuilder.op2("xori", "x" + destReg, "x" + destReg, "1");
                    break;
                case LLVMIntSLT:  // <
                    asmBuilder.op2("slt", "x" + destReg, "x" + lhsReg, "x" + rhsReg);
                    break;
                case LLVMIntSLE:  // <=
                    asmBuilder.op2("slt", "x" + destReg, "x" + rhsReg, "x" + lhsReg);
                    asmBuilder.op2("xori", "x" + destReg, "x" + destReg, "1");
                    break;
            }

            if (destLoc.isStack()) {
                // 结果需要存回栈
                asmBuilder.op1("sw", "x" + destReg, destLoc.getOffset() + "(sp)");
            }
        }
    }

    private void generateCall(LLVMValueRef inst) {
        LLVMValueRef callee = LLVMGetCalledValue(inst);
        String funcName = LLVMGetValueName(callee).getString();

        // 保存调用者保存的寄存器（如果需要）

        // 准备参数
        int numArgs = LLVMCountParams(callee);
        for (int i = 0; i < numArgs; i++) {
            LLVMValueRef arg = LLVMGetOperand(inst, i);
            int argReg = loadOperandToRegister(arg, 5 + i); // t0, t1, ...

            // 将参数值移至参数寄存器
            if (i < 8) { // RISC-V ABI: a0-a7用于前8个参数
                asmBuilder.op1("mv", "a" + i, "x" + argReg);
            } else {
                // 更多的参数需要放在栈上
                asmBuilder.op1("sw", "x" + argReg, (i - 8) * 4 + "(sp)");
            }
        }

        // 调用函数
        asmBuilder.op0("call", funcName);

        // 处理返回值（如果有）
        Location destLoc = regAllocator.getLocation(inst);
        if (destLoc != null) {
            int destReg = ensureRegister(destLoc);
            asmBuilder.op1("mv", "x" + destReg, "a0"); // 返回值在a0中

            if (destLoc.isStack()) {
                // 结果需要存回栈
                asmBuilder.op1("sw", "x" + destReg, destLoc.getOffset() + "(sp)");
            }
        }
    }

    private void generateBranch(LLVMValueRef inst) {
        int numOperands = LLVMGetNumOperands(inst);

        if (numOperands == 1) {
            // 无条件跳转
            LLVMValueRef target = LLVMGetOperand(inst, 0);
            String label = LLVMGetBasicBlockName(LLVMValueAsBasicBlock(target)).getString();
            asmBuilder.op0("j", label);
        } else {
            // 条件跳转
            LLVMValueRef cond = LLVMGetOperand(inst, 0);
            LLVMValueRef trueBlock = LLVMGetOperand(inst, 1);
            LLVMValueRef falseBlock = LLVMGetOperand(inst, 2);

            String trueLabel = LLVMGetBasicBlockName(LLVMValueAsBasicBlock(trueBlock)).getString();
            String falseLabel = LLVMGetBasicBlockName(LLVMValueAsBasicBlock(falseBlock)).getString();

            int condReg = loadOperandToRegister(cond, 5); // t0

            asmBuilder.op1("bnez", "x" + condReg, trueLabel);
            asmBuilder.op0("j", falseLabel);
        }
    }

    private int loadOperandToRegister(LLVMValueRef operand, int tempReg) {
        if (LLVMIsConstant(operand) == 1) {
            // 常量
            long value = LLVMConstIntGetSExtValue(operand);
            asmBuilder.op1("li", "x" + tempReg, String.valueOf(value));
            return tempReg;
        }

        Location loc = regAllocator.getLocation(operand);

        if (loc != null) {
            if (loc.isRegister()) {
                return loc.getRegister();
            } else if (loc.isStack()) {
                asmBuilder.op1("lw", "x" + tempReg, loc.getOffset() + "(sp)");
            } else if (loc.isGlobal()) {
                asmBuilder.op1("la", "t3", loc.getName());
                asmBuilder.op1("lw", "x" + tempReg, "0(t3)");
            }
            return tempReg;
        }

        return tempReg; // 默认返回临时寄存器
    }

    private int ensureRegister(Location loc) {
        if (loc.isRegister()) {
            return loc.getRegister();
        } else {
            return 5; // t0作为默认临时寄存器
        }
    }

    private void writeToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(output))) {
            writer.print(asmBuilder.getBuffer().toString());
        } catch (IOException e) {
            System.err.println("写入输出文件错误: " + e.getMessage());
        }
    }
}