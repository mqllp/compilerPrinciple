package instruction;

import org.bytedeco.llvm.LLVM.LLVMValueRef;

public interface InstructionHandler {
    void process(LLVMValueRef instruction);
}