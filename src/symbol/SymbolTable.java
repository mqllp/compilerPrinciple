package symbol;

import org.bytedeco.llvm.LLVM.LLVMValueRef;
import type.Type;

import java.util.*;

public class SymbolTable {
    // 用于存储所有作用域的符号表栈
    private LinkedList<Map<String, Symbol>> scopeTables;
    // 专门存储函数符号
    private Map<String, Symbol> functionTable;

    public SymbolTable() {
        scopeTables = new LinkedList<>();
        functionTable = new HashMap<>();
        enterScope(); // 创建全局作用域
    }

    public void enterScope() {
        scopeTables.addFirst(new HashMap<>());
    }

    public void exitScope() {
        if (scopeTables.size() > 1) { // 保留全局作用域
            scopeTables.removeFirst();
        }
    }

    // 为语义分析提供的接口
    public boolean declareSymbol(String name, Type type, int lineNo) {
        Map<String, Symbol> currentScope = scopeTables.getFirst();
        if (currentScope.containsKey(name)) {
            return false; // 符号已在当前作用域声明
        }

        currentScope.put(name, new Symbol(name, type, lineNo));
        return true;
    }

    // 为IR生成提供的变量定义接口
    public void put(String name, LLVMValueRef value) {
        Symbol symbol = scopeTables.getFirst().get(name);
        if (symbol != null) {
            // 更新已有符号的LLVM值
            symbol.setLlvmValueRef(value);
        } else {
            // 创建新符号
            scopeTables.getFirst().put(name, new Symbol(name, value, false));
        }
    }

    // 为IR生成提供的函数定义接口
    public void putFunction(String name, LLVMValueRef function) {
        Symbol symbol = functionTable.get(name);
        if (symbol != null) {
            // 更新已有函数符号的LLVM值
            symbol.setLlvmValueRef(function);
        } else {
            // 创建新函数符号
            Symbol funcSymbol = new Symbol(name, function, true);
            functionTable.put(name, funcSymbol);
        }
    }

    // 为IR生成同时提供完整信息的接口
    public void define(String name, Type type, LLVMValueRef value, boolean isFunction) {
        Symbol symbol = new Symbol(name, type, 0, value, isFunction);

        if (isFunction) {
            functionTable.put(name, symbol);
        } else {
            scopeTables.getFirst().put(name, symbol);
        }
    }

    // 查找符号（包括变量和函数）
    public Symbol lookup(String name) {
        // 先在变量作用域中查找
        for (Map<String, Symbol> table : scopeTables) {
            Symbol symbol = table.get(name);
            if (symbol != null) {
                return symbol;
            }
        }
        // 再在函数表中查找
        return functionTable.get(name);
    }

    // 获取LLVM值引用
    public LLVMValueRef get(String name) {
        Symbol symbol = lookup(name);
        return symbol != null ? symbol.getLlvmValueRef() : null;
    }

    // 在当前作用域中查找符号
    public Symbol lookupCurrentScope(String name) {
        return scopeTables.getFirst().get(name);
    }

    // 是否在全局作用域
    public boolean isGlobalScope() {
        return scopeTables.size() == 1;
    }

    // 清空符号表
    public void clear() {
        scopeTables.clear();
        functionTable.clear();
        enterScope();  // 重新创建全局作用域
    }

    public Symbol lookupSymbol(String name) {
        return null;
    }
}