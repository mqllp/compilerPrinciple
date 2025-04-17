import org.llvm4j.llvm4j.Value;
import org.bytedeco.llvm.LLVM.*;
import java.util.*;

public class IRSymbolTable {
    private LinkedList<Map<String, LLVMValueRef>> symbolTables;
    private Map<String, LLVMValueRef> functionTable;

    public IRSymbolTable() {
        symbolTables = new LinkedList<>();
        functionTable = new HashMap<>();
        enterScope();  // 创建全局作用域
    }

    public void enterScope() {
        symbolTables.addFirst(new HashMap<>());
    }

    public void exitScope() {
        if (symbolTables.size() > 1) {  // 保留全局作用域
            symbolTables.removeFirst();
        }
    }

    public void put(String name, LLVMValueRef value) {
        symbolTables.getFirst().put(name, value);
    }

    public void putFunction(String name, LLVMValueRef function) {
        functionTable.put(name, function);
    }

    public LLVMValueRef get(String name) {
        // 首先在变量表中查找
        for (Map<String, LLVMValueRef> table : symbolTables) {
            LLVMValueRef value = table.get(name);
            if (value != null) {
                return value;
            }
        }
        // 如果找不到，在函数表中查找
        return functionTable.get(name);
    }

    public boolean isGlobalScope() {
        return symbolTables.size() == 1;
    }

    public void clear() {
        symbolTables.clear();
        functionTable.clear();
        enterScope();  // 重新创建全局作用域
    }
}
