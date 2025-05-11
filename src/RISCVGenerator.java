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
    private RegisterAllocator registerAllocator; // 添加寄存器分配器
    private int nextRegId = 0;

    public RISCVGenerator(LLVMModuleRef module, String output) {
        this.module = module;
        this.output = output;
        this.assemblyCode = new StringBuilder();
        this.variableOffsets = new HashMap<>();
        this.valueToRegister = new HashMap<>();
        this.registerAllocator = new RegisterAllocator(); // 初始化寄存器分配器
    }

    public void generate() {
        // 构建活跃区间并执行寄存器分配
        registerAllocator.buildIntervals(module);
        registerAllocator.allocate();

        assemblyCode.append("  .text\n");

        // 处理全局变量
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

        // 处理返回值
        if (LLVMGetNumOperands(inst) > 0) {
            LLVMValueRef retValue = LLVMGetOperand(inst, 0);

            if (LLVMIsConstant(retValue) == 1) {
                // 常量返回值
                long value = LLVMConstIntGetSExtValue(retValue);
                assemblyCode.append("  li a0, ").append(value).append("\n");
            } else {
                // 变量返回值
                Location location = registerAllocator.getLocation(retValue);

                if (location != null) {
                    if (location.isRegister()) {
                        assemblyCode.append("  mv a0, x").append(location.getRegister()).append("\n");
                    } else if (location.isStack()) {
                        assemblyCode.append("  lw a0, ").append(location.getOffset()).append("(sp)\n");
                    } else if (location.isGlobal()) {
                        assemblyCode.append("  la t0, ").append(location.getName()).append("\n");
                        assemblyCode.append("  lw a0, 0(t0)\n");
                    }
                } else {
                    assemblyCode.append("  # 警告: 未找到返回值的位置\n");
                }
            }
        }

        // 处理函数返回
        if ("main".equals(funcName)) {
            // main函数使用系统调用退出
            assemblyCode.append("  addi sp, sp, ").append(registerAllocator.getStackSize()).append("\n");
            assemblyCode.append("  li a7, 93\n");
            assemblyCode.append("  ecall\n");
        } else {
            // 普通函数返回
            assemblyCode.append("  lw ra, 0(sp)\n");
            assemblyCode.append("  addi sp, sp, ").append(registerAllocator.getStackSize()).append("\n");
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
        // 获取源操作数
        LLVMValueRef source = LLVMGetOperand(inst, 0);

        // 获取目标位置和源位置
        Location destLocation = registerAllocator.getLocation(inst);
        Location sourceLocation = registerAllocator.getLocation(source);

        if (destLocation == null) {
            assemblyCode.append("  # 警告: 目标变量没有分配位置\n");
            return;
        }

        int destReg;
        if (destLocation.isRegister()) {
            destReg = destLocation.getRegister();
        } else {
            // 如果目标不是寄存器，使用临时寄存器
            destReg = 5; // t0
        }

        // 根据源的位置加载数据
        if (isGlobalVariable(source)) {
            // 从全局变量加载
            String sourceName = LLVMGetValueName(source).getString();
            assemblyCode.append("  la t3, ").append(sourceName).append("\n");
            assemblyCode.append("  lw x").append(destReg).append(", 0(t3)\n");
        } else if (sourceLocation != null) {
            if (sourceLocation.isRegister()) {
                // 从寄存器加载
                assemblyCode.append("  mv x").append(destReg)
                        .append(", x").append(sourceLocation.getRegister()).append("\n");
            } else if (sourceLocation.isStack()) {
                // 从栈加载
                assemblyCode.append("  lw x").append(destReg).append(", ")
                        .append(sourceLocation.getOffset()).append("(sp)\n");
            } else if (sourceLocation.isGlobal()) {
                // 从全局变量加载
                assemblyCode.append("  la t3, ").append(sourceLocation.getName()).append("\n");
                assemblyCode.append("  lw x").append(destReg).append(", 0(t3)\n");
            }
        } else {
            String sourceName = LLVMGetValueName(source).getString();
            assemblyCode.append("  # 警告: 未找到变量 ").append(sourceName).append(" 的位置\n");
        }

        // 如果目标位置在栈上，需要将结果存回栈
        if (destLocation.isStack()) {
            assemblyCode.append("  sw x").append(destReg).append(", ")
                    .append(destLocation.getOffset()).append("(sp)\n");
        }
    }

    private void generateStoreInstruction(LLVMValueRef inst) {
        // 获取源值和目标地址
        LLVMValueRef value = LLVMGetOperand(inst, 0);
        LLVMValueRef pointer = LLVMGetOperand(inst, 1);

        // 获取目标位置
        Location pointerLocation = registerAllocator.getLocation(pointer);

        // 处理源值
        if (LLVMIsConstant(value) == 1) {
            // 常量值
            long constValue = LLVMConstIntGetSExtValue(value);
            assemblyCode.append("  # 存储常量 ").append(constValue).append("\n");
            assemblyCode.append("  li t0, ").append(constValue).append("\n");

            if (isGlobalVariable(pointer)) {
                // 存储到全局变量
                String destName = LLVMGetValueName(pointer).getString();
                assemblyCode.append("  la t1, ").append(destName).append("\n");
                assemblyCode.append("  sw t0, 0(t1)\n");
            } else if (pointerLocation != null) {
                if (pointerLocation.isRegister()) {
                    // 存储到寄存器指向的内存
                    assemblyCode.append("  sw t0, 0(x").append(pointerLocation.getRegister()).append(")\n");
                } else if (pointerLocation.isStack()) {
                    // 存储到栈位置
                    assemblyCode.append("  sw t0, ").append(pointerLocation.getOffset()).append("(sp)\n");
                }
            }
        } else {
            // 变量值
            Location valueLocation = registerAllocator.getLocation(value);

            if (valueLocation != null) {
                int srcReg;

                if (valueLocation.isRegister()) {
                    // 源值在寄存器中
                    srcReg = valueLocation.getRegister();
                } else {
                    // 源值在栈或全局变量中，需要先加载到临时寄存器
                    srcReg = 5; // 使用t0临时寄存器

                    if (valueLocation.isStack()) {
                        assemblyCode.append("  lw x").append(srcReg).append(", ")
                                .append(valueLocation.getOffset()).append("(sp)\n");
                    } else if (valueLocation.isGlobal()) {
                        assemblyCode.append("  la t1, ").append(valueLocation.getName()).append("\n");
                        assemblyCode.append("  lw x").append(srcReg).append(", 0(t1)\n");
                    }
                }

                // 执行存储
                if (isGlobalVariable(pointer)) {
                    String destName = LLVMGetValueName(pointer).getString();
                    assemblyCode.append("  la t1, ").append(destName).append("\n");
                    assemblyCode.append("  sw x").append(srcReg).append(", 0(t1)\n");
                } else if (pointerLocation != null) {
                    if (pointerLocation.isRegister()) {
                        assemblyCode.append("  sw x").append(srcReg).append(", 0(x")
                                .append(pointerLocation.getRegister()).append(")\n");
                    } else if (pointerLocation.isStack()) {
                        assemblyCode.append("  sw x").append(srcReg).append(", ")
                                .append(pointerLocation.getOffset()).append("(sp)\n");
                    }
                }
            }
        }
    }

    private void generateBinaryOpInstruction(LLVMValueRef inst, String op) {
        // 获取操作数
        LLVMValueRef lhs = LLVMGetOperand(inst, 0);
        LLVMValueRef rhs = LLVMGetOperand(inst, 1);

        // 获取结果位置
        Location resultLocation = registerAllocator.getLocation(inst);
        if (resultLocation == null) {
            assemblyCode.append("  # 警告: 结果没有分配位置\n");
            return;
        }

        int destReg;
        if (resultLocation.isRegister()) {
            destReg = resultLocation.getRegister();
        } else {
            // 如果结果不是寄存器，使用临时寄存器
            destReg = 5; // t0
        }

        // 加载左操作数到寄存器
        int leftReg = loadOperandWithAllocator(lhs, 6); // t1

        // 加载右操作数到寄存器
        int rightReg = loadOperandWithAllocator(rhs, 7); // t2

        // 生成运算指令
        assemblyCode.append("  ").append(op).append(" x").append(destReg)
                .append(", x").append(leftReg).append(", x").append(rightReg).append("\n");

        // 如果结果位置在栈上，则需要存储结果
        if (!resultLocation.isRegister()) {
            if (resultLocation.isStack()) {
                assemblyCode.append("  sw x").append(destReg).append(", ")
                        .append(resultLocation.getOffset()).append("(sp)\n");
            }
        }
    }

    // 辅助方法：使用RegisterAllocator加载操作数到寄存器
    private int loadOperandWithAllocator(LLVMValueRef operand, int tempReg) {
        if (LLVMIsConstant(operand) == 1) {
            // 常量操作数
            long value = LLVMConstIntGetSExtValue(operand);
            assemblyCode.append("  li x").append(tempReg).append(", ").append(value).append("\n");
            return tempReg;
        } else {
            // 变量操作数
            Location location = registerAllocator.getLocation(operand);

            if (location != null) {
                if (location.isRegister()) {
                    // 直接使用已分配的寄存器
                    return location.getRegister();
                } else if (location.isStack()) {
                    // 从栈加载到临时寄存器
                    assemblyCode.append("  lw x").append(tempReg).append(", ")
                            .append(location.getOffset()).append("(sp)\n");
                    return tempReg;
                } else if (location.isGlobal()) {
                    // 从全局变量加载到临时寄存器
                    assemblyCode.append("  la t3, ").append(location.getName()).append("\n");
                    assemblyCode.append("  lw x").append(tempReg).append(", 0(t3)\n");
                    return tempReg;
                }
            }

            // 如果没有找到位置，给出警告
            String name = LLVMGetValueName(operand).getString();
            assemblyCode.append("  # 警告: 未找到变量 ").append(name).append(" 的位置\n");
            return tempReg;
        }
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