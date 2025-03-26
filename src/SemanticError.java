public class SemanticError {
    public static final int UNDEFINED_VARIABLE = 1;
    public static final int UNDEFINED_FUNCTION = 2;
    public static final int REDEFINED_VARIABLE = 3;
    public static final int REDEFINED_FUNCTION = 4;
    public static final int TYPE_MISMATCH_ASSIGNMENT = 5;
    public static final int TYPE_MISMATCH_OPERANDS = 6;
    public static final int TYPE_MISMATCH_RETURN = 7;
    public static final int FUNCTION_PARAM_MISMATCH = 8;
    public static final int NOT_AN_ARRAY = 9;
    public static final int NOT_A_FUNCTION = 10;
    public static final int ASSIGNMENT_TO_NON_VARIABLE = 11;
    public static final int OTHER_ERROR = 12;

    private int errorType;
    private int lineNo;
    private String message;

    public SemanticError(int errorType, int lineNo, String message) {
        this.errorType = errorType;
        this.lineNo = lineNo;
        this.message = message;
    }

    @Override
    public String toString() {
        return "Error type " + errorType + " at Line " + lineNo + ": " + message;
    }

    public int getErrorType() {
        return errorType;
    }

    public int getLineNo() {
        return lineNo;
    }
}