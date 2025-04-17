import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.llvm.LLVM.*;

import java.io.IOException;

import static org.bytedeco.llvm.global.LLVM.*;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("使用方式: java Main 输入文件 输出文件");
            return;
        }
        String inputFile = args[0];
        String outputFile = args[1];

        // 初始化LLVM
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();

        // 创建module
        LLVMModuleRef module = LLVMModuleCreateWithName("module");
        LLVMBuilderRef builder = LLVMCreateBuilder();

        // 解析源代码
        CharStream input = CharStreams.fromFileName(inputFile);
        SysYLexer lexer = new SysYLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SysYParser parser = new SysYParser(tokens);
        SysYParser.ProgramContext tree = parser.program();

        // 生成IR
        IRGenerator generator = new IRGenerator(module, builder);
        generator.visit(tree);

        // 输出IR到文件
        BytePointer error = new BytePointer();
        if (LLVMPrintModuleToFile(module, outputFile, error) != 0) {
            System.err.println("错误: " + error.getString());
        }

        // 释放资源
        LLVMDisposeBuilder(builder);
        LLVMDisposeModule(module);
    }
}