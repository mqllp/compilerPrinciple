import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.llvm4j.llvm4j.*;
import org.llvm4j.llvm4j.Context;
import org.llvm4j.llvm4j.Module;
import org.llvm4j.llvm4j.Value;

public class MyVisitor extends SysYParserBaseVisitor<Value> {
    private Context context;
    private Module module;
    private IRBuilder builder;

    public MyVisitor() {
        this.context = Context.create();
        this.module = Module.create("module", this.context);
        this.module.setSourceFileName("module");
    }

    @Override
    public Value visitProgram(SysYParser.ProgramContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public Value visitCompUnit(SysYParser.CompUnitContext ctx) {
        // 遍历CompUnit的所有子节点
        for (SysYParser.FuncDefContext funcDef : ctx.funcDef()) {
            visit(funcDef);
        }
        return null;
    }

    @Override
    public Value visitFuncDef(SysYParser.FuncDefContext ctx) {
        // 处理函数定义，目前只处理main函数
        String funcName = ctx.IDENT().getText();
        if (funcName.equals("main")) {
            // 创建main函数
            Type returnType = Type.getInt32Ty(context);
            FunctionType funcType = FunctionType.get(returnType, false);
            Function mainFunc = Function.create(funcType, Function.LinkageTypes.EXTERNAL, "main", module);

            // 创建基本块
            BasicBlock entryBlock = BasicBlock.create(context, "mainEntry", mainFunc);
            builder = IRBuilder.create(entryBlock);

            // 处理函数体
            visit(ctx.block());

            // 如果函数体没有返回语句，添加默认返回0
            if (!entryBlock.getTerminator().isPresent()) {
                builder.createRet(ConstantInt.get(Type.getInt32Ty(context), 0, true));
            }

            return mainFunc;
        }
        return null;
    }

    @Override
    public Value visitBlock(SysYParser.BlockContext ctx) {
        // 遍历block中的所有语句
        for (SysYParser.BlockItemContext blockItem : ctx.blockItem()) {
            visit(blockItem);
        }
        return null;
    }

    @Override
    public Value visitBlockItem(SysYParser.BlockItemContext ctx) {
        if (ctx.stmt() != null) {
            return visit(ctx.stmt());
        }
        return null;
    }

    @Override
    public Value visitStmt(SysYParser.StmtContext ctx) {
        // 处理return语句
        if (ctx.RETURN() != null) {
            Value retValue = visit(ctx.exp());
            builder.createRet(retValue);
            return null;
        }
        return visitChildren(ctx);
    }

    @Override
    public Value visitExp(SysYParser.ExpContext ctx) {
        return visit(ctx.addExp());
    }

    @Override
    public Value visitAddExp(SysYParser.AddExpContext ctx) {
        if (ctx.addExp() == null) {
            return visit(ctx.mulExp());
        }

        Value left = visit(ctx.addExp());
        Value right = visit(ctx.mulExp());

        if (ctx.PLUS() != null) {
            return builder.createAdd(left, right, "addtmp");
        } else if (ctx.MINUS() != null) {
            return builder.createSub(left, right, "subtmp");
        }

        return null;
    }

    @Override
    public Value visitMulExp(SysYParser.MulExpContext ctx) {
        if (ctx.mulExp() == null) {
            return visit(ctx.unaryExp());
        }

        Value left = visit(ctx.mulExp());
        Value right = visit(ctx.unaryExp());

        if (ctx.MUL() != null) {
            return builder.createMul(left, right, "multmp");
        } else if (ctx.DIV() != null) {
            return builder.createSDiv(left, right, "divtmp");
        } else if (ctx.MOD() != null) {
            return builder.createSRem(left, right, "modtmp");
        }

        return null;
    }

    @Override
    public Value visitUnaryExp(SysYParser.UnaryExpContext ctx) {
        if (ctx.primaryExp() != null) {
            return visit(ctx.primaryExp());
        }

        // 处理一元操作
        Value val = visit(ctx.unaryExp());

        if (ctx.PLUS() != null) {
            return val; // +n = n
        } else if (ctx.MINUS() != null) {
            return builder.createNeg(val, "negtmp");
        }

        return null;
    }

    @Override
    public Value visitPrimaryExp(SysYParser.PrimaryExpContext ctx) {
        if (ctx.number() != null) {
            return visit(ctx.number());
        } else if (ctx.exp() != null) {
            return visit(ctx.exp());
        }
        return null;
    }

    @Override
    public Value visitNumber(SysYParser.NumberContext ctx) {
        // 解析整数字面值（十进制、八进制、十六进制）
        String numText = ctx.INTEGER_CONST().getText();
        int value;

        if (numText.startsWith("0x") || numText.startsWith("0X")) {
            // 十六进制
            value = Integer.parseInt(numText.substring(2), 16);
        } else if (numText.startsWith("0") && numText.length() > 1) {
            // 八进制
            value = Integer.parseInt(numText.substring(1), 8);
        } else {
            // 十进制
            value = Integer.parseInt(numText);
        }

        return ConstantInt.get(Type.getInt32Ty(context), value, true);
    }

    public Module getModule() {
        return this.module;
    }
}