import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
            return;
        }

        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);

        // 使用自定义错误监听器
        SysYLexerErrorListener errorListener = new SysYLexerErrorListener();
        sysYLexer.removeErrorListeners();
        sysYLexer.addErrorListener(errorListener);

        // 获取所有token并输出
        Token token;

        while ((token = sysYLexer.nextToken()) != null && token.getType() != Token.EOF) {
            // 跳过注释，这些被ANTLR标记为skip
            if (token.getChannel() != Token.HIDDEN_CHANNEL) {
                // 处理整数常量 - 将十六进制和八进制转换为十进制
                if (token.getType() == SysYLexer.INTEGER_CONST) {
                    String text = token.getText();
                    int value;

                    // 处理十六进制
                    if (text.startsWith("0x") || text.startsWith("0X")) {
                        value = Integer.parseInt(text.substring(2), 16);
                        System.err.println(
                                SysYLexer.VOCABULARY.getSymbolicName(token.getType()) +
                                        " " + value + " at Line " + token.getLine() + ".");
                    }
                    // 处理八进制
                    else if (text.startsWith("0") && text.length() > 1 && !text.startsWith("0x") && !text.startsWith("0X")) {
                        value = Integer.parseInt(text.substring(1), 8);
                        System.err.println(
                                SysYLexer.VOCABULARY.getSymbolicName(token.getType()) +
                                        " " + value + " at Line " + token.getLine() + ".");
                    }
                    // 处理十进制
                    else {
                        System.err.println(
                                SysYLexer.VOCABULARY.getSymbolicName(token.getType()) +
                                        " " + text + " at Line " + token.getLine() + ".");
                    }
                } else {
                    // 输出其他token
                    System.err.println(
                            SysYLexer.VOCABULARY.getSymbolicName(token.getType()) +
                                    " " + token.getText() + " at Line " + token.getLine() + ".");
                }
            }
        }
    }
}