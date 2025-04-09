import org.llvm4j.llvm4j.*;
import org.llvm4j.optional.Option;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.ArrayList;
import java.util.List;

public class IRVisitor extends SysYParserBaseVisitor<Value> {
    private Context context;
    private Module module;
    private IRBuilder builder;
    private IRSymbolTable symbolTable;
    private Function currentFunction;
    private BasicBlock currentBlock;
    // 用于处理循环的基本块
    private List<BasicBlock> continueBlocks;
    private List<BasicBlock> breakBlocks;

    public IRVisitor(String moduleName) {
        this.context = Context.create();
        this.module = context.newModule(moduleName);
        this.builder = context.newIRBuilder();
        this.symbolTable = new IRSymbolTable();
        this.continueBlocks = new ArrayList<>();
        this.breakBlocks = new ArrayList<>();
    }

    public Module getModule() {
        return module;
    }

    @Override
    public Value visitProgram(SysYParser.ProgramContext ctx) {
        // 访问所有的声明
        ctx.decl().forEach(this::visit);
        // 访问所有的函数定义
        ctx.funcDef().forEach(this::visit);
        return null;
    }

    @Override
    public Value visitVarDecl(SysYParser.VarDeclContext ctx) {
        String bType = ctx.bType().getText();
        Type varType;

        // 根据类型确定LLVM类型
        if (bType.equals("int")) {
            varType = context.getInt32Type();
        } else {
            throw new RuntimeException("Unsupported type: " + bType);
        }

        // 处理所有变量定义
        for (SysYParser.VarDefContext varDef : ctx.varDef()) {
            String varName = varDef.IDENT().getText();

            // 检查是否为全局变量
            if (currentFunction == null) {
                // 全局变量
                Value globalVar = module.addGlobalVariable(
                        varName,
                        varType,
                        Option.empty()
                );

                // 如果有初始值
                if (varDef.ASSIGN() != null) {
                    Value initValue = visit(varDef.initVal());
                    ((GlobalVariable)globalVar).setInitializer(initValue);
                }

                symbolTable.put(varName, globalVar);
            } else {
                // 局部变量
                Value localVar = builder.alloca(varType);

                // 如果有初始值
                if (varDef.ASSIGN() != null) {
                    Value initValue = visit(varDef.initVal());
                    builder.store(initValue, localVar);
                }

                symbolTable.put(varName, localVar);
            }
        }
        return null;
    }

    @Override
    public Value visitFuncDef(SysYParser.FuncDefContext ctx) {
        String funcName = ctx.IDENT().getText();

        // 处理返回值类型
        Type returnType;
        if (ctx.funcType().getText().equals("int")) {
            returnType = context.getInt32Type();
        } else if (ctx.funcType().getText().equals("void")) {
            returnType = context.getVoidType();
        } else {
            throw new RuntimeException("Unsupported return type: " + ctx.funcType().getText());
        }

        // 处理参数类型
        List<Type> paramTypes = new ArrayList<>();
        List<String> paramNames = new ArrayList<>();

        if (ctx.funcFParams() != null) {
            for (SysYParser.FuncFParamContext param : ctx.funcFParams().funcFParam()) {
                paramTypes.add(context.getInt32Type()); // 目前只支持int类型参数
                paramNames.add(param.IDENT().getText());
            }
        }

        // 创建函数类型
        FunctionType funcType = context.getFunctionType(
                returnType,
                paramTypes.toArray(new Type[0]),
                false
        );

        // 创建函数
        Function function = module.addFunction(funcName, funcType);
        this.currentFunction = function;

        // 创建入口基本块
        BasicBlock entryBlock = context.newBasicBlock("entry");
        function.appendBasicBlock(entryBlock);
        this.currentBlock = entryBlock;
        builder.positionAfter(entryBlock);

        // 进入新的作用域
        symbolTable.enterScope();

        // 处理参数
        for (int i = 0; i < paramNames.size(); i++) {
            Value paramAlloca = builder.alloca(context.getInt32Type());
            builder.store(function.getParameter(i), paramAlloca);
            symbolTable.put(paramNames.get(i), paramAlloca);
        }

        // 访问函数体
        visit(ctx.block());

        // 如果是void函数，确保有返回语句
        if (returnType == context.getVoidType() &&
                !builder.getCurrentBasicBlock().isTerminated()) {
            builder.retVoid();
        }

        // 退出作用域
        symbolTable.exitScope();
        this.currentFunction = null;

        return function;
    }

    @Override
    public Value visitBlock(SysYParser.BlockContext ctx) {
        // 如果不是函数的第一个块，则需要创建新的作用域
        if (ctx.getParent() instanceof SysYParser.FuncDefContext) {
            // 函数的第一个块已经创建了作用域
        } else {
            symbolTable.enterScope();
        }

        // 访问块中的所有语句
        for (SysYParser.BlockItemContext item : ctx.blockItem()) {
            visit(item);
        }

        // 退出作用域
        if (!(ctx.getParent() instanceof SysYParser.FuncDefContext)) {
            symbolTable.exitScope();
        }

        return null;
    }

    @Override
    public Value visitStmt(SysYParser.StmtContext ctx) {
        if (ctx.RETURN() != null) {
            // return 语句
            if (ctx.exp() != null) {
                Value returnValue = visit(ctx.exp());
                builder.ret(returnValue);
            } else {
                builder.retVoid();
            }
        } else if (ctx.IF() != null) {
            // if 语句
            Value condition = visit(ctx.cond());

            BasicBlock thenBlock = context.newBasicBlock("if.then");
            BasicBlock elseBlock = ctx.ELSE() != null ?
                    context.newBasicBlock("if.else") : null;
            BasicBlock mergeBlock = context.newBasicBlock("if.end");

            if (ctx.ELSE() != null) {
                builder.condBr(condition, thenBlock, elseBlock);
            } else {
                builder.condBr(condition, thenBlock, mergeBlock);
            }

            // 处理 then 分支
            currentFunction.appendBasicBlock(thenBlock);
            builder.positionAfter(thenBlock);
            visit(ctx.stmt(0));
            if (!builder.getCurrentBasicBlock().isTerminated()) {
                builder.br(mergeBlock);
            }

            // 处理 else 分支
            if (ctx.ELSE() != null) {
                currentFunction.appendBasicBlock(elseBlock);
                builder.positionAfter(elseBlock);
                visit(ctx.stmt(1));
                if (!builder.getCurrentBasicBlock().isTerminated()) {
                    builder.br(mergeBlock);
                }
            }

            // 合并块
            currentFunction.appendBasicBlock(mergeBlock);
            builder.positionAfter(mergeBlock);
        } else if (ctx.WHILE() != null) {
            // while 语句
            BasicBlock condBlock = context.newBasicBlock("while.cond");
            BasicBlock bodyBlock = context.newBasicBlock("while.body");
            BasicBlock endBlock = context.newBasicBlock("while.end");

            // 跳转到条件块
            builder.br(condBlock);

            // 条件块
            currentFunction.appendBasicBlock(condBlock);
            builder.positionAfter(condBlock);
            Value condition = visit(ctx.cond());
            builder.condBr(condition, bodyBlock, endBlock);

            // 循环体
            currentFunction.appendBasicBlock(bodyBlock);
            builder.positionAfter(bodyBlock);

            // 保存break和continue目标
            breakBlocks.add(endBlock);
            continueBlocks.add(condBlock);

            visit(ctx.stmt(0));

            // 恢复break和continue目标
            breakBlocks.remove(breakBlocks.size() - 1);
            continueBlocks.remove(continueBlocks.size() - 1);

            if (!builder.getCurrentBasicBlock().isTerminated()) {
                builder.br(condBlock);
            }

            // 结束块
            currentFunction.appendBasicBlock(endBlock);
            builder.positionAfter(endBlock);
        } else if (ctx.BREAK() != null) {
            // break 语句
            if (breakBlocks.isEmpty()) {
                throw new RuntimeException("Break statement outside loop");
            }
            builder.br(breakBlocks.get(breakBlocks.size() - 1));
        } else if (ctx.CONTINUE() != null) {
            // continue 语句
            if (continueBlocks.isEmpty()) {
                throw new RuntimeException("Continue statement outside loop");
            }
            builder.br(continueBlocks.get(continueBlocks.size() - 1));
        } else if (ctx.lVal() != null && ctx.ASSIGN() != null) {
            // 赋值语句
            Value rValue = visit(ctx.exp());
            Value lValue = symbolTable.get(ctx.lVal().IDENT().getText());
            builder.store(rValue, lValue);
        }
        return null;
    }

    @Override
    public Value visitExp(SysYParser.ExpContext ctx) {
        return visit(ctx.addExp());
    }

    @Override
    public Value visitAddExp(SysYParser.AddExpContext ctx) {
        Value result = visit(ctx.mulExp(0));

        for (int i = 1; i < ctx.mulExp().size(); i++) {
            Value right = visit(ctx.mulExp(i));
            String op = ctx.PLUS_OR_MINUS(i-1).getText();

            if (op.equals("+")) {
                result = builder.add(result, right);
            } else {
                result = builder.sub(result, right);
            }
        }

        return result;
    }

    @Override
    public Value visitMulExp(SysYParser.MulExpContext ctx) {
        Value result = visit(ctx.unaryExp(0));

        for (int i = 1; i < ctx.unaryExp().size(); i++) {
            Value right = visit(ctx.unaryExp(i));
            String op = ctx.MUL_OR_DIV(i-1).getText();

            if (op.equals("*")) {
                result = builder.mul(result, right);
            } else if (op.equals("/")) {
                result = builder.sdiv(result, right);
            } else if (op.equals("%")) {
                result = builder.srem(result, right);
            }
        }

        return result;
    }

    @Override
    public Value visitUnaryExp(SysYParser.UnaryExpContext ctx) {
        if (ctx.primaryExp() != null) {
            return visit(ctx.primaryExp());
        } else if (ctx.IDENT() != null) {
            // 函数调用
            String funcName = ctx.IDENT().getText();
            Function callee = module.getFunction(funcName);
            if (callee == null) {
                throw new RuntimeException("Unknown function: " + funcName);
            }

            List<Value> args = new ArrayList<>();
            if (ctx.funcRParams() != null) {
                for (SysYParser.ExpContext exp : ctx.funcRParams().exp()) {
                    args.add(visit(exp));
                }
            }

            return builder.call(callee, args.toArray(new Value[0]));
        } else {
            Value operand = visit(ctx.unaryExp());
            String op = ctx.unaryOp().getText();

            if (op.equals("+")) {
                return operand;
            } else if (op.equals("-")) {
                return builder.neg(operand);
            } else if (op.equals("!")) {
                // 先转换为布尔值（0或1）
                Value isZero = builder.icmp(
                        IntPredicate.EQ,
                        operand,
                        context.getInt32Type().constant(0)
                );
                // 然后将布尔值转换为int
                return builder.zext(isZero, context.getInt32Type());
            }
        }
        return null;
    }

    @Override
    public Value visitPrimaryExp(SysYParser.PrimaryExpContext ctx) {
        if (ctx.exp() != null) {
            return visit(ctx.exp());
        } else if (ctx.lVal() != null) {
            Value ptr = symbolTable.get(ctx.lVal().IDENT().getText());
            return builder.load(ptr);
        } else if (ctx.number() != null) {
            int value = Integer.parseInt(ctx.number().getText());
            return context.getInt32Type().constant(value);
        }
        return null;
    }

    @Override
    public Value visitCond(SysYParser.CondContext ctx) {
        return visit(ctx.lOrExp());
    }

    @Override
    public Value visitLOrExp(SysYParser.LOrExpContext ctx) {
        if (ctx.lAndExp().size() == 1) {
            return visit(ctx.lAndExp(0));
        }

        BasicBlock current = builder.getCurrentBasicBlock();
        BasicBlock trueBlock = context.newBasicBlock("lor.true");
        BasicBlock falseBlock = context.newBasicBlock("lor.false");
        BasicBlock mergeBlock = context.newBasicBlock("lor.merge");

        Value result = builder.alloca(context.getInt32Type());

        for (int i = 0; i < ctx.lAndExp().size() - 1; i++) {
            Value cond = visit(ctx.lAndExp(i));
            Value isTrue = builder.icmp(
                    IntPredicate.NE,
                    cond,
                    context.getInt32Type().constant(0)
            );
            builder.condBr(isTrue, trueBlock, current);
            current = builder.getCurrentBasicBlock();
        }

        Value lastCond = visit(ctx.lAndExp(ctx.lAndExp().size() - 1));
        builder.br(mergeBlock);

        currentFunction.appendBasicBlock(trueBlock);
        builder.positionAfter(trueBlock);
        builder.store(context.getInt32Type().constant(1), result);
        builder.br(mergeBlock);

        currentFunction.appendBasicBlock(falseBlock);
        builder.positionAfter(falseBlock);
        builder.store(context.getInt32Type().constant(0), result);
        builder.br(mergeBlock);

        currentFunction.appendBasicBlock(mergeBlock);
        builder.positionAfter(mergeBlock);
        return builder.load(result);
    }

    @Override
    public Value visitLAndExp(SysYParser.LAndExpContext ctx) {
        if (ctx.eqExp().size() == 1) {
            return visit(ctx.eqExp(0));
        }

        Value result = visit(ctx.eqExp(0));
        for (int i = 1; i < ctx.eqExp().size(); i++) {
            Value right = visit(ctx.eqExp(i));
            Value leftCond = builder.icmp(
                    IntPredicate.NE,
                    result,
                    context.getInt32Type().constant(0)
            );
            Value rightCond = builder.icmp(
                    IntPredicate.NE,
                    right,
                    context.getInt32Type().constant(0)
            );
            Value and = builder.and(leftCond, rightCond);
            result = builder.zext(and, context.getInt32Type());
        }
        return result;
    }

    @Override
    public Value visitEqExp(SysYParser.EqExpContext ctx) {
        Value result = visit(ctx.relExp(0));

        for (int i = 1; i < ctx.relExp().size(); i++) {
            Value right = visit(ctx.relExp(i));
            String op = ctx.EQ_OR_NEQ(i-1).getText();

            Value cmp;
            if (op.equals("==")) {
                cmp = builder.icmp(IntPredicate.EQ, result, right);
            } else {
                cmp = builder.icmp(IntPredicate.NE, result, right);
            }
            result = builder.zext(cmp, context.getInt32Type());
        }

        return result;
    }

    @Override
    public Value visitRelExp(SysYParser.RelExpContext ctx) {
        Value result = visit(ctx.addExp(0));

        for (int i = 1; i < ctx.addExp().size(); i++) {
            Value right = visit(ctx.addExp(i));
            String op = ctx.REL_OP(i-1).getText();

            Value cmp;
            switch (op) {
                case "<":
                    cmp = builder.icmp(IntPredicate.SLT, result, right);
                    break;
                case "<=":
                    cmp = builder.icmp(IntPredicate.SLE, result, right);
                    break;
                case ">":
                    cmp = builder.icmp(IntPredicate.SGT, result, right);
                    break;
                case ">=":
                    cmp = builder.icmp(IntPredicate.SGE, result, right);
                    break;
                default:
                    throw new RuntimeException("Unknown relational operator: " + op);
            }
            result = builder.zext(cmp, context.getInt32Type());
        }

        return result;
    }
}