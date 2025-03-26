package type;

import java.util.ArrayList;
import java.util.List;

public class FunctionType extends Type {
    private Type returnType;
    private List<Type> paramTypes;

    public FunctionType(Type returnType) {
        super(TypeKind.FUNCTION);
        this.returnType = returnType;
        this.paramTypes = new ArrayList<>();
    }

    public FunctionType(Type returnType, List<Type> paramTypes) {
        super(TypeKind.FUNCTION);
        this.returnType = returnType;
        this.paramTypes = paramTypes;
    }

    public void addParamType(Type type) {
        paramTypes.add(type);
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Type> getParamTypes() {
        return paramTypes;
    }

    @Override
    public boolean isEqual(Type other) {
        if (!(other instanceof FunctionType)) {
            return false;
        }

        FunctionType otherFunc = (FunctionType) other;
        if (!returnType.isEqual(otherFunc.returnType)) {
            return false;
        }

        if (paramTypes.size() != otherFunc.paramTypes.size()) {
            return false;
        }

        for (int i = 0; i < paramTypes.size(); i++) {
            if (!paramTypes.get(i).isEqual(otherFunc.paramTypes.get(i))) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(returnType).append(" (");
        for (int i = 0; i < paramTypes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(paramTypes.get(i));
        }
        sb.append(")");
        return sb.toString();
    }
}