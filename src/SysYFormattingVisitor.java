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

    @Override
    public String visitBType(SysYParser.BTypeContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitVarDecl(SysYParser.VarDeclContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent()).append(visit(ctx.bType())).append(" ");

        for (int i = 0; i < ctx.varDef().size(); i++) {
            sb.append(visit(ctx.varDef(i)));
            if (i < ctx.varDef().size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(";");
        return sb.toString();
    }

    @Override
    public String visitVarDef(SysYParser.VarDefContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(ctx.IDENT().getText());

        for (SysYParser.ConstExpContext expCtx : ctx.constExp()) {
            sb.append("[").append(visit(expCtx)).append("]");
        }

        if (ctx.ASSIGN() != null) {
            sb.append(" = ").append(visit(ctx.initVal()));
        }

        return sb.toString();
    }

    @Override
    public String visitInitVal(SysYParser.InitValContext ctx) {
        if (ctx.exp() != null) {
            return visit(ctx.exp());
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            for (int i = 0; i < ctx.initVal().size(); i++) {
                sb.append(visit(ctx.initVal(i)));
                if (i < ctx.initVal().size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("}");
            return sb.toString();
        }
    }

    @Override
    public String visitFuncDef(SysYParser.FuncDefContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(visit(ctx.funcType())).append(" ");
        sb.append(ctx.IDENT().getText()).append("(");

        if (ctx.funcFParams() != null) {
            sb.append(visit(ctx.funcFParams()));
        }

        sb.append(") ");
        sb.append(visit(ctx.block()));
        return sb.toString();
    }

    @Override
    public String visitFuncType(SysYParser.FuncTypeContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitFuncFParams(SysYParser.FuncFParamsContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ctx.funcFParam().size(); i++) {
            sb.append(visit(ctx.funcFParam(i)));
            if (i < ctx.funcFParam().size() - 1) {
                sb.append(", ");
            }
        }
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
        sb.append(getIndent()).append("}");
        return sb.toString();
    }

    @Override
    public String visitBlockItem(SysYParser.BlockItemContext ctx) {
        return visit(ctx.getChild(0));
    }

    @Override
    public String visitStmt(SysYParser.StmtContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(getIndent());

        if (ctx.lVal() != null && ctx.ASSIGN() != null) {
            // Assignment statement
            sb.append(visit(ctx.lVal())).append(" = ").append(visit(ctx.exp())).append(";\n");
        } else if (ctx.SEMICOLON() != null && ctx.exp() == null && ctx.RETURN() == null) {
            // Empty statement
            sb.append(";\n");
        } else if (ctx.RETURN() != null) {
            // Return statement
            sb.append("return");
            if (ctx.exp() != null) {
                sb.append(" ").append(visit(ctx.exp()));
            }
            sb.append(";\n");
        } else if (ctx.exp() != null && ctx.SEMICOLON() != null) {
            // Expression statement
            sb.append(visit(ctx.exp())).append(";\n");
        } else if (ctx.block() != null) {
            // Block statement
            sb.append(visit(ctx.block())).append("\n");
        } else if (ctx.IF() != null) {
            // If statement
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
        } else if (ctx.WHILE() != null) {
            // While statement
            sb.append("while (").append(visit(ctx.cond())).append(") ");
            if (ctx.stmt(0).block() != null) {
                sb.append(visit(ctx.stmt(0)));
            } else {
                sb.append("\n");
                indentLevel++;
                sb.append(visit(ctx.stmt(0)));
                indentLevel--;
            }
        } else if (ctx.BREAK() != null) {
            sb.append("break;\n");
        } else if (ctx.CONTINUE() != null) {
            sb.append("continue;\n");
        }

        return sb.toString();
    }

    @Override
    public String visitExp(SysYParser.ExpContext ctx) {
        return visit(ctx.addExp());
    }

    @Override
    public String visitCond(SysYParser.CondContext ctx) {
        return visit(ctx.lOrExp());
    }

    @Override
    public String visitLVal(SysYParser.LValContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(ctx.IDENT().getText());

        for (SysYParser.ExpContext expCtx : ctx.exp()) {
            sb.append("[").append(visit(expCtx)).append("]");
        }

        return sb.toString();
    }

    @Override
    public String visitPrimaryExp(SysYParser.PrimaryExpContext ctx) {
        if (ctx.L_PAREN() != null) {
            return "(" + visit(ctx.exp()) + ")";
        } else if (ctx.lVal() != null) {
            return visit(ctx.lVal());
        } else {
            return visit(ctx.number());
        }
    }

    @Override
    public String visitNumber(SysYParser.NumberContext ctx) {
        return ctx.INTEGER_CONST().getText();
    }

    @Override
    public String visitUnaryExp(SysYParser.UnaryExpContext ctx) {
        if (ctx.primaryExp() != null) {
            return visit(ctx.primaryExp());
        } else if (ctx.IDENT() != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(ctx.IDENT().getText()).append("(");
            if (ctx.funcRParams() != null) {
                sb.append(visit(ctx.funcRParams()));
            }
            sb.append(")");
            return sb.toString();
        } else {
            return ctx.getChild(0).getText() + visit(ctx.unaryExp());
        }
    }

    @Override
    public String visitFuncRParams(SysYParser.FuncRParamsContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ctx.param().size(); i++) {
            sb.append(visit(ctx.param(i)));
            if (i < ctx.param().size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    @Override
    public String visitParam(SysYParser.ParamContext ctx) {
        return visit(ctx.exp());
    }

    @Override
    public String visitMulExp(SysYParser.MulExpContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visit(ctx.unaryExp());
        } else {
            return visit(ctx.mulExp()) + " " + ctx.getChild(1).getText() + " " + visit(ctx.unaryExp());
        }
    }

    @Override
    public String visitAddExp(SysYParser.AddExpContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visit(ctx.mulExp());
        } else {
            return visit(ctx.addExp()) + " " + ctx.getChild(1).getText() + " " + visit(ctx.mulExp());
        }
    }

    @Override
    public String visitRelExp(SysYParser.RelExpContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visit(ctx.addExp());
        } else {
            return visit(ctx.relExp()) + " " + ctx.getChild(1).getText() + " " + visit(ctx.addExp());
        }
    }

    @Override
    public String visitEqExp(SysYParser.EqExpContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visit(ctx.relExp());
        } else {
            return visit(ctx.eqExp()) + " " + ctx.getChild(1).getText() + " " + visit(ctx.relExp());
        }
    }

    @Override
    public String visitLAndExp(SysYParser.LAndExpContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visit(ctx.eqExp());
        } else {
            return visit(ctx.lAndExp()) + " && " + visit(ctx.eqExp());
        }
    }

    @Override
    public String visitLOrExp(SysYParser.LOrExpContext ctx) {
        if (ctx.getChildCount() == 1) {
            return visit(ctx.lAndExp());
        } else {
            return visit(ctx.lOrExp()) + " || " + visit(ctx.lAndExp());
        }
    }

    @Override
    public String visitConstExp(SysYParser.ConstExpContext ctx) {
        return visit(ctx.exp());
    }
}