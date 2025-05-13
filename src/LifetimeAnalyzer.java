import org.bytedeco.llvm.LLVM.*;
import java.util.HashMap;
import java.util.Map;
import static org.bytedeco.llvm.global.LLVM.*;

public class LifetimeAnalyzer {
    private final Map<String, int[]> varLifetimes = new HashMap<>();
    private int totalLines = 0;

    public void analyze(LLVMValueRef function) {
        varLifetimes.clear();
        int lineNumber = 0;

        for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(function);
             bb != null;
             bb = LLVMGetNextBasicBlock(bb)) {

            for (LLVMValueRef inst = LLVMGetFirstInstruction(bb);
                 inst != null;
                 inst = LLVMGetNextInstruction(inst)) {

                String defName = LLVMGetValueName(inst).getString();
                if (!defName.isEmpty()) {
                    varLifetimes.put(defName, new int[]{lineNumber, lineNumber});
                }

                for (int i = 0; i < LLVMGetNumOperands(inst); i++) {
                    LLVMValueRef operand = LLVMGetOperand(inst, i);
                    if (operand != null && LLVMIsAConstantInt(operand) == null &&
                            LLVMIsABasicBlock(operand) == null && LLVMIsAGlobalValue(operand) == null) {

                        String name = LLVMGetValueName(operand).getString();
                        if (!name.isEmpty() && varLifetimes.containsKey(name)) {
                            varLifetimes.get(name)[1] = lineNumber;
                        }
                    }
                }

                lineNumber++;
            }
        }

        totalLines = lineNumber;
    }

    public int[] getLifetimeFor(String variable) {
        return varLifetimes.get(variable);
    }

    public int getMaxConcurrentLiveVariables() {
        int[] liveCounts = new int[totalLines];

        for (int[] lifetime : varLifetimes.values()) {
            for (int i = lifetime[0]; i <= lifetime[1]; i++) {
                liveCounts[i]++;
            }
        }

        int maxLive = 0;
        for (int count : liveCounts) {
            maxLive = Math.max(maxLive, count);
        }

        return maxLive;
    }
}