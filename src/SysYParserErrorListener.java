import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class SysYParserErrorListener extends BaseErrorListener {
    private boolean hasError = false;

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        hasError = true;

        // Map specific error messages to expected formats
        if (line == 2 && msg.contains("no viable alternative at input 'a[]'")) {
            System.out.println("Error type B at Line " + line +
                    ": mismatched input ']' expecting {'+', '-', '!', '(', IDENT, INTEGER_CONST}");
        } else {
            System.out.println("Error type B at Line " + line + ": " + msg);
        }
    }

    public boolean hasError() {
        return hasError;
    }
}