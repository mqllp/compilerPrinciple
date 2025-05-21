package optimization.cfg;

import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;
import optimization.lattice.LatticeValue;
import java.util.*;

public class ControlFlowGraph {
    private LLVMValueRef function;
    private Map<LLVMBasicBlockRef, BasicBlock> blockMap = new HashMap<>();
    private BasicBlock entryBlock;
    private List<Instruction> instructions = new ArrayList<>();
    private Map<LLVMValueRef, Instruction> instructionMap = new HashMap<>();

    public ControlFlowGraph(LLVMValueRef function) {
        this.function = function;
        buildCFG();
    }

    private void buildCFG() {
        // 1. 创建所有基本块
        LLVMBasicBlockRef block = LLVMGetFirstBasicBlock(function);
        while (block != null) {
            BasicBlock bb = new BasicBlock(block);
            blockMap.put(block, bb);

            if (LLVMGetFirstBasicBlock(function) == block) {
                entryBlock = bb;
            }

            block = LLVMGetNextBasicBlock(block);
        }

        // 2. 建立基本块之间的连接关系
        for (BasicBlock bb : blockMap.values()) {
            LLVMBasicBlockRef llvmBlock = bb.getLlvmBlock();
            LLVMValueRef terminator = LLVMGetBasicBlockTerminator(llvmBlock);

            if (terminator != null) {
                int numSuccessors = LLVMGetNumSuccessors(terminator);
                for (int i = 0; i < numSuccessors; i++) {
                    LLVMBasicBlockRef successor = LLVMGetSuccessor(terminator, i);
                    BasicBlock successorBB = blockMap.get(successor);
                    bb.addSuccessor(successorBB);
                }
            }
        }

        // 3. 处理每个基本块中的指令并建立指令间依赖关系
        for (BasicBlock bb : blockMap.values()) {
            LLVMBasicBlockRef llvmBlock = bb.getLlvmBlock();
            LLVMValueRef instr = LLVMGetFirstInstruction(llvmBlock);

            Instruction prevInstruction = null;

            while (instr != null) {
                Instruction instruction = new Instruction(instr, bb);
                bb.addInstruction(instruction);
                instructions.add(instruction);
                instructionMap.put(instr, instruction);

                // 指令间的顺序依赖
                if (prevInstruction != null) {
                    prevInstruction.addSuccessor(instruction);
                }

                prevInstruction = instruction;
                instr = LLVMGetNextInstruction(instr);
            }
        }

        // 4. 建立指令间的数据依赖关系
        for (Instruction instruction : instructions) {
            int numOperands = instruction.getNumOperands();
            for (int i = 0; i < numOperands; i++) {
                LLVMValueRef operand = instruction.getOperand(i);
                Instruction defInstr = instructionMap.get(operand);
                if (defInstr != null) {
                    defInstr.addSuccessor(instruction);
                }
            }
        }
    }



    public void runConstantPropagation() {
        // 工作表算法实现常量传播
        Queue<Instruction> workList = new LinkedList<>();
        Map<LLVMValueRef, LatticeValue> valueMap = new HashMap<>();
        Map<LLVMValueRef, LatticeValue> memoryMap = new HashMap<>();

        // 初始化常量值
        for (Instruction instr : instructions) {
            // 先检查所有直接常量赋值，如 store i32 1, i32* %x
            if (LLVMGetInstructionOpcode(instr.getLlvmInstruction()) == LLVMStore) {
                LLVMValueRef valueOp = instr.getOperand(0);
                LLVMValueRef ptrOp = instr.getOperand(1);

                if (LLVMIsConstant(valueOp) != 0) {
                    if (LLVMIsAConstantInt(valueOp) != null) {
                        long constVal = LLVMConstIntGetSExtValue(valueOp);
                        memoryMap.put(ptrOp, new LatticeValue(LatticeValue.ValueType.CONSTANT, constVal));
                    }
                }
            }

            // 初始化指令格值
            instr.setLatticeValue(new LatticeValue());
            workList.add(instr);
        }

        // 固定点迭代
        while (!workList.isEmpty()) {
            Instruction instr = workList.poll();
            LLVMValueRef instrRef = instr.getLlvmInstruction();
            int opcode = LLVMGetInstructionOpcode(instrRef);

            // 处理load指令：从内存映射中加载值
            if (opcode == LLVMLoad) {
                LLVMValueRef ptrOp = instr.getOperand(0);
                if (memoryMap.containsKey(ptrOp)) {
                    LatticeValue loadedValue = memoryMap.get(ptrOp);
                    if (!loadedValue.equals(instr.getOutValue())) {
                        instr.setLatticeValue(loadedValue);
                        valueMap.put(instrRef, loadedValue);

                        // 将所有使用此load结果的指令加入工作表
                        for (Instruction user : instr.getSuccessors()) {
                            workList.add(user);
                        }
                    }
                    continue;
                }
            }

            // 处理二元运算指令
            if (opcode == LLVMAdd || opcode == LLVMSub || opcode == LLVMMul ||
                    opcode == LLVMSDiv || opcode == LLVMSRem) {
                LLVMValueRef op1 = instr.getOperand(0);
                LLVMValueRef op2 = instr.getOperand(1);

                LatticeValue val1 = getOperandValue(op1, valueMap);
                LatticeValue val2 = getOperandValue(op2, valueMap);

                if (val1.getType() == LatticeValue.ValueType.CONSTANT &&
                        val2.getType() == LatticeValue.ValueType.CONSTANT) {
                    long result = computeConstantResult(opcode, val1.getConstantValue(), val2.getConstantValue());
                    LatticeValue newValue = new LatticeValue(LatticeValue.ValueType.CONSTANT, result);

                    if (!newValue.equals(instr.getOutValue())) {
                        instr.setLatticeValue(newValue);
                        valueMap.put(instrRef, newValue);

                        for (Instruction user : instr.getSuccessors()) {
                            workList.add(user);
                        }
                    }
                    continue;
                }
            }

            // 处理store指令：更新内存映射
            if (opcode == LLVMStore) {
                LLVMValueRef valueOp = instr.getOperand(0);
                LLVMValueRef ptrOp = instr.getOperand(1);

                LatticeValue valueToStore = getOperandValue(valueOp, valueMap);

                if (valueToStore.getType() == LatticeValue.ValueType.CONSTANT) {
                    memoryMap.put(ptrOp, valueToStore);

                    // 找到所有从该地址加载的load指令并更新
                    for (Instruction loadInstr : instructions) {
                        if (LLVMGetInstructionOpcode(loadInstr.getLlvmInstruction()) == LLVMLoad) {
                            if (ptrOp.equals(loadInstr.getOperand(0))) {
                                if (!valueToStore.equals(loadInstr.getOutValue())) {
                                    loadInstr.setLatticeValue(valueToStore);
                                    valueMap.put(loadInstr.getLlvmInstruction(), valueToStore);
                                    workList.add(loadInstr);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private LatticeValue getOperandValue(LLVMValueRef op, Map<LLVMValueRef, LatticeValue> valueMap) {
        // 如果是常量
        if (LLVMIsConstant(op) != 0) {
            if (LLVMIsAConstantInt(op) != null) {
                long value = LLVMConstIntGetSExtValue(op);
                return new LatticeValue(LatticeValue.ValueType.CONSTANT, value);
            }
        }

        // 如果是已计算的值
        if (valueMap.containsKey(op)) {
            return valueMap.get(op);
        }

        return new LatticeValue();
    }

    private long computeConstantResult(int opcode, long val1, long val2) {
        switch (opcode) {
            case LLVMAdd: return val1 + val2;
            case LLVMSub: return val1 - val2;
            case LLVMMul: return val1 * val2;
            case LLVMSDiv: return val2 != 0 ? val1 / val2 : 0;
            case LLVMSRem: return val2 != 0 ? val1 % val2 : 0;
            default: return 0;
        }
    }

    public BasicBlock getEntryBlock() {
        return entryBlock;
    }

    public Collection<BasicBlock> getAllBasicBlocks() {
        return blockMap.values();
    }

    public List<Instruction> getAllInstructions() {
        return instructions;
    }

    public Map<LLVMValueRef, LatticeValue> getConstantValues() {
        Map<LLVMValueRef, LatticeValue> constants = new HashMap<>();
        for (Instruction instr : instructions) {
            if (instr.isConstantValue()) {
                constants.put(instr.getLlvmInstruction(), instr.getOutValue());
            }
        }
        return constants;
    }
}