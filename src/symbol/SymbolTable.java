package symbol;

import type.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {
    private List<Map<String, Symbol>> scopeTables;

    public SymbolTable() {
        scopeTables = new ArrayList<>();
        enterScope(); // Global scope
    }

    public void enterScope() {
        scopeTables.add(new HashMap<>());
    }

    public void exitScope() {
        if (scopeTables.size() > 1) {
            scopeTables.remove(scopeTables.size() - 1);
        }
    }

    public boolean declareSymbol(String name, Type type, int lineNo) {
        Map<String, Symbol> currentScope = scopeTables.get(scopeTables.size() - 1);
        if (currentScope.containsKey(name)) {
            return false; // Symbol already declared in current scope
        }

        currentScope.put(name, new Symbol(name, type, lineNo));
        return true;
    }

    public Symbol lookupSymbol(String name) {
        // Search from current scope up to global scope
        for (int i = scopeTables.size() - 1; i >= 0; i--) {
            Symbol symbol = scopeTables.get(i).get(name);
            if (symbol != null) {
                return symbol;
            }
        }
        return null; // Symbol not found
    }

    public Symbol lookupCurrentScope(String name) {
        return scopeTables.get(scopeTables.size() - 1).get(name);
    }
}