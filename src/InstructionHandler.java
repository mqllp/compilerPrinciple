import org.bytedeco.llvm.LLVM.LLVMValueRef;

// 指令处理器 - 使用策略模式处理不同类型的LLVM指令
public interface InstructionHandler {
    void process(LLVMValueRef instruction);
}