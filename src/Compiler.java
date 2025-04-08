import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.llvm4j.llvm4j.Module;

import java.io.FileWriter;
import java.io.IOException;

public class Compiler {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java Compiler <input_file> <output_file>");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputFile = args[1];

        try {
            // 读取输入文件
            CharStream input = CharStreams.fromFileName(inputFile);

            // 词法分析
            SysYLexer lexer = new SysYLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // 语法分析
            SysYParser parser = new SysYParser(tokens);
            ParseTree tree = parser.program();

            // 生成LLVM IR
            MyVisitor visitor = new MyVisitor();
            visitor.visit(tree);

            // 获取生成的模块
            Module module = visitor.getModule();

            // 将LLVM IR写入到输出文件
            try (FileWriter writer = new FileWriter(outputFile)) {
                writer.write(module.toString());
            }

            System.out.println("IR generated successfully: " + outputFile);

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}