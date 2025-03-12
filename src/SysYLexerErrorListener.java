import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class SysYLexerErrorListener extends BaseErrorListener {
    private boolean hasError = false;

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        hasError = true;

        // Extract the problematic character from the error message
        String errorChar = "";
        if (msg.contains("'")) {
            int start = msg.indexOf("'");
            int end = msg.lastIndexOf("'");
            if (start != -1 && end != -1 && start != end) {
                errorChar = msg.substring(start + 1, end);
            }
        }

        // Format the error message
        System.out.println("Error type A at Line " + line + ": Mysterious character \"" + errorChar + "\"");
    }

    public boolean hasError() {
        return hasError;
    }
}