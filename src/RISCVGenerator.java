import org.bytedeco.llvm.LLVM.*;
import org.bytedeco.javacpp.BytePointer;

import java.io.*;
import java.util.*;
import static org.bytedeco.llvm.global.LLVM.*;

public class RISCVGenerator {
    private LLVMModuleRef module;
    private String output;
    private StringBuilder assemblyCode;

    public RISCVGenerator(LLVMModuleRef module, String output) {
        this.module = module;
        this.output = output;
        this.assemblyCode = new StringBuilder();
    }

    public void generate() {
        // 生成汇编代码头部
        assemblyCode.append("  .text\n");

        // 获取LLVM IR文本形式
        BytePointer irString = LLVMPrintModuleToString(module);
        String ir = irString.getString();
        LLVMDisposeMessage(irString);

        // 解析IR并生成RISC-V汇编
        parseAndGenerateRISCV(ir);

        // 写入输出文件
        try (PrintWriter writer = new PrintWriter(new FileWriter(output))) {
            writer.print(assemblyCode.toString());
        } catch (IOException e) {
            System.err.println("写入输出文件时出错: " + e.getMessage());
        }
    }

    private void parseAndGenerateRISCV(String ir) {
        // 按行解析IR
        String[] lines = ir.split("\n");

        // 查找函数定义
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // 处理函数定义
            if (line.startsWith("define ") && line.contains("@main(")) {
                processMainFunction(lines, i);
            }
        }
    }

    private void processMainFunction(String[] lines, int startLine) {
        // 生成main函数标签
        assemblyCode.append("  .globl main\n");
        assemblyCode.append("main:\n");
        assemblyCode.append("  addi sp, sp, 0\n");
        assemblyCode.append("mainEntry:\n");

        // 查找返回值
        int returnValue = findMainReturnValue(lines, startLine);

        // 生成exit系统调用
        assemblyCode.append("  li a0, ").append(returnValue).append("\n");
        assemblyCode.append("  addi sp, sp, 0\n");
        assemblyCode.append("  li a7, 93\n");
        assemblyCode.append("  ecall\n");
    }

    private int findMainReturnValue(String[] lines, int startLine) {
        // 默认返回值
        int returnValue = 0;

        // 寻找ret指令
        for (int i = startLine; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("ret i32 ")) {
                // 解析返回值
                String valueStr = line.substring("ret i32 ".length()).trim();
                try {
                    returnValue = Integer.parseInt(valueStr);
                } catch (NumberFormatException e) {
                    // 如果不是简单的数字，可能是一个复杂表达式
                    // 这里简化处理，假设是-1
                    if (valueStr.equals("-1")) {
                        returnValue = -1;
                    }
                }
                break;
            }
        }

        return returnValue;
    }
}