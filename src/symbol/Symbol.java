package symbol;

import type.Type;

public class Symbol {
    private String name;
    private Type type;
    private int lineNo;

    public Symbol(String name, Type type, int lineNo) {
        this.name = name;
        this.type = type;
        this.lineNo = lineNo;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public int getLineNo() {
        return lineNo;
    }
}