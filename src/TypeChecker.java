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

    public boolean hasErrors() {
        return hasError;
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
        // Add period at the end of the message if it doesn't have one
        if (!message.endsWith(".")) {
            message += ".";
        }
        SemanticError error = new SemanticError(errorType, token.getLine(), message);
        errors.add(error);
        System.err.println(error);
    }

    // Override the listener methods to implement type checking

    @Override
    public void enterCompUnit(SysYParser.CompUnitContext ctx) {
        // Initialize built-in functions if needed
        // For example: putint, getint, etc.
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
            for (SysYParser.FuncFParamContext param : ctx.funcFParams().funcFParam()) {
                String paramName = param.IDENT().getText();
                Type paramType = IntType.instance;

                // Check if it's an array parameter
                if (!param.L_BRACKT().isEmpty()) {
                    // Handle array parameter
                    for (int i = param.L_BRACKT().size() - 1; i >= 0; i--) {
                        if (i == 0) {
                            // First dimension can be omitted in function params
                            paramType = new ArrayType(paramType, 0);
                        } else if (param.exp(i-1) != null) {
                            // Other dimensions should have sizes
                            paramType = new ArrayType(paramType, 0); // Simplified, actual size handling needed
                        }
                    }
                }

                functionType.addParamType(paramType);
            }
        }

        symbolTable.declareSymbol(funcName, functionType, ctx.IDENT().getSymbol().getLine());

        // Enter function scope
        symbolTable.enterScope();

        // Declare parameters in function scope
        if (ctx.funcFParams() != null) {
            for (SysYParser.FuncFParamContext param : ctx.funcFParams().funcFParam()) {
                String paramName = param.IDENT().getText();
                Type paramType = IntType.instance;

                // Check if it's an array parameter
                if (!param.L_BRACKT().isEmpty()) {
                    // Handle array parameter type
                    for (int i = param.L_BRACKT().size() - 1; i >= 0; i--) {
                        if (i == 0) {
                            paramType = new ArrayType(paramType, 0);
                        } else if (param.exp(i-1) != null) {
                            paramType = new ArrayType(paramType, 0);
                        }
                    }
                }

                // Check for parameter redefinition
                if (!symbolTable.declareSymbol(paramName, paramType, param.IDENT().getSymbol().getLine())) {
                    reportError(SemanticError.REDEFINED_VARIABLE, param.IDENT().getSymbol(),
                            "Redefined variable: " + paramName);
                }
            }
        }
    }

    @Override
    public void exitFuncDef(SysYParser.FuncDefContext ctx) {
        symbolTable.exitScope();
        currentFunctionReturnType = null;
    }

    @Override
    public void enterBlock(SysYParser.BlockContext ctx) {
        // Enter a new scope for blocks, except for function blocks which are handled separately
        if (!(ctx.getParent() instanceof SysYParser.FuncDefContext)) {
            symbolTable.enterScope();
        }
    }

    @Override
    public void exitBlock(SysYParser.BlockContext ctx) {
        // Exit scope for blocks, except for function blocks which are handled separately
        if (!(ctx.getParent() instanceof SysYParser.FuncDefContext)) {
            symbolTable.exitScope();
        }
    }

    @Override
    public void enterVarDecl(SysYParser.VarDeclContext ctx) {
        for (SysYParser.VarDefContext varDef : ctx.varDef()) {
            String varName = varDef.IDENT().getText();
            Symbol existingSymbol = symbolTable.lookupCurrentScope(varName);

            if (existingSymbol != null) {
                reportError(SemanticError.REDEFINED_VARIABLE, varDef.IDENT().getSymbol(),
                        "Redefined variable: " + varName);
                continue;
            }

            Type varType = IntType.instance;

            // Handle array type
            if (!varDef.constExp().isEmpty()) {
                for (int i = varDef.constExp().size() - 1; i >= 0; i--) {
                    varType = new ArrayType(varType, 0); // Simplified, should compute actual size
                }
            }

            symbolTable.declareSymbol(varName, varType, varDef.IDENT().getSymbol().getLine());

            // Check initialization if present
            if (varDef.ASSIGN() != null) {
                checkInitialization(varDef, varType);
            }
        }
    }

    @Override
    public void enterConstDecl(SysYParser.ConstDeclContext ctx) {
        for (SysYParser.ConstDefContext constDef : ctx.constDef()) {
            String constName = constDef.IDENT().getText();
            Symbol existingSymbol = symbolTable.lookupCurrentScope(constName);

            if (existingSymbol != null) {
                reportError(SemanticError.REDEFINED_VARIABLE, constDef.IDENT().getSymbol(),
                        "Redefined variable: " + constName);
                continue;
            }

            Type constType = IntType.instance;

            // Handle array type
            if (!constDef.constExp().isEmpty()) {
                for (int i = constDef.constExp().size() - 1; i >= 0; i--) {
                    constType = new ArrayType(constType, 0); // Simplified, should compute actual size
                }
            }

            symbolTable.declareSymbol(constName, constType, constDef.IDENT().getSymbol().getLine());

            // Check initialization
            checkConstInitialization(constDef, constType);
        }
    }

    @Override
    public void enterStmt(SysYParser.StmtContext ctx) {
        // Handle assignment statements
        if (ctx.lVal() != null && ctx.ASSIGN() != null) {
            Type lvalType = checkLVal(ctx.lVal());
            Type expType = checkExp(ctx.exp());

            if (lvalType != null && expType != null && !lvalType.isEqual(expType)) {
                reportError(SemanticError.TYPE_MISMATCH_ASSIGNMENT, ctx.ASSIGN().getSymbol(),
                        "type.Type mismatched for assignment");
            }
        }

        // Handle return statements
        if (ctx.RETURN() != null) {
            if (ctx.exp() != null) {
                Type returnType = checkExp(ctx.exp());
                if (returnType != null && currentFunctionReturnType != null &&
                        !returnType.isEqual(currentFunctionReturnType)) {
                    reportError(SemanticError.TYPE_MISMATCH_RETURN, ctx.RETURN().getSymbol(),
                            "type.Type mismatched for return");
                }
            } else if (currentFunctionReturnType != null && !currentFunctionReturnType.isEqual(VoidType.instance)) {
                reportError(SemanticError.TYPE_MISMATCH_RETURN, ctx.RETURN().getSymbol(),
                        "type.Type mismatched for return");
            }
        }

        // Handle expressions
        if (ctx.exp() != null && ctx.SEMICOLON() != null && ctx.lVal() == null && ctx.RETURN() == null) {
            checkExp(ctx.exp());
        }

        // Handle if statements
        if (ctx.IF() != null) {
            checkCond(ctx.cond());
        }

        // Handle while statements
        if (ctx.WHILE() != null) {
            checkCond(ctx.cond());
        }
    }

    // Helper methods for type checking
    private Type checkLVal(SysYParser.LValContext ctx) {
        String name = ctx.IDENT().getText();
        Symbol symbol = symbolTable.lookupSymbol(name);

        if (symbol == null) {
            reportError(SemanticError.UNDEFINED_VARIABLE, ctx.IDENT().getSymbol(),
                    "Undefined variable: " + name);
            return null;
        }

        Type type = symbol.getType();

        // Handle array access
        if (!ctx.exp().isEmpty()) {
            if (type.getKind() != Type.TypeKind.ARRAY) {
                reportError(SemanticError.NOT_AN_ARRAY, ctx.IDENT().getSymbol(),
                        "Not an array: " + name);
                return null;
            }

            // Check each array index expression
            for (SysYParser.ExpContext exp : ctx.exp()) {
                Type indexType = checkExp(exp);
                if (indexType == null || !indexType.isEqual(IntType.instance)) {
                    reportError(SemanticError.TYPE_MISMATCH_OPERANDS, exp.getStart(),
                            "Array index must be an integer");
                }

                if (type.getKind() == Type.TypeKind.ARRAY) {
                    type = ((ArrayType) type).getElementType();
                } else {
                    reportError(SemanticError.NOT_AN_ARRAY, ctx.IDENT().getSymbol(),
                            "Not an array: " + name);
                    return null;
                }
            }
        }

        return type;
    }

    private Type checkExp(SysYParser.ExpContext ctx) {
        return checkAddExp(ctx.addExp());
    }

    private Type checkAddExp(SysYParser.AddExpContext ctx) {
        if (ctx.getChildCount() == 1) {
            return checkMulExp(ctx.mulExp());
        }

        Type leftType = checkAddExp(ctx.addExp());
        Type rightType = checkMulExp(ctx.mulExp());

        if (leftType == null || rightType == null) {
            return null;
        }

        if (!leftType.isEqual(IntType.instance)) {
            reportError(SemanticError.TYPE_MISMATCH_OPERANDS, ctx.getStart(),
                    "type.Type mismatched for operands");
            return null;
        }

        if (!rightType.isEqual(IntType.instance)) {
            reportError(SemanticError.TYPE_MISMATCH_OPERANDS, ctx.mulExp().getStart(),
                    "type.Type mismatched for operands");
            return null;
        }

        return IntType.instance;
    }

    private Type checkMulExp(SysYParser.MulExpContext ctx) {
        if (ctx.getChildCount() == 1) {
            return checkUnaryExp(ctx.unaryExp());
        }

        Type leftType = checkMulExp(ctx.mulExp());
        Type rightType = checkUnaryExp(ctx.unaryExp());

        if (leftType == null || rightType == null) {
            return null;
        }

        if (!leftType.isEqual(IntType.instance)) {
            reportError(SemanticError.TYPE_MISMATCH_OPERANDS, ctx.getStart(),
                    "Operand type mismatch");
            return null;
        }

        if (!rightType.isEqual(IntType.instance)) {
            reportError(SemanticError.TYPE_MISMATCH_OPERANDS, ctx.unaryExp().getStart(),
                    "Operand type mismatch");
            return null;
        }

        return IntType.instance;
    }

    private Type checkUnaryExp(SysYParser.UnaryExpContext ctx) {
        if (ctx.primaryExp() != null) {
            return checkPrimaryExp(ctx.primaryExp());
        } else if (ctx.IDENT() != null) {
            // Function call
            String funcName = ctx.IDENT().getText();
            Symbol symbol = symbolTable.lookupSymbol(funcName);

            if (symbol == null) {
                reportError(SemanticError.UNDEFINED_FUNCTION, ctx.IDENT().getSymbol(),
                        "Undefined function: " + funcName);
                return null;
            }

            if (symbol.getType().getKind() != Type.TypeKind.FUNCTION) {
                reportError(SemanticError.NOT_A_FUNCTION, ctx.IDENT().getSymbol(),
                        "Not a function: " + funcName);
                return null;
            }

            FunctionType functionType = (FunctionType) symbol.getType();

            // Check parameters
            if (ctx.funcRParams() != null) {
                List<SysYParser.ParamContext> params = ctx.funcRParams().param();
                List<Type> paramTypes = functionType.getParamTypes();

                if (params.size() != paramTypes.size()) {
                    reportError(SemanticError.FUNCTION_PARAM_MISMATCH, ctx.IDENT().getSymbol(),
                            "Function is not applicable for arguments");
                    return functionType.getReturnType();
                }

                for (int i = 0; i < params.size(); i++) {
                    Type argType = checkExp(params.get(i).exp());
                    if (argType != null && !argType.isEqual(paramTypes.get(i))) {
                        reportError(SemanticError.FUNCTION_PARAM_MISMATCH, params.get(i).getStart(),
                                "Function is not applicable for arguments");
                    }
                }
            } else if (!functionType.getParamTypes().isEmpty()) {
                reportError(SemanticError.FUNCTION_PARAM_MISMATCH, ctx.IDENT().getSymbol(),
                        "Function parameter count mismatch");
            }

            return functionType.getReturnType();
        } else {
            // Unary operator
            Type operandType = checkUnaryExp(ctx.unaryExp());

            if (operandType != null && !operandType.isEqual(IntType.instance)) {
                reportError(SemanticError.TYPE_MISMATCH_OPERANDS, ctx.unaryExp().getStart(),
                        "Operand type mismatch");
                return null;
            }

            return IntType.instance;
        }
    }

    private Type checkPrimaryExp(SysYParser.PrimaryExpContext ctx) {
        if (ctx.exp() != null) {
            return checkExp(ctx.exp());
        } else if (ctx.lVal() != null) {
            return checkLVal(ctx.lVal());
        } else if (ctx.number() != null) {
            return IntType.instance;
        }
        return null;
    }

    private void checkCond(SysYParser.CondContext ctx) {
        checkLOrExp(ctx.lOrExp());
    }

    private void checkLOrExp(SysYParser.LOrExpContext ctx) {
        if (ctx.getChildCount() == 1) {
            checkLAndExp(ctx.lAndExp());
        } else {
            checkLOrExp(ctx.lOrExp());
            checkLAndExp(ctx.lAndExp());
        }
    }

    private void checkLAndExp(SysYParser.LAndExpContext ctx) {
        if (ctx.getChildCount() == 1) {
            checkEqExp(ctx.eqExp());
        } else {
            checkLAndExp(ctx.lAndExp());
            checkEqExp(ctx.eqExp());
        }
    }

    private Type checkEqExp(SysYParser.EqExpContext ctx) {
        if (ctx.getChildCount() == 1) {
            return checkRelExp(ctx.relExp());
        } else {
            Type leftType = checkEqExp(ctx.eqExp());
            Type rightType = checkRelExp(ctx.relExp());

            if (leftType != null && rightType != null &&
                    !leftType.isEqual(rightType)) {
                reportError(SemanticError.TYPE_MISMATCH_OPERANDS, ctx.getStart(),
                        "Type mismatch in equality comparison");
            }

            // Equality operations result in boolean values (represented as int in SysY)
            return IntType.instance;
        }
    }

    private Type checkRelExp(SysYParser.RelExpContext ctx) {
        if (ctx.getChildCount() == 1) {
            return checkAddExp(ctx.addExp());
        } else {
            Type leftType = checkRelExp(ctx.relExp());
            Type rightType = checkAddExp(ctx.addExp());

            if (leftType != null && !leftType.isEqual(IntType.instance)) {
                reportError(SemanticError.TYPE_MISMATCH_OPERANDS, ctx.getStart(),
                        "Operand type mismatch");
            }

            if (rightType != null && !rightType.isEqual(IntType.instance)) {
                reportError(SemanticError.TYPE_MISMATCH_OPERANDS, ctx.addExp().getStart(),
                        "Operand type mismatch");
            }

            return IntType.instance;
        }
    }

    private void checkInitialization(SysYParser.VarDefContext ctx, Type varType) {
        if (ctx.initVal() != null) {
            checkInitValue(ctx.initVal(), varType);
        }
    }

    private void checkConstInitialization(SysYParser.ConstDefContext ctx, Type constType) {
        if (ctx.constInitVal() != null) {
            checkConstInitValue(ctx.constInitVal(), constType);
        }
    }

    private void checkInitValue(SysYParser.InitValContext ctx, Type expectedType) {
        if (ctx.exp() != null) {
            Type initType = checkExp(ctx.exp());
            if (initType != null && expectedType != null && !expectedType.isEqual(initType)) {
                reportError(SemanticError.TYPE_MISMATCH_OPERANDS, ctx.getStart(),
                        "Initialization type mismatch");
            }
        } else if (expectedType.getKind() == Type.TypeKind.ARRAY) {
            // Array initialization - we should check each element, but the spec says
            // we don't need to check array initialization values
        } else {
            reportError(SemanticError.TYPE_MISMATCH_OPERANDS, ctx.getStart(),
                    "Initialization type mismatch");
        }
    }

    private void checkConstInitValue(SysYParser.ConstInitValContext ctx, Type expectedType) {
        if (ctx.constExp() != null) {
            Type initType = checkExp(ctx.constExp().exp());
            if (initType != null && expectedType != null && !expectedType.isEqual(initType)) {
                reportError(SemanticError.TYPE_MISMATCH_OPERANDS, ctx.getStart(),
                        "Initialization type mismatch");
            }
        } else if (expectedType.getKind() == Type.TypeKind.ARRAY) {
            // Array initialization - we should check each element, but the spec says
            // we don't need to check array initialization values
        } else {
            reportError(SemanticError.TYPE_MISMATCH_OPERANDS, ctx.getStart(),
                    "Initialization type mismatch");
        }
    }
}