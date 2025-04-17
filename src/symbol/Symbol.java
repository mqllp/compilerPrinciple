package symbol;

import org.bytedeco.llvm.LLVM.LLVMValueRef;
import type.Type;

public class Symbol {
    private String name;
    private Type type;                  // 语义分析使用
    private int lineNo;                 // 语义分析使用
    private LLVMValueRef llvmValueRef;  // IR生成使用
    private boolean isFunction;
    private boolean isVoid;
    private boolean isConstant;

    // 语义分析构造函数
    public Symbol(String name, Type type, int lineNo) {
        this.name = name;
        this.type = type;
        this.lineNo = lineNo;
    }

    // IR生成构造函数
    public Symbol(String name, LLVMValueRef llvmValueRef, boolean isFunction) {
        this.name = name;
        this.llvmValueRef = llvmValueRef;
        this.isFunction = isFunction;
    }

    // 完整构造函数
    public Symbol(String name, Type type, int lineNo, LLVMValueRef llvmValueRef, boolean isFunction) {
        this.name = name;
        this.type = type;
        this.lineNo = lineNo;
        this.llvmValueRef = llvmValueRef;
        this.isFunction = isFunction;
    }

    // Getter和Setter方法
    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getLineNo() {
        return lineNo;
    }

    public LLVMValueRef getLlvmValueRef() {
        return llvmValueRef;
    }

    public void setLlvmValueRef(LLVMValueRef llvmValueRef) {
        this.llvmValueRef = llvmValueRef;
    }

    public boolean isFunction() {
        return isFunction;
    }

    public boolean isVoid() {
        return isVoid;
    }

    public void setVoid(boolean isVoid) {
        this.isVoid = isVoid;
    }

    public boolean isConstant() {
        return isConstant;
    }

    public void setConstant(boolean isConstant) {
        this.isConstant = isConstant;
    }
}