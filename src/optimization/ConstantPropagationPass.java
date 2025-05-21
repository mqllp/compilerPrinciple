package optimization;

import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;
import optimization.cfg.*;
import optimization.lattice.*;
import java.util.*;

public class ConstantPropagationPass implements OptimizationPass {
    private boolean changed = false;

    @Override
    public LLVMModuleRef run(LLVMModuleRef module) {
        changed = false;

        // 遍历模块中的所有函数
        LLVMValueRef function = LLVMGetFirstFunction(module);
        while (function != null) {
            // 跳过声明（没有函数体的函数）
            if (LLVMIsDeclaration(function) == 0) {
                // 构建控制流图
                ControlFlowGraph cfg = new ControlFlowGraph(function);

                // 执行常量传播
                cfg.runConstantPropagation();

                // 获取常量结果并应用优化
                Map<LLVMValueRef, LatticeValue> constants = cfg.getConstantValues();

                // 进行常量替换和优化
                if (!constants.isEmpty()) {
                    changed = true;
                    System.out.println("找到" + constants.size() + "个可优化的常量指令");
                    applyConstantOptimization(function, constants);
                    simplifyReturnStatements(function, cfg);
                }
            }

            function = LLVMGetNextFunction(function);
        }

        return module;
    }

    private void applyConstantOptimization(LLVMValueRef function, Map<LLVMValueRef, LatticeValue> constants) {
        LLVMContextRef context = LLVMGetModuleContext(LLVMGetGlobalParent(function));
        LLVMBuilderRef builder = LLVMCreateBuilderInContext(context);

        // 逐个替换常量
        for (Map.Entry<LLVMValueRef, LatticeValue> entry : constants.entrySet()) {
            LLVMValueRef instruction = entry.getKey();
            LatticeValue value = entry.getValue();

            // 只处理确定是常量的指令
            if (value.getType() == LatticeValue.ValueType.CONSTANT) {
                // 创建常量值
                LLVMValueRef constValue = LLVMConstInt(LLVMInt32Type(), value.getConstantValue(), 0);

                // 替换所有使用
                LLVMReplaceAllUsesWith(instruction, constValue);

                // 如果不是终结指令，可以考虑删除该指令
                if (LLVMGetInstructionOpcode(instruction) != LLVMRet &&
                        LLVMGetInstructionOpcode(instruction) != LLVMBr) {
                    // 检查指令是否可以安全删除
                    if (LLVMGetFirstUse(instruction) == null) {
                        LLVMInstructionEraseFromParent(instruction);
                    }
                }
            }
        }

        // 优化返回语句
        LLVMBasicBlockRef block = LLVMGetFirstBasicBlock(function);
        while (block != null) {
            LLVMValueRef instr = LLVMGetFirstInstruction(block);
            while (instr != null) {
                LLVMValueRef nextInstr = LLVMGetNextInstruction(instr);

                if (LLVMGetInstructionOpcode(instr) == LLVMRet) {
                    // 如果返回指令有操作数
                    if (LLVMGetNumOperands(instr) > 0) {
                        LLVMValueRef retVal = LLVMGetOperand(instr, 0);

                        // 如果返回值是常量表达式
                        if (constants.containsKey(retVal) &&
                                constants.get(retVal).getType() == LatticeValue.ValueType.CONSTANT) {
                            // 创建新的常量返回
                            LLVMPositionBuilderAtEnd(builder, block);
                            LLVMValueRef constVal = LLVMConstInt(LLVMInt32Type(),
                                    constants.get(retVal).getConstantValue(), 0);
                            LLVMValueRef newRet = LLVMBuildRet(builder, constVal);

                            // 删除旧的返回指令
                            LLVMInstructionEraseFromParent(instr);
                        }
                        // 直接常量返回值
                        else if (LLVMIsConstant(retVal) != 0 && LLVMIsAConstantInt(retVal) != null) {
                            // 已经是常量返回，不需要修改
                        }
                    }
                }

                instr = nextInstr;
            }

            block = LLVMGetNextBasicBlock(block);
        }

        LLVMDisposeBuilder(builder);
    }

    private void simplifyReturnStatements(LLVMValueRef function, ControlFlowGraph cfg) {
        LLVMContextRef context = LLVMGetModuleContext(LLVMGetGlobalParent(function));
        LLVMBuilderRef builder = LLVMCreateBuilderInContext(context);

        for (Instruction instr : cfg.getAllInstructions()) {
            if (LLVMGetInstructionOpcode(instr.getLlvmInstruction()) == LLVMRet) {
                // 检查返回值是否是常量
                if (instr.getNumOperands() > 0) {
                    LLVMValueRef returnValue = instr.getOperand(0);

                    // 如果返回值是常量，直接替换整个返回语句
                    if (LLVMIsConstant(returnValue) != 0) {
                        // 生成新的返回指令
                        LLVMPositionBuilderBefore(builder, instr.getLlvmInstruction());
                        LLVMBuildRet(builder, returnValue);
                        LLVMInstructionEraseFromParent(instr.getLlvmInstruction());
                    }
                    // 如果我们可以计算出返回值是常量
                    else if (LLVMIsAInstruction(returnValue) != null) {
                        LatticeValue computedValue = instr.getInValue();
                        if (computedValue != null && computedValue.getType() == LatticeValue.ValueType.CONSTANT) {
                            Long constValue = computedValue.getConstantValue();
                            LLVMValueRef constRetVal = LLVMConstInt(LLVMInt32Type(), constValue, 0);
                            LLVMPositionBuilderBefore(builder, instr.getLlvmInstruction());
                            LLVMBuildRet(builder, constRetVal);
                            LLVMInstructionEraseFromParent(instr.getLlvmInstruction());
                        }
                    }
                }
            }
        }

        LLVMDisposeBuilder(builder);
    }

    @Override
    public boolean hasChanged() {
        return changed;
    }
}