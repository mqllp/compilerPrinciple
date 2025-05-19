import org.bytedeco.llvm.LLVM.*;
import java.util.*;
import static org.bytedeco.llvm.global.LLVM.*;

public class RegisterManager {
    public final List<String> registerPool;
    private final Map<String, String> varToRegMap;
    private final Map<String, Integer> regPriority;
    private final LifetimeAnalyzer lifetimeAnalyzer;
    private final MemoryManager memoryManager;
    private final CodeGenerator codeGenerator;
    private int nextPriority = 0;

    public RegisterManager(LifetimeAnalyzer analyzer, MemoryManager memory, CodeGenerator code) {
        // 初始化寄存器池，按照调用约定和使用频率排序
        registerPool = new ArrayList<>(Arrays.asList(
                "a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7",
                "s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11",
                "t2", "t3", "t4", "t5", "t6"
        ));
        varToRegMap = new HashMap<>();
        regPriority = new HashMap<>();
        this.lifetimeAnalyzer = analyzer;
        this.memoryManager = memory;
        this.codeGenerator = code;
    }

    public String allocate(String variable) {
        // 如果变量已经分配了寄存器，直接返回
        if (varToRegMap.containsKey(variable)) {
            touchRegister(variable);
            return varToRegMap.get(variable);
        }

        // 寻找空闲寄存器
        for (String reg : registerPool) {
            if (!varToRegMap.containsValue(reg)) {
                varToRegMap.put(variable, reg);
                regPriority.put(variable, nextPriority++);
                return reg;
            }
        }

        // 没有空闲寄存器，执行溢出
        return spillAndReallocate(variable);
    }

    private String spillAndReallocate(String variable) {
        String victimVar = findLeastRecentlyUsed();
        if (victimVar == null) {
            return "t0"; // 使用临时寄存器
        }

        String reg = varToRegMap.remove(victimVar);
        regPriority.remove(victimVar);

        // 将受害者变量保存到栈中
        int offset = memoryManager.allocate(victimVar);
        codeGenerator.emitStore(reg, offset);
        codeGenerator.emitComment("Spill " + victimVar + " to stack offset " + offset);

        // 分配寄存器给新变量
        varToRegMap.put(variable, reg);
        regPriority.put(variable, nextPriority++);
        return reg;
    }

    private String findLeastRecentlyUsed() {
        return regPriority.entrySet()
                .stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public String get(String variable) {
        if (varToRegMap.containsKey(variable)) {
            touchRegister(variable);
            return varToRegMap.get(variable);
        }

        // 如果变量不在寄存器中，需要从栈上加载
        String reg = allocate(variable);
        if (memoryManager.hasOffset(variable)) {
            int offset = memoryManager.getOffset(variable);
            codeGenerator.emitLoad(reg, offset);
            codeGenerator.emitComment("Reload " + variable + " from stack offset " + offset);
        }
        return reg;
    }

    private void touchRegister(String variable) {
        regPriority.put(variable, nextPriority++);
    }

    public void free(String variable) {
        if (varToRegMap.containsKey(variable)) {
            varToRegMap.remove(variable);
            regPriority.remove(variable);
        }
    }

    public void releaseDead(int currentLine) {
        // 释放已经不活跃的变量
        Set<String> varsToRelease = new HashSet<>();

        for (String var : varToRegMap.keySet()) {
            int[] lifetime = lifetimeAnalyzer.getLifetimeFor(var);
            if (lifetime != null && lifetime[1] < currentLine) {
                varsToRelease.add(var);
            }
        }

        for (String var : varsToRelease) {
            free(var);
        }
    }

    public boolean isConstant(LLVMValueRef value) {
        return LLVMIsAConstantInt(value) != null;
    }
}