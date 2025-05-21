package optimization;

import org.bytedeco.llvm.LLVM.LLVMModuleRef;
import java.util.ArrayList;
import java.util.List;

public class IROptimizer {
    private List<OptimizationPass> passes = new ArrayList<>();

    public IROptimizer() {
        // 添加各种优化Pass
        passes.add(new ConstantPropagationPass());
        passes.add(new UnusedVarEliminationPass());
        passes.add(new DeadCodeEliminationPass());
    }

    public LLVMModuleRef optimize(LLVMModuleRef module) {
        boolean changed = true;
        int iterations = 0;

        // 迭代运行优化，直到没有变化或达到最大迭代次数
        while (changed && iterations < 3) {
            changed = false;
            iterations++;

            // 先运行常量传播
            for (OptimizationPass pass : passes) {
                if (pass instanceof ConstantPropagationPass) {
                    module = pass.run(module);
                    changed |= pass.hasChanged();
                }
            }

            /*
            // 然后运行未使用变量消除
            for (OptimizationPass pass : passes) {
                if (pass instanceof UnusedVarEliminationPass) {
                    module = pass.run(module);
                    changed |= pass.hasChanged();
                }
            }

            // 最后运行死代码消除
            for (OptimizationPass pass : passes) {
                if (pass instanceof DeadCodeEliminationPass) {
                    module = pass.run(module);
                    changed |= pass.hasChanged();
                }
            }

             */
        }

        return module;
    }
}