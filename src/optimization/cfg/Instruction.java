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
            LLVMValueRef lhs = getOperand(0);
            LLVMValueRef rhs = getOperand(1);

            // 获取操作数的值
            LatticeValue lhsValue = variableValues.getOrDefault(lhs, new LatticeValue());
            LatticeValue rhsValue = variableValues.getOrDefault(rhs, new LatticeValue());

            // 如果操作数都是常量，计算结果
            if (lhsValue.getType() == LatticeValue.ValueType.CONSTANT &&
                    rhsValue.getType() == LatticeValue.ValueType.CONSTANT) {
                Long lhsConstant = lhsValue.getConstantValue();
                Long rhsConstant = rhsValue.getConstantValue();

                if (opcode == LLVMAdd) {
                    return new LatticeValue(LatticeValue.ValueType.CONSTANT, lhsConstant + rhsConstant);
                } else if (opcode == LLVMSub) {
                    return new LatticeValue(LatticeValue.ValueType.CONSTANT, lhsConstant - rhsConstant);
                } else if (opcode == LLVMMul) {
                    return new LatticeValue(LatticeValue.ValueType.CONSTANT, lhsConstant * rhsConstant);
                } else if (opcode == LLVMSDiv && rhsConstant != 0) {
                    return new LatticeValue(LatticeValue.ValueType.CONSTANT, lhsConstant / rhsConstant);
                } else if (opcode == LLVMSRem && rhsConstant != 0) {
                    return new LatticeValue(LatticeValue.ValueType.CONSTANT, lhsConstant % rhsConstant);
                }
            }

            // 如果任一操作数不是常量，结果为NAC
            if (lhsValue.getType() == LatticeValue.ValueType.NAC ||
                    rhsValue.getType() == LatticeValue.ValueType.NAC) {
                return new LatticeValue(LatticeValue.ValueType.NAC, null);
            }
        }

        // 处理存储指令
        if (opcode == LLVMStore) {
            LLVMValueRef valueToStore = getOperand(0); // 存储的值
            LLVMValueRef ptr = getOperand(1);          // 存储地址

            // 如果存储的是常量值
            if (LLVMIsConstant(valueToStore) != 0) {
                if (LLVMIsAConstantInt(valueToStore) != null) {
                    long constVal = LLVMConstIntGetSExtValue(valueToStore);
                    LatticeValue latticeVal = new LatticeValue(LatticeValue.ValueType.CONSTANT, constVal);
                    updateVariableValue(ptr, latticeVal);
                    return latticeVal;
                }
            } else {
                // 获取要存储的非常量值
                LatticeValue storedVal = variableValues.getOrDefault(valueToStore, new LatticeValue());
                updateVariableValue(ptr, storedVal);
                return storedVal;
            }
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