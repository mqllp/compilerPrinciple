package optimization.lattice;

import java.util.Objects;

public class LatticeValue {
    public enum ValueType {
        UNDEF,  // 未定义
        CONSTANT, // 常量
        NAC     // 非常量(Not A Constant)
    }

    private ValueType type;
    private Long constantValue; // 当type为CONSTANT时有效

    public LatticeValue() {
        this.type = ValueType.UNDEF;
        this.constantValue = null;
    }

    public LatticeValue(ValueType type, Long constantValue) {
        this.type = type;
        this.constantValue = constantValue;
    }

    public ValueType getType() {
        return type;
    }

    public Long getConstantValue() {
        return constantValue;
    }

    // 格值的meet操作，用于数据流分析
    public static LatticeValue meet(LatticeValue a, LatticeValue b) {
        // UNDEF ⊓ x = x
        if (a.type == ValueType.UNDEF) return b;
        if (b.type == ValueType.UNDEF) return a;

        // NAC ⊓ x = NAC
        if (a.type == ValueType.NAC || b.type == ValueType.NAC) {
            return new LatticeValue(ValueType.NAC, null);
        }

        // CONSTANT(c1) ⊓ CONSTANT(c2) = CONSTANT(c1) if c1 = c2, NAC otherwise
        if (Objects.equals(a.constantValue, b.constantValue)) {
            return new LatticeValue(ValueType.CONSTANT, a.constantValue);
        } else {
            return new LatticeValue(ValueType.NAC, null);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LatticeValue that = (LatticeValue) obj;
        return type == that.type &&
                Objects.equals(constantValue, that.constantValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, constantValue);
    }

    @Override
    public String toString() {
        if (type == ValueType.CONSTANT) {
            return "CONSTANT(" + constantValue + ")";
        }
        return type.toString();
    }
}