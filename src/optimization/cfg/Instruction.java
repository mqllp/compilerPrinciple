package optimization.cfg;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;
import optimization.lattice.LatticeValue;
import java.util.*;

public class Instruction {
    private LLVMValueRef llvmInstruction;
    private List<Instruction> successors = new ArrayList<>();
    private List<Instruction> predecessors = new ArrayList<>();
    private BasicBlock parentBlock;
    private Map<LLVMValueRef, LatticeValue> variableValues = new HashMap<>();

    private LatticeValue inValue;
    private LatticeValue outValue;

    public Instruction(LLVMValueRef instruction, BasicBlock parent) {
        this.llvmInstruction = instruction;
        this.parentBlock = parent;
        this.inValue = new LatticeValue(); // 默认为UNDEF
        this.outValue = new LatticeValue(); // 默认为UNDEF
    }

    public void addSuccessor(Instruction successor) {
        if (!successors.contains(successor)) {
            successors.add(successor);
            successor.addPredecessor(this);
        }
    }

    public void addPredecessor(Instruction predecessor) {
        if (!predecessors.contains(predecessor)) {
            predecessors.add(predecessor);
        }
    }

    public boolean isConstantValue() {
        return outValue != null && outValue.getType() == LatticeValue.ValueType.CONSTANT;
    }

    public Long getConstantValue() {
        return outValue != null && outValue.getType() == LatticeValue.ValueType.CONSTANT ?
                outValue.getConstantValue() : null;
    }

    public void setLatticeValue(LatticeValue value) {
        this.outValue = value;
    }

    public LatticeValue getOutValue() {
        return outValue;
    }

    public LatticeValue getInValue() {
        return inValue;
    }

    public void setInValue(LatticeValue value) {
        this.inValue = value;
    }

    public LLVMValueRef getLlvmInstruction() {
        return llvmInstruction;
    }

    public int getOpcode() {
        return LLVMGetInstructionOpcode(llvmInstruction);
    }

    public boolean isAssignment() {
        int opcode = getOpcode();
        return opcode == LLVMAdd || opcode == LLVMSub || opcode == LLVMMul ||
                opcode == LLVMSDiv || opcode == LLVMSRem || opcode == LLVMLoad ||
                opcode == LLVMAlloca;
    }

    public LLVMValueRef getOperand(int index) {
        return LLVMGetOperand(llvmInstruction, index);
    }

    public int getNumOperands() {
        return LLVMGetNumOperands(llvmInstruction);
    }

    public LatticeValue evaluate() {
        // 根据指令类型计算输出格值
        int opcode = getOpcode();

        // 对于常量整数，直接返回其值
        if (LLVMIsConstant(llvmInstruction) != 0) {
            if (LLVMIsAConstantInt(llvmInstruction) != null) {
                long value = LLVMConstIntGetSExtValue(llvmInstruction);
                return new LatticeValue(LatticeValue.ValueType.CONSTANT, value);
            }
        }

        // 处理二元运算
        if (opcode == LLVMAdd || opcode == LLVMSub || opcode == LLVMMul ||
                opcode == LLVMSDiv || opcode == LLVMSRem) {

            LLVMValueRef op1 = getOperand(0);
            LLVMValueRef op2 = getOperand(1);

            LatticeValue val1 = variableValues.getOrDefault(op1, new LatticeValue());
            LatticeValue val2 = variableValues.getOrDefault(op2, new LatticeValue());

            // 如果任一操作数不是常量，结果是NAC
            if (val1.getType() != LatticeValue.ValueType.CONSTANT ||
                    val2.getType() != LatticeValue.ValueType.CONSTANT) {
                return new LatticeValue(LatticeValue.ValueType.NAC, null);
            }

            long constVal1 = val1.getConstantValue();
            long constVal2 = val2.getConstantValue();
            long result = 0;

            // 执行相应的操作
            switch (opcode) {
                case LLVMAdd: result = constVal1 + constVal2; break;
                case LLVMSub: result = constVal1 - constVal2; break;
                case LLVMMul: result = constVal1 * constVal2; break;
                case LLVMSDiv:
                    if (constVal2 == 0) return new LatticeValue(LatticeValue.ValueType.NAC, null);
                    result = constVal1 / constVal2;
                    break;
                case LLVMSRem:
                    if (constVal2 == 0) return new LatticeValue(LatticeValue.ValueType.NAC, null);
                    result = constVal1 % constVal2;
                    break;
            }

            return new LatticeValue(LatticeValue.ValueType.CONSTANT, result);
        }

        // 对于加载指令，返回存储的值
        if (opcode == LLVMLoad) {
            LLVMValueRef ptr = getOperand(0);
            return variableValues.getOrDefault(ptr, new LatticeValue());
        }

        // 默认情况下返回NAC
        return new LatticeValue(LatticeValue.ValueType.NAC, null);
    }

    public void updateVariableValue(LLVMValueRef var, LatticeValue value) {
        variableValues.put(var, value);
    }

    public BasicBlock getParentBlock() {
        return parentBlock;
    }

    public List<Instruction> getSuccessors() {
        return successors;
    }

    public List<Instruction> getPredecessors() {
        return predecessors;
    }

    @Override
    public String toString() {
        BytePointer ptr = new BytePointer();
        LLVMPrintValueToString(llvmInstruction);
        String result = ptr.getString();
        LLVMDisposeMessage(ptr);
        return result;
    }
}