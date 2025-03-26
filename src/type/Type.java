package type;

public abstract class Type {
    public enum TypeKind {
        INT,
        ARRAY,
        FUNCTION,
        VOID
    }

    private TypeKind kind;

    public Type(TypeKind kind) {
        this.kind = kind;
    }

    public TypeKind getKind() {
        return kind;
    }

    public abstract boolean isEqual(Type other);

    @Override
    public abstract String toString();
}