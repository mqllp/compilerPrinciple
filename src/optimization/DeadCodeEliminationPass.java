package optimization;

import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;
import optimization.cfg.*;
import optimization.lattice.LatticeValue;

import java.util.*;

public class DeadCodeEliminationPass implements OptimizationPass {
    private boolean changed = false;

    @Override
    public LLVMModuleRef run(LLVMModuleRef module) {
        changed = false;

        LLVMValueRef function = LLVMGetFirstFunction(module);
        while (function != null) {
            if (LLVMIsDeclaration(function) == 0) {
                ControlFlowGraph cfg = new ControlFlowGraph(function);

                removeUnreachableBlocks(cfg, function);
                simplifyConstantBranches(cfg, function);
                eliminateRedundantJumps(cfg, function);
            }
            function = LLVMGetNextFunction(function);
        }

        return module;
    }

    // 使用 DFS 标记所有可达块，并删除不可达块
    private void removeUnreachableBlocks(ControlFlowGraph cfg, LLVMValueRef function) {
        Set<LLVMBasicBlockRef> reachable = new HashSet<>();
        Deque<LLVMBasicBlockRef> worklist = new ArrayDeque<>();

        LLVMBasicBlockRef entry = LLVMGetEntryBasicBlock(function);
        worklist.add(entry);

        while (!worklist.isEmpty()) {
            LLVMBasicBlockRef block = worklist.pop();
            if (!reachable.contains(block)) {
                reachable.add(block);
                LLVMValueRef term = LLVMGetBasicBlockTerminator(block);
                int numSuccessors = LLVMGetNumSuccessors(term);
                for (int i = 0; i < numSuccessors; i++) {
                    worklist.add(LLVMGetSuccessor(term, i));
                }
            }
        }

        // 删除不可达块
        LLVMBasicBlockRef block = LLVMGetFirstBasicBlock(function);
        while (block != null) {
            LLVMBasicBlockRef next = LLVMGetNextBasicBlock(block);
            if (!reachable.contains(block)) {
                // 删除指令
                LLVMValueRef instr = LLVMGetFirstInstruction(block);
                while (instr != null) {
                    LLVMValueRef nextInstr = LLVMGetNextInstruction(instr);
                    LLVMInstructionEraseFromParent(instr);
                    instr = nextInstr;
                }
                LLVMRemoveBasicBlockFromParent(block);
                changed = true;
                System.out.println("删除了不可达基本块");
            }
            block = next;
        }
    }

    // 简化 br i1 %cond, label %true, label %false 的常量条件分支
    private void simplifyConstantBranches(ControlFlowGraph cfg, LLVMValueRef function) {
        Map<LLVMValueRef, LatticeValue> constantValues = cfg.getConstantValues();

        for (Instruction instruction : cfg.getAllInstructions()) {
            LLVMValueRef instr = instruction.getLlvmInstruction();
            if (LLVMGetInstructionOpcode(instr) == LLVMBr && LLVMGetNumOperands(instr) == 3) {
                LLVMValueRef cond = LLVMGetOperand(instr, 0);

                boolean isConst = false;
                long condValue = 0;

                if (LLVMIsAConstantInt(cond) != null) {
                    isConst = true;
                    condValue = LLVMConstIntGetSExtValue(cond);
                } else {
                    LatticeValue val = constantValues.get(cond);
                    if (val != null && val.getType() == LatticeValue.ValueType.CONSTANT) {
                        isConst = true;
                        condValue = val.getConstantValue();
                    }
                }

                if (isConst) {
                    LLVMBasicBlockRef target = condValue != 0
                            ? LLVMValueAsBasicBlock(LLVMGetOperand(instr, 2)) // true
                            : LLVMValueAsBasicBlock(LLVMGetOperand(instr, 1)); // false

                    LLVMBuilderRef builder = LLVMCreateBuilder();
                    LLVMPositionBuilderBefore(builder, instr);
                    LLVMBuildBr(builder, target);
                    LLVMInstructionEraseFromParent(instr);
                    LLVMDisposeBuilder(builder);

                    changed = true;
                    System.out.println("简化了常量条件分支");
                }
            }
        }
    }

    // 合并只被一个前驱无条件跳转到的基本块，或优化中间跳转
    private void eliminateRedundantJumps(ControlFlowGraph cfg, LLVMValueRef function) {
        boolean modified;
        do {
            modified = false;
            for (BasicBlock block : cfg.getAllBasicBlocks()) {
                LLVMBasicBlockRef llvmBlock = block.getLlvmBlock();
                LLVMValueRef terminator = LLVMGetBasicBlockTerminator(llvmBlock);

                if (terminator != null && LLVMGetInstructionOpcode(terminator) == LLVMBr &&
                        LLVMGetNumOperands(terminator) == 1) {

                    LLVMBasicBlockRef target = LLVMValueAsBasicBlock(LLVMGetOperand(terminator, 0));
                    BasicBlock targetBB = null;

                    for (BasicBlock bb : cfg.getAllBasicBlocks()) {
                        if (bb.getLlvmBlock().equals(target)) {
                            targetBB = bb;
                            break;
                        }
                    }

                    if (targetBB != null && targetBB.getPredecessors().size() == 1 && targetBB != block) {
                        LLVMValueRef instr = LLVMGetFirstInstruction(target);
                        LLVMValueRef term = LLVMGetBasicBlockTerminator(target);
                        LLVMBuilderRef builder = LLVMCreateBuilder();
                        LLVMPositionBuilderBefore(builder, terminator);

                        while (instr != null && !instr.equals(term)) {
                            LLVMValueRef next = LLVMGetNextInstruction(instr);
                            LLVMInstructionRemoveFromParent(instr);
                            LLVMInsertIntoBuilder(builder, instr);
                            instr = next;
                        }

                        // 构建终结指令
                        if (term != null) {
                            if (LLVMGetInstructionOpcode(term) == LLVMBr && LLVMGetNumOperands(term) == 1) {
                                LLVMBuildBr(builder, LLVMValueAsBasicBlock(LLVMGetOperand(term, 0)));
                            } else if (LLVMGetInstructionOpcode(term) == LLVMRet) {
                                if (LLVMGetNumOperands(term) > 0) {
                                    LLVMBuildRet(builder, LLVMGetOperand(term, 0));
                                } else {
                                    LLVMBuildRetVoid(builder);
                                }
                            }
                        }

                        LLVMInstructionEraseFromParent(terminator);
                        LLVMDeleteBasicBlock(target);
                        LLVMDisposeBuilder(builder);

                        changed = true;
                        modified = true;
                        System.out.println("合并了冗余跳转块");
                        break;
                    }
                }
            }
        } while (modified);
    }

    @Override
    public boolean hasChanged() {
        return changed;
    }
}
