package type;

public class ArrayType extends Type {
    private Type elementType;
    private int numElements;

    public ArrayType(Type elementType, int numElements) {
        super(TypeKind.ARRAY);
        this.elementType = elementType;
        this.numElements = numElements;
    }

    public Type getElementType() {
        return elementType;
    }

    public int getNumElements() {
        return numElements;
    }

    @Override
    public boolean isEqual(Type other) {
        if (!(other instanceof ArrayType)) {
            return false;
        }
        ArrayType otherArray = (ArrayType) other;
        return elementType.isEqual(otherArray.elementType);
    }

    @Override
    public String toString() {
        return elementType + "[" + numElements + "]";
    }
}