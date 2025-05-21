package optimization;

import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;
import java.util.*;

public class UnusedVarEliminationPass implements OptimizationPass {
    private boolean changed = false;

    @Override
    public LLVMModuleRef run(LLVMModuleRef module) {
        changed = false;

        // 遍历模块中的所有函数
        LLVMValueRef function = LLVMGetFirstFunction(module);
        while (function != null) {
            // 跳过没有函数体的声明
            if (LLVMIsDeclaration(function) == 0) {
                // 收集局部变量定义和使用信息
                Map<LLVMValueRef, Boolean> varUsageMap = collectVarUsage(function);

                // 删除未使用的变量
                removeUnusedVariables(function, varUsageMap);
            }

            function = LLVMGetNextFunction(function);
        }

        return module;
    }

    private Map<LLVMValueRef, Boolean> collectVarUsage(LLVMValueRef function) {
        Map<LLVMValueRef, Boolean> varUsageMap = new HashMap<>();
        Map<LLVMValueRef, LLVMValueRef> loadResultToVar = new HashMap<>();

        // 第一遍：收集所有局部变量定义
        LLVMBasicBlockRef block = LLVMGetFirstBasicBlock(function);
        while (block != null) {
            LLVMValueRef instr = LLVMGetFirstInstruction(block);
            while (instr != null) {
                if (LLVMGetInstructionOpcode(instr) == LLVMAlloca) {
                    varUsageMap.put(instr, false);
                }
                // 记录load指令和它们加载的变量
                else if (LLVMGetInstructionOpcode(instr) == LLVMLoad) {
                    LLVMValueRef ptr = LLVMGetOperand(instr, 0);
                    loadResultToVar.put(instr, ptr);
                }
                instr = LLVMGetNextInstruction(instr);
            }
            block = LLVMGetNextBasicBlock(block);
        }

        // 第二遍：检查变量使用情况
        block = LLVMGetFirstBasicBlock(function);
        while (block != null) {
            LLVMValueRef instr = LLVMGetFirstInstruction(block);
            while (instr != null) {
                // 如果是return指令，检查返回值
                if (LLVMGetInstructionOpcode(instr) == LLVMRet && LLVMGetNumOperands(instr) > 0) {
                    LLVMValueRef retVal = LLVMGetOperand(instr, 0);
                    markUsedValue(retVal, varUsageMap, loadResultToVar);
                }
                // 对于binary操作，检查两个操作数
                else if (isBinaryOperation(LLVMGetInstructionOpcode(instr))) {
                    for (int i = 0; i < LLVMGetNumOperands(instr); i++) {
                        LLVMValueRef operand = LLVMGetOperand(instr, i);
                        markUsedValue(operand, varUsageMap, loadResultToVar);
                    }
                }
                // 对于store指令，检查要存储的值
                else if (LLVMGetInstructionOpcode(instr) == LLVMStore) {
                    LLVMValueRef valueOp = LLVMGetOperand(instr, 0);
                    markUsedValue(valueOp, varUsageMap, loadResultToVar);
                }

                instr = LLVMGetNextInstruction(instr);
            }
            block = LLVMGetNextBasicBlock(block);
        }

        return varUsageMap;
    }

    private void markUsedValue(LLVMValueRef value, Map<LLVMValueRef, Boolean> varUsageMap,
                               Map<LLVMValueRef, LLVMValueRef> loadResultToVar) {
        // 如果值是load指令的结果，标记被加载的变量为已使用
        if (loadResultToVar.containsKey(value)) {
            LLVMValueRef ptr = loadResultToVar.get(value);
            if (varUsageMap.containsKey(ptr)) {
                varUsageMap.put(ptr, true);
            }
        }
        // 如果值直接是一个变量
        else if (varUsageMap.containsKey(value)) {
            varUsageMap.put(value, true);
        }
    }

    private boolean isBinaryOperation(int opcode) {
        return opcode == LLVMAdd || opcode == LLVMSub || opcode == LLVMMul ||
                opcode == LLVMSDiv || opcode == LLVMSRem;
    }

    private void removeUnusedVariables(LLVMValueRef function, Map<LLVMValueRef, Boolean> varUsageMap) {
        // 收集需要删除的指令
        List<LLVMValueRef> instructionsToRemove = new ArrayList<>();
        Map<LLVMValueRef, List<LLVMValueRef>> relatedStoreInstructions = new HashMap<>();

        // 找出未使用的变量和相关的store指令
        LLVMBasicBlockRef block = LLVMGetFirstBasicBlock(function);
        while (block != null) {
            LLVMValueRef instr = LLVMGetFirstInstruction(block);
            while (instr != null) {
                // 检查store指令，看是否存储到未使用的变量
                if (LLVMGetInstructionOpcode(instr) == LLVMStore) {
                    LLVMValueRef pointer = LLVMGetOperand(instr, 1);
                    if (varUsageMap.containsKey(pointer) && !varUsageMap.get(pointer)) {
                        // 这是一个store到未使用变量的指令
                        relatedStoreInstructions.computeIfAbsent(pointer, k -> new ArrayList<>()).add(instr);
                    }
                }
                instr = LLVMGetNextInstruction(instr);
            }
            block = LLVMGetNextBasicBlock(block);
        }

        // 删除相关的store指令和未使用的变量
        for (Map.Entry<LLVMValueRef, Boolean> entry : varUsageMap.entrySet()) {
            if (!entry.getValue()) { // 未使用的变量
                LLVMValueRef var = entry.getKey();

                // 先删除所有相关的store指令
                if (relatedStoreInstructions.containsKey(var)) {
                    for (LLVMValueRef storeInstr : relatedStoreInstructions.get(var)) {
                        LLVMInstructionEraseFromParent(storeInstr);
                        changed = true;
                    }
                }

                // 删除变量定义
                LLVMInstructionEraseFromParent(var);
                changed = true;
                System.out.println("删除了未使用的变量");
            }
        }
    }

    @Override
    public boolean hasChanged() {
        return changed;
    }
}