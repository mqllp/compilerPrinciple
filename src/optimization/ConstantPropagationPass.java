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
                if (!constants.isEmpty()) {
                    changed = true;
                    applyConstantOptimization(constants);
                }
            }

            function = LLVMGetNextFunction(function);
        }

        return module;
    }

    private void applyConstantOptimization(Map<LLVMValueRef, LatticeValue> constants) {
        // 对于每个被识别为常量的指令，用常量值替换其使用
        for (Map.Entry<LLVMValueRef, LatticeValue> entry : constants.entrySet()) {
            LLVMValueRef instruction = entry.getKey();
            LatticeValue value = entry.getValue();

            if (value.getType() == LatticeValue.ValueType.CONSTANT) {
                // 创建常量值
                LLVMContextRef context = LLVMGetModuleContext(LLVMGetGlobalParent(instruction));
                LLVMValueRef constValue = LLVMConstInt(LLVMInt32Type(), value.getConstantValue(), 0);

                // 替换所有使用该指令的地方为常量值
                LLVMReplaceAllUsesWith(instruction, constValue);
            }
        }
    }

    @Override
    public boolean hasChanged() {
        return changed;
    }
}