package type;

public class IntType extends Type {
    public static final IntType instance = new IntType();

    private IntType() {
        super(TypeKind.INT);
    }

    @Override
    public boolean isEqual(Type other) {
        return other instanceof IntType;
    }

    @Override
    public String toString() {
        return "int";
    }
}