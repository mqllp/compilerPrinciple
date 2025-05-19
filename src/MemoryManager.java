import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MemoryManager {
    private final Map<String, Integer> variableOffsets;
    private final boolean[] stackSlots;
    private final int capacity;

    public MemoryManager(int capacity) {
        this.variableOffsets = new HashMap<>();
        this.stackSlots = new boolean[capacity / 4]; // 每个槽位4字节
        this.capacity = capacity;
    }

    public int allocate(String variable) {
        // 如果变量已经有栈位置，直接返回
        if (variableOffsets.containsKey(variable)) {
            return variableOffsets.get(variable);
        }

        // 寻找第一个空闲槽位
        for (int i = 0; i < stackSlots.length; i++) {
            if (!stackSlots[i]) {
                int offset = i * 4;
                variableOffsets.put(variable, offset);
                stackSlots[i] = true;
                return offset;
            }
        }

        throw new RuntimeException("栈溢出 - 没有可用槽位");
    }

    public int getOffset(String variable) {
        return variableOffsets.getOrDefault(variable, allocate(variable));
    }

    public void free(String variable) {
        if (variableOffsets.containsKey(variable)) {
            int offset = variableOffsets.get(variable);
            stackSlots[offset / 4] = false;
            variableOffsets.remove(variable);
        }
    }

    public void releaseDeadVariables(int currentLine, LifetimeAnalyzer analyzer) {
        Set<String> varsToRelease = new HashSet<>();

        for (String var : variableOffsets.keySet()) {
            int[] lifetime = analyzer.getLifetimeFor(var);
            if (lifetime != null && lifetime[1] < currentLine) {
                varsToRelease.add(var);
            }
        }

        for (String var : varsToRelease) {
            free(var);
        }
    }

    public int calculateStackSize() {
        int maxOffset = 0;
        for (int offset : variableOffsets.values()) {
            maxOffset = Math.max(maxOffset, offset);
        }
        return maxOffset + 4; // 确保至少有4字节空间
    }

    public boolean hasOffset(String variable) {
        return variableOffsets.containsKey(variable);
    }
}