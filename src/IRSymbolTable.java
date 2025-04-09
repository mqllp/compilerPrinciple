import org.llvm4j.llvm4j.Value;
import java.util.*;

public class IRSymbolTable {
    private LinkedList<Map<String, Value>> symbolTables;
    private Map<String, Value> functionTable;  // 存储函数定义

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

    public void put(String name, Value value) {
        symbolTables.getFirst().put(name, value);
    }

    public void putFunction(String name, Value function) {
        functionTable.put(name, function);
    }

    public Value get(String name) {
        // 首先在变量表中查找
        for (Map<String, Value> table : symbolTables) {
            Value value = table.get(name);
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
