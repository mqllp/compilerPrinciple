// src/optimization/OptimizationPass.java
package optimization;

import org.bytedeco.llvm.LLVM.LLVMModuleRef;

public interface OptimizationPass {
    LLVMModuleRef run(LLVMModuleRef module);
    boolean hasChanged(); // 检查优化是否产生了变化
}