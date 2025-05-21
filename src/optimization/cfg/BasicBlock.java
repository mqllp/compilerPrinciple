package optimization.cfg;

import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;
import java.util.*;

public class BasicBlock {
    private LLVMBasicBlockRef llvmBlock;
    private List<BasicBlock> successors = new ArrayList<>();
    private List<BasicBlock> predecessors = new ArrayList<>();
    private List<Instruction> instructions = new ArrayList<>();
    private String name;

    public BasicBlock(LLVMBasicBlockRef block) {
        this.llvmBlock = block;
        this.name = LLVMGetBasicBlockName(block).getString();
    }

    public void addInstruction(Instruction instruction) {
        instructions.add(instruction);
    }

    public void addSuccessor(BasicBlock successor) {
        if (!successors.contains(successor)) {
            successors.add(successor);
            successor.addPredecessor(this);
        }
    }

    public void addPredecessor(BasicBlock predecessor) {
        if (!predecessors.contains(predecessor)) {
            predecessors.add(predecessor);
        }
    }

    public LLVMBasicBlockRef getLlvmBlock() {
        return llvmBlock;
    }

    public List<BasicBlock> getSuccessors() {
        return successors;
    }

    public List<BasicBlock> getPredecessors() {
        return predecessors;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "BasicBlock{" + name + "}";
    }
}