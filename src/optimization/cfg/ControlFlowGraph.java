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
        Queue<Instruction> workList = new LinkedList<>(instructions);
        Map<LLVMValueRef, LatticeValue> valueMap = new HashMap<>();

        // 初始化所有指令的格值为UNDEF
        for (Instruction instr : instructions) {
            instr.setLatticeValue(new LatticeValue());
        }

        while (!workList.isEmpty()) {
            Instruction instr = workList.poll();
            LatticeValue oldValue = instr.getOutValue();
            LatticeValue newValue = instr.evaluate();

            if (!oldValue.equals(newValue)) {
                instr.setLatticeValue(newValue);

                // 将所有受影响的指令加入工作表
                for (Instruction succ : instr.getSuccessors()) {
                    workList.add(succ);
                }

                // 更新变量值映射
                valueMap.put(instr.getLlvmInstruction(), newValue);

                // 更新依赖指令的变量值
                for (Instruction dependent : instructions) {
                    for (int i = 0; i < dependent.getNumOperands(); i++) {
                        LLVMValueRef operand = dependent.getOperand(i);
                        if (operand == instr.getLlvmInstruction()) {
                            dependent.updateVariableValue(operand, newValue);
                        }
                    }
                }
            }
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