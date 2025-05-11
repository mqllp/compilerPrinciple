import org.bytedeco.llvm.LLVM.*;
import org.bytedeco.javacpp.BytePointer;

import java.io.*;
import java.util.*;
import static org.bytedeco.llvm.global.LLVM.*;

public class RISCVGenerator {
    private LLVMModuleRef module;
    private String output;
    private StringBuilder assemblyCode;
    private Map<String, Integer> variableOffsets; // 记录局部变量在栈中的偏移量
    private int currentStackOffset;
    private Map<LLVMValueRef, Integer> valueToRegister; // 值到寄存器的映射
    private int nextRegId = 0;

    public RISCVGenerator(LLVMModuleRef module, String output) {
        this.module = module;
        this.output = output;
        this.assemblyCode = new StringBuilder();
        this.variableOffsets = new HashMap<>();
        this.valueToRegister = new HashMap<>();
    }

    public void generate() {
        // 一次性生成代码段开始标记
        assemblyCode.append("  .text\n");

        // 是否有全局变量需要处理
        boolean hasGlobals = LLVMGetFirstGlobal(module) != null;

        if (hasGlobals) {
            assemblyCode.append("  .data\n");
            generateGlobalVariables();
        }

        // 处理所有函数
        generateFunctions();

        // 写入输出文件
        writeToFile();
    }

    private void generateGlobalVariables() {
//        assemblyCode.append("  .data\n");

        for (LLVMValueRef global = LLVMGetFirstGlobal(module);
             global != null;
             global = LLVMGetNextGlobal(global)) {

            String name = LLVMGetValueName(global).getString();
            assemblyCode.append("  .globl ").append(name).append("\n");
            assemblyCode.append(name).append(":\n");

            // 获取初始化值
            LLVMValueRef initVal = LLVMGetInitializer(global);
            if (initVal != null && LLVMIsConstant(initVal) == 1) {
                long value = LLVMConstIntGetSExtValue(initVal);
                assemblyCode.append("  .word ").append(value).append("\n");
            } else {
                assemblyCode.append("  .word 0\n"); // 默认值
            }
        }
    }

    private void generateFunctions() {
        for (LLVMValueRef func = LLVMGetFirstFunction(module);
             func != null;
             func = LLVMGetNextFunction(func)) {

            String name = LLVMGetValueName(func).getString();

            // 检查函数是否有定义体
            if (LLVMCountBasicBlocks(func) > 0) {
                generateFunction(func, name);
            }
        }
    }

    private void generateFunction(LLVMValueRef func, String name) {
        // 重置函数状态
        variableOffsets.clear();
        currentStackOffset = 0;
        valueToRegister.clear();
        nextRegId = 0;

        // 生成函数标签
        assemblyCode.append("  .text\n");
        assemblyCode.append("  .globl ").append(name).append("\n");
        assemblyCode.append(name).append(":\n");

        // 针对main函数简化栈帧处理
        if ("main".equals(name)) {
            assemblyCode.append("  addi sp, sp, 0\n");  // 简化的栈帧空间
        } else {
            // 非main函数的标准序言
            assemblyCode.append("  addi sp, sp, -32\n");
            assemblyCode.append("  sw ra, 28(sp)\n");
            assemblyCode.append("  sw s0, 24(sp)\n");
            assemblyCode.append("  addi s0, sp, 32\n");
        }

        // 处理函数的所有基本块
        for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(func);
             bb != null;
             bb = LLVMGetNextBasicBlock(bb)) {
            generateBasicBlock(bb);
        }
    }

    private void generateBasicBlock(LLVMBasicBlockRef bb) {
        // 获取基本块名称并生成标签
        String bbName = LLVMGetBasicBlockName(bb).getString();
        assemblyCode.append(bbName).append(":\n");

        // 处理基本块中的所有指令
        for (LLVMValueRef inst = LLVMGetFirstInstruction(bb);
             inst != null;
             inst = LLVMGetNextInstruction(inst)) {

            generateInstruction(inst);
        }
    }

    private void generateInstruction(LLVMValueRef inst) {
        // 获取指令类型
        int opcode = LLVMGetInstructionOpcode(inst);

        switch (opcode) {
            case LLVMRet:
                generateRetInstruction(inst);
                break;
            case LLVMAlloca:
                generateAllocaInstruction(inst);
                break;
            case LLVMLoad:
                generateLoadInstruction(inst);
                break;
            case LLVMStore:
                generateStoreInstruction(inst);
                break;
            case LLVMAdd:
                generateBinaryOpInstruction(inst, "add");
                break;
            case LLVMSub:
                generateBinaryOpInstruction(inst, "sub");
                break;
            case LLVMMul:
                generateBinaryOpInstruction(inst, "mul");
                break;
            case LLVMSDiv:
                generateBinaryOpInstruction(inst, "div");
                break;
            default:
                // 暂不支持的指令
                assemblyCode.append("  # 未实现指令: ").append(opcode).append("\n");
        }
    }

    // 具体指令生成方法...

    private void generateRetInstruction(LLVMValueRef inst) {
        // 获取函数名
        LLVMValueRef function = LLVMGetBasicBlockParent(LLVMGetInstructionParent(inst));
        String funcName = LLVMGetValueName(function).getString();

        // 获取返回值
        if (LLVMGetNumOperands(inst) > 0) {
            LLVMValueRef retValue = LLVMGetOperand(inst, 0);

            if (LLVMIsConstant(retValue) == 1) {
                // 常量返回
                long value = LLVMConstIntGetSExtValue(retValue);
                assemblyCode.append("  li a0, ").append(value).append("\n");
            } else {
                // 变量返回
                int reg = getRegister(retValue);
                assemblyCode.append("  mv a0, t").append(reg).append("\n");
            }
        }

        // 区分main函数和普通函数的返回处理
        if ("main".equals(funcName)) {
            // main函数使用系统调用退出
            assemblyCode.append("  addi sp, sp, 0\n");
            assemblyCode.append("  li a7, 93\n");  // exit系统调用号
            assemblyCode.append("  ecall\n");
        } else {
            // 普通函数使用标准返回序列
            assemblyCode.append("  lw ra, 28(sp)\n");
            assemblyCode.append("  lw s0, 24(sp)\n");
            assemblyCode.append("  addi sp, sp, 32\n");
            assemblyCode.append("  ret\n");
        }
    }

    private void generateAllocaInstruction(LLVMValueRef inst) {
        // 获取分配的变量名
        String name = LLVMGetValueName(inst).getString();

        // 为局部变量分配栈空间（4字节对齐）
        currentStackOffset -= 4;
        variableOffsets.put(name, currentStackOffset);

        // 在栈上分配空间，并初始化为0
        assemblyCode.append("  # 分配局部变量 ").append(name).append("\n");
        assemblyCode.append("  addi sp, sp, -4\n");
        assemblyCode.append("  sw zero, 0(sp)\n");
    }

    private void generateLoadInstruction(LLVMValueRef inst) {
        // 获取源操作数和目标寄存器
        LLVMValueRef source = LLVMGetOperand(inst, 0);
        String destName = LLVMGetValueName(inst).getString();

        // 获取或分配目标寄存器
        int destReg = getOrCreateRegister(inst);

        // 获取源地址名称
        String sourceName = LLVMGetValueName(source).getString();

        if (isGlobalVariable(source)) {
            // 从全局变量加载
            assemblyCode.append("  # 加载全局变量 ").append(sourceName).append("\n");
            assemblyCode.append("  la t0, ").append(sourceName).append("\n");
            assemblyCode.append("  lw t").append(destReg).append(", 0(t0)\n");
        } else {
            // 从局部变量(栈)加载
            Integer offset = variableOffsets.get(sourceName);
            if (offset != null) {
                assemblyCode.append("  # 加载局部变量 ").append(sourceName).append("\n");
                assemblyCode.append("  lw t").append(destReg).append(", ").append(offset).append("(s0)\n");
            } else {
                assemblyCode.append("  # 警告: 未找到变量 ").append(sourceName).append("\n");
            }
        }

        // 记录值到寄存器的映射
        valueToRegister.put(inst, destReg);
    }

    private void generateStoreInstruction(LLVMValueRef inst) {
        // 获取源值和目标地址
        LLVMValueRef value = LLVMGetOperand(inst, 0);
        LLVMValueRef pointer = LLVMGetOperand(inst, 1);

        // 获取目标变量名
        String destName = LLVMGetValueName(pointer).getString();

        // 处理源值
        int srcReg;
        if (LLVMIsConstant(value) == 1) {
            // 常量值处理
            long constValue = LLVMConstIntGetSExtValue(value);
            assemblyCode.append("  # 加载常量 ").append(constValue).append("\n");
            assemblyCode.append("  li t0, ").append(constValue).append("\n");
            srcReg = 0;
        } else {
            // 变量值处理
            srcReg = getOrCreateRegister(value);
        }

        if (isGlobalVariable(pointer)) {
            // 存储到全局变量
            assemblyCode.append("  # 存储到全局变量 ").append(destName).append("\n");
            assemblyCode.append("  la t1, ").append(destName).append("\n");
            if (LLVMIsConstant(value) == 1) {
                assemblyCode.append("  sw t0, 0(t1)\n");
            } else {
                assemblyCode.append("  sw t").append(srcReg).append(", 0(t1)\n");
            }
        } else {
            // 存储到局部变量(栈)
            Integer offset = variableOffsets.get(destName);
            if (offset != null) {
                assemblyCode.append("  # 存储到局部变量 ").append(destName).append("\n");
                if (LLVMIsConstant(value) == 1) {
                    assemblyCode.append("  sw t0, ").append(offset).append("(s0)\n");
                } else {
                    assemblyCode.append("  sw t").append(srcReg).append(", ").append(offset).append("(s0)\n");
                }
            } else {
                assemblyCode.append("  # 警告: 未找到变量 ").append(destName).append("\n");
            }
        }
    }

    private void generateBinaryOpInstruction(LLVMValueRef inst, String op) {
        // 获取操作数
        LLVMValueRef lhs = LLVMGetOperand(inst, 0);
        LLVMValueRef rhs = LLVMGetOperand(inst, 1);

        // 目标寄存器
        int destReg = getOrCreateRegister(inst);

        // 处理左操作数
        int leftReg = loadOperandToRegister(lhs, 0);

        // 处理右操作数
        int rightReg = loadOperandToRegister(rhs, 1);

        // 生成相应的RISC-V指令
        assemblyCode.append("  # ").append(op).append(" 操作\n");
        assemblyCode.append("  ").append(op).append(" t").append(destReg)
                .append(", t").append(leftReg).append(", t").append(rightReg).append("\n");

        // 记录结果寄存器
        valueToRegister.put(inst, destReg);
    }

    // 辅助方法：将操作数加载到寄存器
    private int loadOperandToRegister(LLVMValueRef operand, int tempIndex) {
        if (LLVMIsConstant(operand) == 1) {
            // 常量操作数
            long value = LLVMConstIntGetSExtValue(operand);
            assemblyCode.append("  li t").append(tempIndex + 2).append(", ").append(value).append("\n");
            return tempIndex + 2;
        } else {
            // 变量操作数
            Integer reg = valueToRegister.get(operand);
            if (reg != null) {
                return reg;
            } else {
                // 尝试加载变量
                String name = LLVMGetValueName(operand).getString();
                assemblyCode.append("  # 加载变量 ").append(name).append(" 到寄存器\n");

                if (isGlobalVariable(operand)) {
                    assemblyCode.append("  la t1, ").append(name).append("\n");
                    assemblyCode.append("  lw t").append(tempIndex + 2).append(", 0(t1)\n");
                } else {
                    Integer offset = variableOffsets.get(name);
                    if (offset != null) {
                        assemblyCode.append("  lw t").append(tempIndex + 2)
                                .append(", ").append(offset).append("(s0)\n");
                    } else {
                        assemblyCode.append("  # 警告: 未找到变量 ").append(name).append("\n");
                    }
                }
                return tempIndex + 2;
            }
        }
    }

    // 为值获取或创建一个寄存器
    private int getOrCreateRegister(LLVMValueRef value) {
        return valueToRegister.computeIfAbsent(value, k -> ++nextRegId);
    }

    // 检查值是否为全局变量
    private boolean isGlobalVariable(LLVMValueRef value) {
        if (value == null) return false;

        // 遍历模块的所有全局变量
        for (LLVMValueRef global = LLVMGetFirstGlobal(module);
             global != null;
             global = LLVMGetNextGlobal(global)) {

            if (global.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private int getRegister(LLVMValueRef value) {
        // 简单的寄存器分配
        return valueToRegister.computeIfAbsent(value, k -> ++nextRegId);
    }

    private void writeToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(output))) {
            writer.print(assemblyCode.toString());
        } catch (IOException e) {
            System.err.println("写入输出文件错误: " + e.getMessage());
        }
    }
}