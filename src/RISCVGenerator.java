import org.bytedeco.llvm.LLVM.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.bytedeco.llvm.global.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.LLVMGetValueName;

public class RISCVGenerator {
    private LLVMModuleRef module;
    private String outputPath;
    private AsmBuilder asmBuilder;

    private int totalLineNum;
    private int currentLineNum;
    private int stackSize;
    private int stackArraySize;

    private static final List<String> allRegisters = Arrays.asList(
            "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7",
            "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11",
            "t2", "t3", "t4", "t5", "t6"
    );

    private final String[] regAlloc = new String[allRegisters.size()];
    private final String[] stackAlloc = new String[16000];

    private final Map<String, int[]> varLifetime = new HashMap<>();

    public RISCVGenerator(LLVMModuleRef module, String outputPath) {
        this.module = module;
        this.outputPath = outputPath;
        this.asmBuilder = new AsmBuilder();
    }

    public void generate() {
        emitGlobalData();
        emitFunctions();
        writeOutput();
    }

    private void writeOutput() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write(asmBuilder.getStringBuffer().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void emitGlobalData() {
        asmBuilder.buildLabel(".data");
        for (LLVMValueRef global = LLVMGetFirstGlobal(module); global != null; global = LLVMGetNextGlobal(global)) {
            String name = LLVMGetValueName(global).getString();
            long value = LLVMConstIntGetSExtValue(LLVMGetInitializer(global));
            asmBuilder.buildLabel(name);
            asmBuilder.op0(".word", String.valueOf(value));
        }
        asmBuilder.newline();
    }

    private void emitFunctions() {
        asmBuilder.buildLabel(".text");
        for (LLVMValueRef func = LLVMGetFirstFunction(module); func != null; func = LLVMGetNextFunction(func)) {
            String funcName = LLVMGetValueName(func).getString();
            if (funcName.isEmpty()) continue;

            asmBuilder.op0(".globl", funcName);
            asmBuilder.buildLabel(funcName);

            analyzeLifetimes(func);
            stackSize = Math.max((getMaxLiveCount() - allRegisters.size() + 3) * 4, 0);
            stackArraySize = stackSize / 4;

            asmBuilder.op2("addi", "sp", "sp", String.valueOf(-stackSize));

            int line = 0;
            for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(func); bb != null; bb = LLVMGetNextBasicBlock(bb)) {
                asmBuilder.buildLabel(LLVMGetBasicBlockName(bb).getString());
                for (LLVMValueRef inst = LLVMGetFirstInstruction(bb); inst != null; inst = LLVMGetNextInstruction(inst)) {
                    handleInstruction(inst);
                    asmBuilder.newline();
                    releaseRegistersAt(line);
                    line++;
                    currentLineNum = line;
                }
            }
        }
    }

    private void analyzeLifetimes(LLVMValueRef func) {
        varLifetime.clear();
        int line = 0;
        for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(func); bb != null; bb = LLVMGetNextBasicBlock(bb)) {
            for (LLVMValueRef inst = LLVMGetFirstInstruction(bb); inst != null; inst = LLVMGetNextInstruction(inst)) {
                String def = LLVMGetValueName(inst).getString();
                if (!def.isEmpty()) varLifetime.put(def, new int[]{line, line});

                for (int i = 0; i < LLVMGetNumOperands(inst); i++) {
                    LLVMValueRef op = LLVMGetOperand(inst, i);
                    if (op != null && LLVMIsAGlobalValue(op) == null && LLVMIsABasicBlock(op) == null) {
                        String name = LLVMGetValueName(op).getString();
                        if (!name.isEmpty() && varLifetime.containsKey(name)) {
                            varLifetime.get(name)[1] = line;
                        }
                    }
                }
                line++;
            }
        }
        totalLineNum = line;
    }

    private int getMaxLiveCount() {
        int max = 0;
        for (int i = 0; i < totalLineNum; i++) {
            int live = 0;
            for (int[] range : varLifetime.values()) {
                if (range[0] <= i && range[1] >= i) live++;
            }
            max = Math.max(max, live);
        }
        return max;
    }

    private void handleInstruction(LLVMValueRef inst) {
        int opcode = LLVMGetInstructionOpcode(inst);
        int operandNum = LLVMGetNumOperands(inst);
        LLVMValueRef op1 = operandNum > 0 ? LLVMGetOperand(inst, 0) : null;
        LLVMValueRef op2 = operandNum > 1 ? LLVMGetOperand(inst, 1) : null;
        LLVMValueRef op3 = operandNum > 2 ? LLVMGetOperand(inst, 2) : null;

        switch (opcode) {
            case LLVMAlloca:
                allocateRegister(LLVMGetValueName(inst).getString());
                break;
            case LLVMStore:
                generateStore(op1, op2);
                break;
            case LLVMLoad:
                generateLoad(inst, op1);
                break;
            case LLVMRet:
                generateReturn(op1);
                break;
            case LLVMAdd:
            case LLVMSub:
            case LLVMMul:
            case LLVMSDiv:
            case LLVMSRem:
                generateBinary(inst, op1, op2, opcode);
                break;
            case LLVMICmp:
                generateICmp(inst, op1, op2);
                break;
            case LLVMBr:
                generateBranch(inst, op1, op2, op3);
                break;
            case LLVMZExt:
                generateZExt(inst, op1);
                break;
        }
    }

    private void generateStore(LLVMValueRef val, LLVMValueRef ptr) {
        String ptrName = LLVMGetValueName(ptr).getString();
        String valueReg = (LLVMIsAConstantInt(val) != null)
                ? "t0"
                : getRegister(LLVMGetValueName(val).getString());

        if (LLVMIsAConstantInt(val) != null) {
            asmBuilder.op1("li", valueReg, String.valueOf(LLVMConstIntGetSExtValue(val)));
        }

        int offset = allocStackSlot(ptrName);
        asmBuilder.op1("sw", valueReg, offset + "(sp)");
    }

    private void generateLoad(LLVMValueRef inst, LLVMValueRef ptr) {
        String ptrName = LLVMGetValueName(ptr).getString();
        String resultReg = allocateRegister(LLVMGetValueName(inst).getString());
        int offset = allocStackSlot(ptrName);
        asmBuilder.op1("lw", resultReg, offset + "(sp)");
    }

    private void generateReturn(LLVMValueRef retVal) {
        if (retVal != null) {
            if (LLVMIsAConstantInt(retVal) != null) {
                asmBuilder.op1("li", "a0", String.valueOf(LLVMConstIntGetSExtValue(retVal)));
            } else {
                asmBuilder.op1("mv", "a0", getRegister(LLVMGetValueName(retVal).getString()));
            }
        }
        asmBuilder.op2("addi", "sp", "sp", String.valueOf(stackSize));
        asmBuilder.op1("li", "a7", "93");
        asmBuilder.op("ecall");
    }

    private void generateBinary(LLVMValueRef inst, LLVMValueRef op1, LLVMValueRef op2, int opcode) {
        String regDst = allocateRegister(LLVMGetValueName(inst).getString());
        String regL = LLVMIsAConstantInt(op1) != null ? "t0" : getRegister(LLVMGetValueName(op1).getString());
        String regR = LLVMIsAConstantInt(op2) != null ? "t1" : getRegister(LLVMGetValueName(op2).getString());

        if (LLVMIsAConstantInt(op1) != null)
            asmBuilder.op1("li", regL, String.valueOf(LLVMConstIntGetSExtValue(op1)));
        if (LLVMIsAConstantInt(op2) != null)
            asmBuilder.op1("li", regR, String.valueOf(LLVMConstIntGetSExtValue(op2)));

        String opText;
        switch (opcode) {
            case LLVMAdd:
                opText = "add";
                break;
            case LLVMSub:
                opText = "sub";
                break;
            case LLVMMul:
                opText = "mul";
                break;
            case LLVMSDiv:
                opText = "div";
                break;
            case LLVMSRem:
                opText = "rem";
                break;
            default:
                throw new RuntimeException("Unsupported binary op");
        }
        asmBuilder.op2(opText, regDst, regL, regR);
    }

    private void generateICmp(LLVMValueRef inst, LLVMValueRef lhs, LLVMValueRef rhs) {
        String regL = LLVMIsAConstantInt(lhs) != null ? "t0" : getRegister(LLVMGetValueName(lhs).getString());
        String regR = LLVMIsAConstantInt(rhs) != null ? "t1" : getRegister(LLVMGetValueName(rhs).getString());
        String regDst = allocateRegister(LLVMGetValueName(inst).getString());

        if (LLVMIsAConstantInt(lhs) != null)
            asmBuilder.op1("li", regL, String.valueOf(LLVMConstIntGetSExtValue(lhs)));
        if (LLVMIsAConstantInt(rhs) != null)
            asmBuilder.op1("li", regR, String.valueOf(LLVMConstIntGetSExtValue(rhs)));

        switch (LLVMGetICmpPredicate(inst)) {
            case LLVMIntEQ:
                asmBuilder.op2("xor", regDst, regL, regR);
                asmBuilder.op1("seqz", regDst, regDst);
                break;
            case LLVMIntNE:
                asmBuilder.op2("xor", regDst, regL, regR);
                asmBuilder.op1("snez", regDst, regDst);
                break;
            case LLVMIntSGT:
                asmBuilder.op2("slt", regDst, regR, regL);
                break;
            case LLVMIntSGE:
                asmBuilder.op2("slt", regDst, regL, regR);
                asmBuilder.op2("xori", regDst, regDst, "1");
                break;
            case LLVMIntSLT:
                asmBuilder.op2("slt", regDst, regL, regR);
                break;
            case LLVMIntSLE:
                asmBuilder.op2("slt", regDst, regR, regL);
                asmBuilder.op2("xori", regDst, regDst, "1");
                break;
        }
    }

    private void generateBranch(LLVMValueRef inst, LLVMValueRef cond, LLVMValueRef ifTrue, LLVMValueRef ifFalse) {
        if (ifTrue != null && ifFalse != null) {
            String condReg = LLVMIsAConstantInt(cond) != null ? "t0" : getRegister(LLVMGetValueName(cond).getString());
            if (LLVMIsAConstantInt(cond) != null) {
                asmBuilder.op1("li", condReg, String.valueOf(LLVMConstIntGetSExtValue(cond)));
            }
            asmBuilder.op1("bnez", condReg, LLVMGetBasicBlockName(LLVMValueAsBasicBlock(ifTrue)).getString());
            asmBuilder.op0("j", LLVMGetBasicBlockName(LLVMValueAsBasicBlock(ifFalse)).getString());
        } else {
            asmBuilder.op0("j", LLVMGetBasicBlockName(LLVMValueAsBasicBlock(cond)).getString());
        }
    }

    private void generateZExt(LLVMValueRef inst, LLVMValueRef op1) {
        String reg = allocateRegister(LLVMGetValueName(inst).getString());
        if (LLVMIsAConstantInt(op1) != null) {
            asmBuilder.op1("li", reg, String.valueOf(LLVMConstIntGetSExtValue(op1)));
        } else {
            String src = getRegister(LLVMGetValueName(op1).getString());
            asmBuilder.op2("andi", reg, src, "1");
        }
    }

    private String allocateRegister(String var) {
        for (int i = 0; i < allRegisters.size(); i++) {
            if (regAlloc[i] == null) {
                regAlloc[i] = var;
                return allRegisters.get(i);
            }
        }
        int idx = spillRegister();
        regAlloc[idx] = var;
        return allRegisters.get(idx);
    }

    private String getRegister(String var) {
        for (int i = 0; i < allRegisters.size(); i++) {
            if (var.equals(regAlloc[i])) return allRegisters.get(i);
        }
        int idx = spillRegister();
        loadFromStack(var, idx);
        regAlloc[idx] = var;
        return allRegisters.get(idx);
    }

    private int spillRegister() {
        for (int i = 0; i < allRegisters.size(); i++) {
            if (regAlloc[i] != null) {
                int offset = allocStackSlot(regAlloc[i]);
                asmBuilder.op1("sw", allRegisters.get(i), offset + "(sp)");
                return i;
            }
        }
        return 0;
    }

    private void loadFromStack(String var, int regIndex) {
        for (int i = stackArraySize - 1; i >= 0; i--) {
            if (var.equals(stackAlloc[i])) {
                stackAlloc[i] = null;
                asmBuilder.op1("lw", allRegisters.get(regIndex), i * 4 + "(sp)");
                return;
            }
        }
    }

    private int allocStackSlot(String var) {
        for (int i = 0; i < stackAlloc.length; i++) {
            if (var.equals(stackAlloc[i])) return i * 4;
        }
        for (int i = 0; i < stackAlloc.length; i++) {
            if (stackAlloc[i] == null) {
                stackAlloc[i] = var;
                return i * 4;
            }
        }
        throw new RuntimeException("Stack overflow: " + var);
    }

    private void releaseRegistersAt(int line) {
        for (int i = 0; i < allRegisters.size(); i++) {
            String var = regAlloc[i];
            if (var != null && varLifetime.containsKey(var) && varLifetime.get(var)[1] < line) {
                regAlloc[i] = null;
            }
        }
        for (int i = 0; i < stackArraySize; i++) {
            String var = stackAlloc[i];
            if (var != null && varLifetime.containsKey(var) && varLifetime.get(var)[1] < line) {
                stackAlloc[i] = null;
            }
        }
    }
}
