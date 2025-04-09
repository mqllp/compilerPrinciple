import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.io.*;

public class Main {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java Main <source-file> <output-file>");
            System.exit(1);
        }

        try {
            // 读取输入文件
            String inputFile = args[0];
            String outputFile = args[1];

            // 创建词法分析器
            CharStream input = CharStreams.fromFileName(inputFile);
            SysYLexer lexer = new SysYLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // 创建语法分析器
            SysYParser parser = new SysYParser(tokens);
            parser.setErrorHandler(new BailErrorStrategy());  // 使用严格的错误处理
            ParseTree tree = parser.program();

            // 创建访问器并生成IR
            IRVisitor visitor = new IRVisitor(new File(inputFile).getName());
            visitor.visit(tree);

            // 输出IR到文件
            visitor.getModule().dump(outputFile);

            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}