import java.util.HashMap;
import java.util.Map;
import instruction.InstructionHandler;

import static org.bytedeco.llvm.global.LLVM.*;

public class InstructionHandlerFactory {
    private final Map<Integer, InstructionHandler> handlers = new HashMap<>();

    public InstructionHandlerFactory(ModuleProcessor processor) {
        registerHandlers(processor);
    }

    private void registerHandlers(ModuleProcessor processor) {
        handlers.put(LLVMRet, processor::handleReturn);
        handlers.put(LLVMBr, processor::handleBranch);
        handlers.put(LLVMAdd, (inst) -> processor.handleBinaryOperation(inst, "add"));
        handlers.put(LLVMSub, (inst) -> processor.handleBinaryOperation(inst, "sub"));
        handlers.put(LLVMMul, (inst) -> processor.handleBinaryOperation(inst, "mul"));
        handlers.put(LLVMSDiv, (inst) -> processor.handleBinaryOperation(inst, "div"));
        handlers.put(LLVMSRem, (inst) -> processor.handleBinaryOperation(inst, "rem"));
        handlers.put(LLVMAlloca, processor::handleAlloca);
        handlers.put(LLVMLoad, processor::handleLoad);
        handlers.put(LLVMStore, processor::handleStore);
        handlers.put(LLVMICmp, processor::handleICmp);
        handlers.put(LLVMZExt, processor::handleZExt);
    }

    public InstructionHandler getHandler(int opcode) {
        return handlers.getOrDefault(opcode, null);
    }
}