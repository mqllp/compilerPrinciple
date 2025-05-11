public class Location {
    public enum LocationType { REGISTER, STACK, GLOBAL }

    private LocationType type;
    private int register;     // 寄存器编号（如果在寄存器中）
    private int offset;       // 栈偏移量（如果在栈上）
    private String name;      // 全局变量名称

    // 构造函数 - 寄存器位置
    public Location(int register) {
        this.type = LocationType.REGISTER;
        this.register = register;
    }

    // 构造函数 - 栈位置
    public Location(int offset, boolean isStack) {
        this.type = LocationType.STACK;
        this.offset = offset;
    }

    // 构造函数 - 全局变量位置
    public Location(String name) {
        this.type = LocationType.GLOBAL;
        this.name = name;
    }

    // Getter和Setter方法
    public LocationType getType() {
        return type;
    }

    public int getRegister() {
        return register;
    }

    public int getOffset() {
        return offset;
    }

    public String getName() {
        return name;
    }

    public boolean isRegister() {
        return type == LocationType.REGISTER;
    }

    public boolean isStack() {
        return type == LocationType.STACK;
    }

    public boolean isGlobal() {
        return type == LocationType.GLOBAL;
    }
}