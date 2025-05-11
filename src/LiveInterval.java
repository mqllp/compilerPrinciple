import org.bytedeco.llvm.LLVM.LLVMValueRef;

public class LiveInterval {
    private LLVMValueRef value;   // LLVM值引用
    private String name;          // 变量名
    private int start;            // 开始点
    private int end;              // 结束点
    private Location location;    // 分配的位置

    // 构造函数
    public LiveInterval(LLVMValueRef value, String name, int start, int end) {
        this.value = value;
        this.name = name;
        this.start = start;
        this.end = end;
        this.location = null; // 初始未分配位置
    }

    // Getter和Setter方法
    public LLVMValueRef getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    // 辅助方法：比较结束点
    public static int compareEndPoint(LiveInterval a, LiveInterval b) {
        return Integer.compare(a.end, b.end);
    }

    // 辅助方法：判断是否与另一个区间重叠
    public boolean overlaps(LiveInterval other) {
        return !(this.end <= other.start || this.start >= other.end);
    }
}