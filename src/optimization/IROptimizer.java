// src/optimization/IROptimizer.java
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
        // 目前只运行常量传播优化
        for (OptimizationPass pass : passes) {
            if (pass instanceof ConstantPropagationPass) {
                System.out.println("正在运行常量传播优化...");
                module = pass.run(module);
                if (pass.hasChanged()) {
                    System.out.println("常量传播优化完成，代码已更改");
                } else {
                    System.out.println("常量传播优化完成，代码无变化");
                }
                // 暂时不运行其他优化pass
                // TODO: 迭代运行优化pass直到达到不动点
                break;
            }
        }
        return module;
    }
}