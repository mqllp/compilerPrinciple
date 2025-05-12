/*

import org.bytedeco.llvm.LLVM.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.bytedeco.llvm.global.LLVM.*;

public class RISCVGenerator {
    // 核心组件
    private final ModuleProcessor moduleProcessor;
    private final CodeGenerator codeGenerator;
    private final RegisterManager registerManager;
    private final MemoryManager memoryManager;
    private final LifetimeAnalyzer lifetimeAnalyzer;

    // 模块与输出相关信息
    private final LLVMModuleRef module;
    private final String targetFilePath;

    // 常量定义
    private static final int MAX_STACK_SIZE = 16000;
    private static final String SYSCALL_EXIT = "93";

    public RISCVGenerator(LLVMModuleRef module, String outputPath) {
        this.module = module;
        this.targetFilePath = outputPath;

        // 初始化子组件
        this.registerManager = new RegisterManager();
        this.memoryManager = new MemoryManager(MAX_STACK_SIZE);
        this.lifetimeAnalyzer = new LifetimeAnalyzer();
        this.codeGenerator = new CodeGenerator();
        this.moduleProcessor = new ModuleProcessor();
    }

    public void generate() {
        moduleProcessor.processGlobalVariables();
        moduleProcessor.processFunctions();
        writeToFile();
    }

    private void writeToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFilePath))) {
            writer.write(codeGenerator.getAssemblyCode());
        } catch (IOException e) {
            System.err.println("Failed to write assembly to file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 代码生成器 - 生成RISC-V汇编代码
    private class CodeGenerator {
        private final StringBuilder assemblyCode = new StringBuilder();

        // 生成标签
        public void emitLabel(String label) {
            assemblyCode.append(label).append(":\n");
        }

        // 生成注释
        public void emitComment(String comment) {
            assemblyCode.append("# ").append(comment).append("\n");
        }

        // 生成无操作数指令
        public void emit(String instruction) {
            assemblyCode.append("\t").append(instruction).append("\n");
        }

        // 生成有1个操作数的指令
        public void emit1(String instruction, String op) {
            assemblyCode.append("\t").append(instruction).append(" ").append(op).append("\n");
        }

        // 生成有2个操作数的指令
        public void emit2(String instruction, String op1, String op2) {
            assemblyCode.append("\t").append(instruction)
                    .append(" ").append(op1)
                    .append(", ").append(op2)
                    .append("\n");
        }

        // 生成有3个操作数的指令
        public void emit3(String instruction, String op1, String op2, String op3) {
            assemblyCode.append("\t").append(instruction)
                    .append(" ").append(op1)
                    .append(", ").append(op2)
                    .append(", ").append(op3)
                    .append("\n");
        }

        // 生成内存加载指令
        public void emitLoad(String reg, int offset) {
            emit2("lw", reg, offset + "(sp)");
        }

        // 生成内存存储指令
        public void emitStore(String reg, int offset) {
            emit2("sw", reg, offset + "(sp)");
        }

        // 生成数据段
        public void emitDataSection() {
            assemblyCode.append(".data\n");
        }

        // 生成代码段
        public void emitTextSection() {
            assemblyCode.append(".text\n");
        }

        // 生成全局变量声明
        public void emitGlobal(String name) {
            emit1(".globl", name);
        }

        // 生成数据定义
        public void emitWord(String value) {
            emit1(".word", value);
        }

        // 生成新行
        public void emitNewLine() {
            assemblyCode.append("\n");
        }

        // 获取生成的汇编代码
        public String getAssemblyCode() {
            return assemblyCode.toString();
        }
    }

    // 指令处理器 - 使用策略模式处理不同类型的LLVM指令
    private interface InstructionHandler {
        void process(LLVMValueRef instruction);
    }

    // 模块处理器 - 处理LLVM模块，包括全局变量和函数
    private class ModuleProcessor {
        private final Map<Integer, InstructionHandler> instructionHandlers;

        public ModuleProcessor() {
            instructionHandlers = createInstructionHandlers();
        }

        private Map<Integer, InstructionHandler> createInstructionHandlers() {
            Map<Integer, InstructionHandler> handlers = new HashMap<>();
            handlers.put(LLVMRet, this::handleReturn);
            handlers.put(LLVMBr, this::handleBranch);
            handlers.put(LLVMAdd, (inst) -> handleBinaryOperation(inst, "add"));
            handlers.put(LLVMSub, (inst) -> handleBinaryOperation(inst, "sub"));
            handlers.put(LLVMMul, (inst) -> handleBinaryOperation(inst, "mul"));
            handlers.put(LLVMSDiv, (inst) -> handleBinaryOperation(inst, "div"));
            handlers.put(LLVMSRem, (inst) -> handleBinaryOperation(inst, "rem"));
            handlers.put(LLVMAlloca, this::handleAlloca);
            handlers.put(LLVMLoad, this::handleLoad);
            handlers.put(LLVMStore, this::handleStore);
            handlers.put(LLVMICmp, this::handleICmp);
            handlers.put(LLVMZExt, this::handleZExt);
            return handlers;
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
                if (requiredStackSize > 0) {
                    codeGenerator.emit2("addi", "sp", "sp");
                }

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
                        memoryManager.releaseDeadVariables(lineNumber);

                        lineNumber++;
                    }
                }
            }
        }

        private void processInstruction(LLVMValueRef instruction) {
            int opcode = LLVMGetInstructionOpcode(instruction);
            InstructionHandler handler = instructionHandlers.get(opcode);

            if (handler != null) {
                handler.process(instruction);
            } else {
                codeGenerator.emitComment("Unsupported instruction: " + opcode);
            }
        }

        // 以下是各种指令的处理方法

        private void handleReturn(LLVMValueRef inst) {
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
                codeGenerator.emit2("addi", "sp", "sp");
            }

            codeGenerator.emit2("li", "a7", SYSCALL_EXIT);
            codeGenerator.emit("ecall");
        }

        private void handleBranch(LLVMValueRef inst) {
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
                codeGenerator.emit2("bnez", condReg, trueLabel);
                codeGenerator.emit1("j", falseLabel);
            }
        }

        private void handleBinaryOperation(LLVMValueRef inst, String operation) {
            String resultVar = LLVMGetValueName(inst).getString();
            LLVMValueRef op1 = LLVMGetOperand(inst, 0);
            LLVMValueRef op2 = LLVMGetOperand(inst, 1);

            String resultReg = registerManager.allocate(resultVar);
            String op1Reg, op2Reg;

            // 处理第一个操作数
            if (registerManager.isConstant(op1)) {
                op1Reg = "t0";
                long value = LLVMConstIntGetSExtValue(op1);
                codeGenerator.emit2("li", op1Reg, String.valueOf(value));
            } else {
                String varName = LLVMGetValueName(op1).getString();
                op1Reg = registerManager.get(varName);
            }

            // 处理第二个操作数
            if (registerManager.isConstant(op2)) {
                op2Reg = "t1";
                long value = LLVMConstIntGetSExtValue(op2);
                codeGenerator.emit2("li", op2Reg, String.valueOf(value));
            } else {
                String varName = LLVMGetValueName(op2).getString();
                op2Reg = registerManager.get(varName);
            }

            // 执行操作
            codeGenerator.emit3(operation, resultReg, op1Reg, op2Reg);
        }

        private void handleAlloca(LLVMValueRef inst) {
            // 为局部变量分配寄存器
            String varName = LLVMGetValueName(inst).getString();
            registerManager.allocate(varName);
        }

        private void handleLoad(LLVMValueRef inst) {
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

        private void handleStore(LLVMValueRef inst) {
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

        private void handleICmp(LLVMValueRef inst) {
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

        private void handleZExt(LLVMValueRef inst) {
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

    // 寄存器管理器的实现
    private class RegisterManager {
        private final List<String> registerPool;
        private final Map<String, String> varToRegMap;
        private final Map<String, Integer> regPriority;
        private int nextPriority = 0;

        public RegisterManager() {
            // 初始化寄存器池，按照调用约定和使用频率排序
            registerPool = new ArrayList<>(Arrays.asList(
                    "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7",
                    "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11",
                    "t2", "t3", "t4", "t5", "t6"
            ));
            varToRegMap = new HashMap<>();
            regPriority = new HashMap<>();
        }

        public String allocate(String variable) {
            // 如果变量已经分配了寄存器，直接返回
            if (varToRegMap.containsKey(variable)) {
                touchRegister(variable);
                return varToRegMap.get(variable);
            }

            // 寻找空闲寄存器
            for (String reg : registerPool) {
                if (!varToRegMap.containsValue(reg)) {
                    varToRegMap.put(variable, reg);
                    regPriority.put(variable, nextPriority++);
                    return reg;
                }
            }

            // 没有空闲寄存器，执行溢出
            return spillAndReallocate(variable);
        }

        private String spillAndReallocate(String variable) {
            // 找出优先级最低的变量
            String victimVar = findLeastRecentlyUsed();
            String reg = varToRegMap.remove(victimVar);

            // 将受害者变量保存到栈中
            int offset = memoryManager.allocate(victimVar);
            codeGenerator.emitStore(reg, offset);

            // 分配寄存器给新变量
            varToRegMap.put(variable, reg);
            regPriority.put(variable, nextPriority++);
            return reg;
        }

        private String findLeastRecentlyUsed() {
            return regPriority.entrySet()
                    .stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
        }

        public String get(String variable) {
            if (varToRegMap.containsKey(variable)) {
                touchRegister(variable);
                return varToRegMap.get(variable);
            }

            // 变量在栈上，需要加载到寄存器
            String reg = allocate(variable);
            int offset = memoryManager.getOffset(variable);
            codeGenerator.emitLoad(reg, offset);
            return reg;
        }

        private void touchRegister(String variable) {
            regPriority.put(variable, nextPriority++);
        }

        public void free(String variable) {
            if (varToRegMap.containsKey(variable)) {
                varToRegMap.remove(variable);
                regPriority.remove(variable);
            }
        }

        public void releaseDead(int currentLine) {
            // 释放已经不活跃的变量
            Set<String> varsToRelease = new HashSet<>();

            for (String var : varToRegMap.keySet()) {
                int[] lifetime = lifetimeAnalyzer.getLifetimeFor(var);
                if (lifetime != null && lifetime[1] < currentLine) {
                    varsToRelease.add(var);
                }
            }

            for (String var : varsToRelease) {
                free(var);
            }
        }

        public boolean isConstant(LLVMValueRef value) {
            return LLVMIsAConstantInt(value) != null;
        }
    }

    // 其他方法实现补充
    private class MemoryManager {
        private final Map<String, Integer> variableOffsets;
        private final boolean[] stackSlots;
        private final int capacity;

        public MemoryManager(int capacity) {
            this.variableOffsets = new HashMap<>();
            this.stackSlots = new boolean[capacity / 4]; // 每个槽位4字节
            this.capacity = capacity;
        }

        public int allocate(String variable) {
            // 如果变量已经有栈位置，直接返回
            if (variableOffsets.containsKey(variable)) {
                return variableOffsets.get(variable);
            }

            // 寻找第一个空闲槽位
            for (int i = 0; i < stackSlots.length; i++) {
                if (!stackSlots[i]) {
                    int offset = i * 4;
                    variableOffsets.put(variable, offset);
                    stackSlots[i] = true;
                    return offset;
                }
            }

            throw new RuntimeException("栈溢出 - 没有可用槽位");
        }

        public int getOffset(String variable) {
            return variableOffsets.getOrDefault(variable, allocate(variable));
        }

        public void free(String variable) {
            if (variableOffsets.containsKey(variable)) {
                int offset = variableOffsets.get(variable);
                stackSlots[offset / 4] = false;
                variableOffsets.remove(variable);
            }
        }

        public void releaseDeadVariables(int currentLine) {
            Set<String> varsToRelease = new HashSet<>();

            for (String var : variableOffsets.keySet()) {
                int[] lifetime = lifetimeAnalyzer.getLifetimeFor(var);
                if (lifetime != null && lifetime[1] < currentLine) {
                    varsToRelease.add(var);
                }
            }

            for (String var : varsToRelease) {
                free(var);
            }
        }

        public int calculateStackSize() {
            int maxOffset = 0;
            for (int offset : variableOffsets.values()) {
                maxOffset = Math.max(maxOffset, offset);
            }
            return maxOffset + 4; // 确保至少有4字节空间
        }
    }

    private class LifetimeAnalyzer {
        private final Map<String, int[]> varLifetimes = new HashMap<>();
        private int totalLines = 0;

        public void analyze(LLVMValueRef function) {
            varLifetimes.clear();
            int lineNumber = 0;

            for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(function);
                 bb != null;
                 bb = LLVMGetNextBasicBlock(bb)) {

                for (LLVMValueRef inst = LLVMGetFirstInstruction(bb);
                     inst != null;
                     inst = LLVMGetNextInstruction(inst)) {

                    String defName = LLVMGetValueName(inst).getString();
                    if (!defName.isEmpty()) {
                        varLifetimes.put(defName, new int[]{lineNumber, lineNumber});
                    }

                    for (int i = 0; i < LLVMGetNumOperands(inst); i++) {
                        LLVMValueRef operand = LLVMGetOperand(inst, i);
                        if (operand != null && LLVMIsAConstantInt(operand) == null &&
                                LLVMIsABasicBlock(operand) == null && LLVMIsAGlobalValue(operand) == null) {

                            String name = LLVMGetValueName(operand).getString();
                            if (!name.isEmpty() && varLifetimes.containsKey(name)) {
                                varLifetimes.get(name)[1] = lineNumber;
                            }
                        }
                    }

                    lineNumber++;
                }
            }

            totalLines = lineNumber;
        }

        public int[] getLifetimeFor(String variable) {
            return varLifetimes.get(variable);
        }

        public int getMaxConcurrentLiveVariables() {
            int[] liveCounts = new int[totalLines];

            for (int[] lifetime : varLifetimes.values()) {
                for (int i = lifetime[0]; i <= lifetime[1]; i++) {
                    liveCounts[i]++;
                }
            }

            int maxLive = 0;
            for (int count : liveCounts) {
                maxLive = Math.max(maxLive, count);
            }

            return maxLive;
        }
    }
}


 */

