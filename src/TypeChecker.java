import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import symbol.Symbol;
import symbol.SymbolTable;
import type.*;



import java.util.ArrayList;
import java.util.List;

public class TypeChecker extends SysYParserBaseListener {
    private SymbolTable symbolTable;
    private List<SemanticError> errors;
    private boolean hasError;
    private Type currentFunctionReturnType;

    public TypeChecker() {
        symbolTable = new SymbolTable();
        errors = new ArrayList<>();
        hasError = false;
    }

    public void check(SysYParser.CompUnitContext ctx) {
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, ctx);

        if (!hasError) {
            System.err.println("No semantic errors in the program!");
        }
    }

    private void reportError(int errorType, Token token, String message) {
        hasError = true;
        SemanticError error = new SemanticError(errorType, token.getLine(), message);
        errors.add(error);
        System.err.println(error);
    }

    // Override the listener methods to implement type checking

    @Override
    public void enterCompUnit(SysYParser.CompUnitContext ctx) {
        // Initialize built-in functions if needed
    }

    @Override
    public void enterFuncDef(SysYParser.FuncDefContext ctx) {
        String funcName = ctx.IDENT().getText();
        Symbol existingSymbol = symbolTable.lookupCurrentScope(funcName);

        if (existingSymbol != null) {
            reportError(SemanticError.REDEFINED_FUNCTION, ctx.IDENT().getSymbol(),
                    "Redefined function: " + funcName);
            return;
        }

        // Handle function return type
        Type returnType;
        if (ctx.funcType().getText().equals("void")) {
            returnType = VoidType.instance;
        } else {
            returnType = IntType.instance;
        }

        FunctionType functionType = new FunctionType(returnType);
        currentFunctionReturnType = returnType;

        // Process function parameters
        if (ctx.funcFParams() != null) {
            // Add parameter types to function type
        }

        symbolTable.declareSymbol(funcName, functionType, ctx.IDENT().getSymbol().getLine());

        // Enter function scope
        symbolTable.enterScope();

        // Declare parameters in function scope
        if (ctx.funcFParams() != null) {
            for (SysYParser.FuncFParamContext param : ctx.funcFParams().funcFParam()) {
                // Process and declare each parameter
            }
        }
    }

    @Override
    public void exitFuncDef(SysYParser.FuncDefContext ctx) {
        symbolTable.exitScope();
        currentFunctionReturnType = null;
    }

    // Implement other listener methods for:
    // - Variable declarations
    // - Array declarations
    // - Function calls
    // - Expressions
    // - Statements
    // - etc.

    // Additional helper methods for type checking
}