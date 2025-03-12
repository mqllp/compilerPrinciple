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

        // Format syntax error message and send to stdout (not stderr)
        System.out.println("Error type B at Line " + line + ": " + msg);
    }

    public boolean hasError() {
        return hasError;
    }
}