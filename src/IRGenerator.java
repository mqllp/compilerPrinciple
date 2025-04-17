import org.antlr.v4.runtime.tree.ParseTree;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;
import org.bytedeco.llvm.global.LLVM;
import symbol.SymbolTable;

import java.util.*;

import static org.bytedeco.llvm.global.LLVM.*;

public class IRGenerator extends SysYParserBaseVisitor<LLVMValueRef> {
    private LLVMModuleRef module;
    private LLVMBuilderRef builder;
    private SymbolTable symbolTable;
    private LLVMValueRef currentFunction;
    private Stack<LLVMBasicBlockRef> breakBlocks; // break语句跳转的块
    private Stack<LLVMBasicBlockRef> continueBlocks; // continue语句跳转的块

    // 常用类型
    private LLVMTypeRef i32Type = LLVMInt32Type();
    private LLVMTypeRef voidType = LLVMVoidType();
    // 常量0，用于比较和初始化
    private LLVMValueRef zero = LLVMConstInt(i32Type, 0, 0);

    public IRGenerator(LLVMModuleRef module, LLVMBuilderRef builder) {
        this.module = module;
        this.builder = builder;
        this.symbolTable = new SymbolTable();
        this.breakBlocks = new Stack<>();
        this.continueBlocks = new Stack<>();
    }


    private boolean isPreviousInstructionBranch(LLVMBasicBlockRef block) {
        LLVMValueRef lastInstruction = LLVMGetLastInstruction(block);
        if (lastInstruction != null) {
            int opcode = LLVM.LLVMGetInstructionOpcode(lastInstruction);
            return opcode == LLVMRet || opcode == LLVMBr;
        }
        return false;
    }

    @Override
    public LLVMValueRef visitProgram(SysYParser.ProgramContext ctx) {
        return visit(ctx.compUnit());
    }

    @Override
    public LLVMValueRef visitCompUnit(SysYParser.CompUnitContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (!(child instanceof SysYParser.ProgramContext)) {
                visit(child);
            }
        }
        return null;
    }

    @Override
    public LLVMValueRef visitDecl(SysYParser.DeclContext ctx) {
        return visit(ctx.getChild(0));
    }

    @Override
    public LLVMValueRef visitVarDecl(SysYParser.VarDeclContext ctx) {
        // 获取类型
        LLVMTypeRef type = i32Type; // SysY中只有int类型

        // 处理所有变量定义
        for (SysYParser.VarDefContext varDef : ctx.varDef()) {
            String varName = varDef.IDENT().getText();

            // 处理全局变量和局部变量
            if (currentFunction == null) {
                // 全局变量
                LLVMValueRef globalVar = LLVMAddGlobal(module, type, varName);

                // 处理初始化
                if (varDef.ASSIGN() != null) {
                    LLVMValueRef initValue = visit(varDef.initVal());
                    LLVMSetInitializer(globalVar, initValue);
                } else {
                    // 默认初始化为0
                    LLVMSetInitializer(globalVar, zero);
                }

                symbolTable.put(varName, globalVar);
            } else {
                // 局部变量
                LLVMValueRef localVar = LLVMBuildAlloca(builder, type, varName);

                // 处理初始化
                if (varDef.ASSIGN() != null) {
                    LLVMValueRef initValue = visit(varDef.initVal());
                    LLVMBuildStore(builder, initValue, localVar);
                }

                symbolTable.put(varName, localVar);
            }
        }

        return null;
    }

    @Override
    public LLVMValueRef visitConstDecl(SysYParser.ConstDeclContext ctx) {
        // 处理常量声明，与变量声明类似，但常量必须初始化
        LLVMTypeRef type = i32Type; // SysY中只有int类型常量

        for (SysYParser.ConstDefContext constDef : ctx.constDef()) {
            String constName = constDef.IDENT().getText();

            if (currentFunction == null) {
                // 全局常量
                LLVMValueRef globalConst = LLVMAddGlobal(module, type, constName);
                LLVMValueRef initValue = visit(constDef.constInitVal());
                LLVMSetInitializer(globalConst, initValue);
                symbolTable.put(constName, globalConst);
            } else {
                // 局部常量
                LLVMValueRef localConst = LLVMBuildAlloca(builder, type, constName);
                LLVMValueRef initValue = visit(constDef.constInitVal());
                LLVMBuildStore(builder, initValue, localConst);
                symbolTable.put(constName, localConst);
            }
        }

        return null;
    }

    @Override
    public LLVMValueRef visitConstInitVal(SysYParser.ConstInitValContext ctx) {
        // 只处理简单的常量表达式，数组暂不处理
        if (ctx.constExp() != null) {
            return visit(ctx.constExp());
        }
        return zero;
    }

    @Override
    public LLVMValueRef visitConstExp(SysYParser.ConstExpContext ctx) {
        return visit(ctx.exp());
    }

    @Override
    public LLVMValueRef visitInitVal(SysYParser.InitValContext ctx) {
        // 只处理简单的表达式初始化，数组暂不处理
        if (ctx.exp() != null) {
            return visit(ctx.exp());
        }
        return zero;
    }

    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {
        String funcName = ctx.IDENT().getText();
        boolean isVoid = ctx.funcType().VOID() != null;
        LLVMTypeRef returnType = isVoid ? voidType : i32Type;

        // 处理函数参数
        int paramCount = ctx.funcFParams() != null ? ctx.funcFParams().funcFParam().size() : 0;
        PointerPointer<Pointer> paramTypes = new PointerPointer<>(paramCount);
        for (int i = 0; i < paramCount; i++) {
            paramTypes.put(i, i32Type);
        }

        // 创建函数类型
        LLVMTypeRef funcType = LLVMFunctionType(returnType, paramTypes, paramCount, 0);

        // 添加函数到模块
        LLVMValueRef function = LLVMAddFunction(module, funcName, funcType);
        symbolTable.putFunction(funcName, function);
        currentFunction = function;

        // 创建入口基本块
        LLVMBasicBlockRef entryBlock = LLVMAppendBasicBlock(function, funcName + "Entry");
        LLVMPositionBuilderAtEnd(builder, entryBlock);

        // 进入新作用域
        symbolTable.enterScope();

        // 处理参数
        if (ctx.funcFParams() != null) {
            for (int i = 0; i < paramCount; i++) {
                String paramName = ctx.funcFParams().funcFParam(i).IDENT().getText();
                LLVMValueRef paramAlloca = LLVMBuildAlloca(builder, i32Type, paramName);
                LLVMValueRef param = LLVMGetParam(function, i);
                LLVMBuildStore(builder, param, paramAlloca);
                symbolTable.put(paramName, paramAlloca);
            }
        }

        // 访问函数体
        visit(ctx.block());

        // 确保void函数有return语句
        if (isVoid && !isPreviousInstructionBranch(LLVMGetInsertBlock(builder))) {
            LLVMBuildRetVoid(builder);
        }

        // 退出作用域
        symbolTable.exitScope();
        currentFunction = null;

        return function;
    }

    @Override
    public LLVMValueRef visitBlock(SysYParser.BlockContext ctx) {
        // 如果不是函数的直接块，创建新作用域
        if (!(ctx.getParent() instanceof SysYParser.FuncDefContext)) {
            symbolTable.enterScope();
        }

        LLVMValueRef lastValue = null;
        // 访问块中的所有语句
        for (SysYParser.BlockItemContext item : ctx.blockItem()) {
            lastValue = visit(item);
            // 如果遇到终止指令（如return），则停止处理后续语句
            if (lastValue != null && LLVMGetInstructionOpcode(lastValue) == LLVMRet) {
                break;
            }
        }

        // 退出作用域
        if (!(ctx.getParent() instanceof SysYParser.FuncDefContext)) {
            symbolTable.exitScope();
        }

        return lastValue;
    }

    @Override
    public LLVMValueRef visitBlockItem(SysYParser.BlockItemContext ctx) {
        return visit(ctx.getChild(0));
    }

    @Override
    public LLVMValueRef visitStmt(SysYParser.StmtContext ctx) {
        // 处理赋值语句
        if (ctx.lVal() != null && ctx.ASSIGN() != null) {
            String varName = ctx.lVal().IDENT().getText();
            LLVMValueRef varPtr = symbolTable.get(varName);
            LLVMValueRef value = visit(ctx.exp());
            return LLVMBuildStore(builder, value, varPtr);
        }

        // 处理表达式语句
        if (ctx.exp() != null && ctx.SEMICOLON() != null && ctx.ASSIGN() == null) {
            return visit(ctx.exp());
        }

        // 处理块
        if (ctx.block() != null) {
            return visit(ctx.block());
        }

        // 处理if语句
        if (ctx.IF() != null) {
            LLVMValueRef condValue = visit(ctx.cond());
            LLVMValueRef condition = LLVMBuildICmp(builder, LLVMIntNE, condValue, zero, "ifcond");

            LLVMBasicBlockRef thenBlock = LLVMAppendBasicBlock(currentFunction, "then");
            LLVMBasicBlockRef elseBlock = ctx.ELSE() != null ?
                    LLVMAppendBasicBlock(currentFunction, "else") : null;
            LLVMBasicBlockRef mergeBlock = LLVMAppendBasicBlock(currentFunction, "ifcont");

            LLVMBuildCondBr(builder, condition, thenBlock, ctx.ELSE() != null ? elseBlock : mergeBlock);

            // 处理then块
            LLVMPositionBuilderAtEnd(builder, thenBlock);
            visit(ctx.stmt(0));
            if (!isPreviousInstructionBranch(LLVMGetInsertBlock(builder))) {
                LLVMBuildBr(builder, mergeBlock);
            }

            // 处理else块
            if (ctx.ELSE() != null) {
                LLVMPositionBuilderAtEnd(builder, elseBlock);
                visit(ctx.stmt(1));
                if (!isPreviousInstructionBranch(LLVMGetInsertBlock(builder))) {
                    LLVMBuildBr(builder, mergeBlock);
                }
            }

            // 继续在合并块后面生成代码
            LLVMPositionBuilderAtEnd(builder, mergeBlock);
            return null;
        }

        // 处理while语句
        if (ctx.WHILE() != null) {
            LLVMBasicBlockRef condBlock = LLVMAppendBasicBlock(currentFunction, "whilecond");
            LLVMBasicBlockRef bodyBlock = LLVMAppendBasicBlock(currentFunction, "whilebody");
            LLVMBasicBlockRef endBlock = LLVMAppendBasicBlock(currentFunction, "whileend");

            // 跳转到条件块
            LLVMBuildBr(builder, condBlock);

            // 设置条件块
            LLVMPositionBuilderAtEnd(builder, condBlock);
            LLVMValueRef condValue = visit(ctx.cond());
            LLVMValueRef condition = LLVMBuildICmp(builder, LLVMIntNE, condValue, zero, "whilecond");
            LLVMBuildCondBr(builder, condition, bodyBlock, endBlock);

            // 设置循环体块
            LLVMPositionBuilderAtEnd(builder, bodyBlock);

            // 记录break和continue的目标
            breakBlocks.push(endBlock);
            continueBlocks.push(condBlock);

            visit(ctx.stmt(0));

            // 恢复break和continue的目标
            breakBlocks.pop();
            continueBlocks.pop();

            // 循环结束后返回条件块
            if (!isPreviousInstructionBranch(LLVMGetInsertBlock(builder))) {
                LLVMBuildBr(builder, condBlock);
            }

            // 继续在循环结束块后面生成代码
            LLVMPositionBuilderAtEnd(builder, endBlock);
            return null;
        }

        // 处理break语句
        if (ctx.BREAK() != null) {
            if (breakBlocks.isEmpty()) {
                throw new RuntimeException("break statement outside of loop");
            }
            return LLVMBuildBr(builder, breakBlocks.peek());
        }

        // 处理continue语句
        if (ctx.CONTINUE() != null) {
            if (continueBlocks.isEmpty()) {
                throw new RuntimeException("continue statement outside of loop");
            }
            return LLVMBuildBr(builder, continueBlocks.peek());
        }

        // 处理return语句
        if (ctx.RETURN() != null) {
            if (ctx.exp() != null) {
                // 有返回值的情况
                LLVMValueRef returnValue = visit(ctx.exp());
                return LLVMBuildRet(builder, returnValue);
            } else {
                // 无返回值的情况（void函数）
                return LLVMBuildRetVoid(builder);
            }
        }

        return null;
    }

    @Override
    public LLVMValueRef visitExp(SysYParser.ExpContext ctx) {
        return visit(ctx.addExp());
    }

    @Override
    public LLVMValueRef visitAddExp(SysYParser.AddExpContext ctx) {
        // 处理加减表达式
        if (ctx.addExp() == null) {
            return visit(ctx.mulExp());
        }

        LLVMValueRef left = visit(ctx.addExp());
        LLVMValueRef right = visit(ctx.mulExp());

        if (ctx.PLUS() != null) {
            return LLVMBuildAdd(builder, left, right, "addtmp");
        } else {
            return LLVMBuildSub(builder, left, right, "subtmp");
        }
    }

    @Override
    public LLVMValueRef visitMulExp(SysYParser.MulExpContext ctx) {
        // 处理乘除模表达式
        if (ctx.mulExp() == null) {
            return visit(ctx.unaryExp());
        }

        LLVMValueRef left = visit(ctx.mulExp());
        LLVMValueRef right = visit(ctx.unaryExp());

        if (ctx.MUL() != null) {
            return LLVMBuildMul(builder, left, right, "multmp");
        } else if (ctx.DIV() != null) {
            return LLVMBuildSDiv(builder, left, right, "divtmp");
        } else {
            return LLVMBuildSRem(builder, left, right, "modtmp");
        }
    }

    @Override
    public LLVMValueRef visitUnaryExp(SysYParser.UnaryExpContext ctx) {
        // 处理一元表达式
        if (ctx.primaryExp() != null) {
            return visit(ctx.primaryExp());
        } else if (ctx.IDENT() != null) {
            // 处理函数调用
            String funcName = ctx.IDENT().getText();
            LLVMValueRef function = symbolTable.get(funcName);
            if (function == null) {
                throw new RuntimeException("Unknown function: " + funcName);
            }

            // 处理参数
            int paramCount = ctx.funcRParams() != null ? ctx.funcRParams().param().size() : 0;
            PointerPointer<Pointer> params = new PointerPointer<>(paramCount);
            if (ctx.funcRParams() != null) {
                for (int i = 0; i < paramCount; i++) {
                    params.put(i, visit(ctx.funcRParams().param(i)));
                }
            }

            return LLVMBuildCall(builder, function, params, paramCount, "calltmp");
        } else {
            // 处理一元运算符
            LLVMValueRef operand = visit(ctx.unaryExp());

            if (ctx.PLUS() != null) {
                return operand;  // +x 等同于 x
            } else if (ctx.MINUS() != null) {
                return LLVMBuildNeg(builder, operand, "negtmp");
            } else {  // NOT
                // 将非零值转为0，0转为1
                LLVMValueRef isZero = LLVMBuildICmp(builder, LLVMIntEQ, operand, zero, "iszero");
                return LLVMBuildZExt(builder, isZero, i32Type, "nottmp");
            }
        }
    }

    @Override
    public LLVMValueRef visitPrimaryExp(SysYParser.PrimaryExpContext ctx) {
        if (ctx.exp() != null) {
            return visit(ctx.exp());
        } else if (ctx.lVal() != null) {
            return visitLVal(ctx.lVal());
        } else {
            return visit(ctx.number());
        }
    }

    @Override
    public LLVMValueRef visitLVal(SysYParser.LValContext ctx) {
        String name = ctx.IDENT().getText();
        LLVMValueRef var = symbolTable.get(name);
        if (var == null) {
            throw new RuntimeException("Undefined variable: " + name);
        }

        // 索引数组变量暂不处理
        return LLVMBuildLoad(builder, var, name + "1");
    }

    @Override
    public LLVMValueRef visitNumber(SysYParser.NumberContext ctx) {
        int value = Integer.parseInt(ctx.INTEGER_CONST().getText());
        return LLVMConstInt(i32Type, value, 0);
    }

    @Override
    public LLVMValueRef visitCond(SysYParser.CondContext ctx) {
        return visit(ctx.lOrExp());
    }

    @Override
    public LLVMValueRef visitLOrExp(SysYParser.LOrExpContext ctx) {
        // 简单处理逻辑或表达式
        if (ctx.lOrExp() == null) {
            return visit(ctx.lAndExp());
        }

        LLVMValueRef left = visit(ctx.lOrExp());

        // 短路求值
        LLVMBasicBlockRef origBlock = LLVMGetInsertBlock(builder);
        LLVMBasicBlockRef rightBlock = LLVMAppendBasicBlock(currentFunction, "or.right");
        LLVMBasicBlockRef mergeBlock = LLVMAppendBasicBlock(currentFunction, "or.merge");

        // 分配结果变量
        LLVMValueRef resultPtr = LLVMBuildAlloca(builder, i32Type, "or.result");

        // 检查左侧是否为真
        LLVMValueRef leftCond = LLVMBuildICmp(builder, LLVMIntNE, left, zero, "or.left.cond");

        // 如果左侧为真，设置结果为1并跳至合并块
        LLVMBuildStore(builder, LLVMConstInt(i32Type, 1, 0), resultPtr);
        LLVMBuildCondBr(builder, leftCond, mergeBlock, rightBlock);

        // 处理右侧
        LLVMPositionBuilderAtEnd(builder, rightBlock);
        LLVMValueRef right = visit(ctx.lAndExp());
        LLVMValueRef rightCond = LLVMBuildICmp(builder, LLVMIntNE, right, zero, "or.right.cond");
        LLVMValueRef rightResult = LLVMBuildZExt(builder, rightCond, i32Type, "or.right.result");
        LLVMBuildStore(builder, rightResult, resultPtr);
        LLVMBuildBr(builder, mergeBlock);

        // 处理合并块
        LLVMPositionBuilderAtEnd(builder, mergeBlock);
        return LLVMBuildLoad(builder, resultPtr, "or.load");
    }

    @Override
    public LLVMValueRef visitLAndExp(SysYParser.LAndExpContext ctx) {
        // 简单处理逻辑与表达式
        if (ctx.lAndExp() == null) {
            return visit(ctx.eqExp());
        }

        LLVMValueRef left = visit(ctx.lAndExp());

        // 短路求值
        LLVMBasicBlockRef origBlock = LLVMGetInsertBlock(builder);
        LLVMBasicBlockRef rightBlock = LLVMAppendBasicBlock(currentFunction, "and.right");
        LLVMBasicBlockRef mergeBlock = LLVMAppendBasicBlock(currentFunction, "and.merge");

        // 分配结果变量
        LLVMValueRef resultPtr = LLVMBuildAlloca(builder, i32Type, "and.result");

        // 检查左侧是否为假
        LLVMValueRef leftCond = LLVMBuildICmp(builder, LLVMIntEQ, left, zero, "and.left.cond");

        // 如果左侧为假，设置结果为0并跳至合并块
        LLVMBuildStore(builder, zero, resultPtr);
        LLVMBuildCondBr(builder, leftCond, mergeBlock, rightBlock);

        // 处理右侧
        LLVMPositionBuilderAtEnd(builder, rightBlock);
        LLVMValueRef right = visit(ctx.eqExp());
        LLVMValueRef rightCond = LLVMBuildICmp(builder, LLVMIntNE, right, zero, "and.right.cond");
        LLVMValueRef rightResult = LLVMBuildZExt(builder, rightCond, i32Type, "and.right.result");
        LLVMBuildStore(builder, rightResult, resultPtr);
        LLVMBuildBr(builder, mergeBlock);

        // 处理合并块
        LLVMPositionBuilderAtEnd(builder, mergeBlock);
        return LLVMBuildLoad(builder, resultPtr, "and.load");
    }

    @Override
    public LLVMValueRef visitEqExp(SysYParser.EqExpContext ctx) {
        // 处理相等性表达式
        if (ctx.eqExp() == null) {
            return visit(ctx.relExp());
        }

        LLVMValueRef left = visit(ctx.eqExp());
        LLVMValueRef right = visit(ctx.relExp());

        LLVMValueRef cmp;
        if (ctx.EQ() != null) {
            cmp = LLVMBuildICmp(builder, LLVMIntEQ, left, right, "eq");
        } else {
            cmp = LLVMBuildICmp(builder, LLVMIntNE, left, right, "ne");
        }

        return LLVMBuildZExt(builder, cmp, i32Type, "eqtmp");
    }

    @Override
    public LLVMValueRef visitRelExp(SysYParser.RelExpContext ctx) {
        // 处理关系表达式
        if (ctx.relExp() == null) {
            return visit(ctx.addExp());
        }

        LLVMValueRef left = visit(ctx.relExp());
        LLVMValueRef right = visit(ctx.addExp());

        LLVMValueRef cmp;
        if (ctx.LT() != null) {
            cmp = LLVMBuildICmp(builder, LLVMIntSLT, left, right, "lt");
        } else if (ctx.GT() != null) {
            cmp = LLVMBuildICmp(builder, LLVMIntSGT, left, right, "gt");
        } else if (ctx.LE() != null) {
            cmp = LLVMBuildICmp(builder, LLVMIntSLE, left, right, "le");
        } else {
            cmp = LLVMBuildICmp(builder, LLVMIntSGE, left, right, "ge");
        }

        return LLVMBuildZExt(builder, cmp, i32Type, "reltmp");
    }
}