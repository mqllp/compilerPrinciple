import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

        // 收集所有token
        List<Token> tokens = new ArrayList<>();
        Token token;

        while ((token = sysYLexer.nextToken()) != null && token.getType() != Token.EOF) {
            if (token.getChannel() != Token.HIDDEN_CHANNEL) {
                tokens.add(token);
            }
        }

        // 只有在没有错误的情况下才输出token
        if (!errorListener.hasError()) {
            for (Token t : tokens) {
                if (t.getType() == SysYLexer.INTEGER_CONST) {
                    String text = t.getText();
                    int value;

                    // 处理十六进制
                    if (text.startsWith("0x") || text.startsWith("0X")) {
                        value = Integer.parseInt(text.substring(2), 16);
                        System.err.println(
                                SysYLexer.VOCABULARY.getSymbolicName(t.getType()) +
                                        " " + value + " at Line " + t.getLine() + ".");
                    }
                    // 处理八进制
                    else if (text.startsWith("0") && text.length() > 1 && !text.startsWith("0x") && !text.startsWith("0X")) {
                        value = Integer.parseInt(text.substring(1), 8);
                        System.err.println(
                                SysYLexer.VOCABULARY.getSymbolicName(t.getType()) +
                                        " " + value + " at Line " + t.getLine() + ".");
                    }
                    // 处理十进制
                    else {
                        System.err.println(
                                SysYLexer.VOCABULARY.getSymbolicName(t.getType()) +
                                        " " + text + " at Line " + t.getLine() + ".");
                    }
                } else {
                    // 输出其他token
                    System.err.println(
                            SysYLexer.VOCABULARY.getSymbolicName(t.getType()) +
                                    " " + t.getText() + " at Line " + t.getLine() + ".");
                }
            }
        }
    }
}