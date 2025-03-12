import org.antlr.v4.runtime.tree.ParseTree;

public class SysYFormattingVisitor extends SysYParserBaseVisitor<String> {
    private int indentLevel = 0;
    private final String INDENT = "    ";

    private String getIndent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            sb.append(INDENT);
        }
        return sb.toString();
    }

    @Override
    public String visitProgram(SysYParser.ProgramContext ctx) {
        return visit(ctx.compUnit());
    }

    @Override
    public String visitCompUnit(SysYParser.CompUnitContext ctx) {
        StringBuilder sb = new StringBuilder();
        boolean firstFunc = true;

        for (int i = 0; i < ctx.getChildCount() - 1; i++) { // Skip EOF
            ParseTree child = ctx.getChild(i);
            if (child instanceof SysYParser.FuncDefContext) {
                if (!firstFunc) {
                    sb.append("\n");
                }
                firstFunc = false;
            }
            sb.append(visit(child));
        }

        return sb.toString();
    }

    @Override
    public String visitDecl(SysYParser.DeclContext ctx) {
        return visit(ctx.getChild(0)) + "\n";
    }

    @Override
    public String visitConstDecl(SysYParser.ConstDeclContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent()).append("const ");
        sb.append(visit(ctx.bType())).append(" ");

        for (int i = 0; i < ctx.constDef().size(); i++) {
            sb.append(visit(ctx.constDef(i)));
            if (i < ctx.constDef().size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(";");
        return sb.toString();
    }

    // Other visit methods for all rule contexts...

    @Override
    public String visitFuncDef(SysYParser.FuncDefContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent()).append(visit(ctx.funcType())).append(" ");
        sb.append(ctx.IDENT().getText()).append("(");

        if (ctx.funcFParams() != null) {
            sb.append(visit(ctx.funcFParams()));
        }

        sb.append(") ");
        sb.append(visit(ctx.block()));
        return sb.toString();
    }

    @Override
    public String visitBlock(SysYParser.BlockContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        indentLevel++;

        for (SysYParser.BlockItemContext item : ctx.blockItem()) {
            sb.append(visit(item));
        }

        indentLevel--;
        sb.append(getIndent()).append("}\n");
        return sb.toString();
    }

    @Override
    public String visitStmt(SysYParser.StmtContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent());

        if (ctx.IF() != null) {
            sb.append("if (").append(visit(ctx.cond())).append(") ");

            if (ctx.stmt(0).block() != null) {
                sb.append(visit(ctx.stmt(0)));
            } else {
                sb.append("\n");
                indentLevel++;
                sb.append(visit(ctx.stmt(0)));
                indentLevel--;
            }

            if (ctx.ELSE() != null) {
                sb.append(getIndent()).append("else");

                // Handle else if special case
                if (ctx.stmt(1).IF() != null) {
                    sb.append(" ").append(visit(ctx.stmt(1)));
                } else if (ctx.stmt(1).block() != null) {
                    sb.append(" ").append(visit(ctx.stmt(1)));
                } else {
                    sb.append("\n");
                    indentLevel++;
                    sb.append(visit(ctx.stmt(1)));
                    indentLevel--;
                }
            }
        }
        // Handle other statement types...

        return sb.toString();
    }

    // Methods for expressions, binary ops, etc.
    @Override
    public String visitAddExp(SysYParser.AddExpContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visit(ctx.mulExp());
        } else {
            return visit(ctx.addExp()) + " " + ctx.getChild(1).getText() + " " + visit(ctx.mulExp());
        }
    }

    // And many more methods for all other syntax elements
}