package type;

public class VoidType extends Type {
    public static final VoidType instance = new VoidType();

    private VoidType() {
        super(TypeKind.VOID);
    }

    @Override
    public boolean isEqual(Type other) {
        return other instanceof VoidType;
    }

    @Override
    public String toString() {
        return "void";
    }
}