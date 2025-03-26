import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
            return;
        }

        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);

        // Lexical analysis
        SysYLexer sysYLexer = new SysYLexer(input);
        SysYLexerErrorListener lexerErrorListener = new SysYLexerErrorListener();
        sysYLexer.removeErrorListeners();
        sysYLexer.addErrorListener(lexerErrorListener);

        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);

        // Don't proceed if there are lexical errors
        if (lexerErrorListener.hasError()) {
            return;
        }

        // Syntax analysis
        SysYParser sysYParser = new SysYParser(tokens);
        SysYParserErrorListener parserErrorListener = new SysYParserErrorListener();
        sysYParser.removeErrorListeners();
        sysYParser.addErrorListener(parserErrorListener);

        ParseTree tree = sysYParser.program();

        // Don't proceed if there are syntax errors
        if (parserErrorListener.hasError()) {
            return;
        }

        // Semantic analysis
        TypeChecker typeChecker = new TypeChecker();
        typeChecker.check((SysYParser.CompUnitContext) tree.getChild(0));

        // Only proceed with formatting if no semantic errors
        if (!typeChecker.hasErrors()) {
            // Code formatting - output to stdout
            SysYFormattingVisitor formatter = new SysYFormattingVisitor();
            String formattedCode = formatter.visit(tree);
            System.out.println(formattedCode);
        }
    }
}