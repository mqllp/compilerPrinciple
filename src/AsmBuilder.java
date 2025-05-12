public class AsmBuilder {
    public AsmBuilder() {
        this.buffer = new StringBuilder();
    }

    private StringBuilder buffer;

    public StringBuilder getBuffer() {
        return this.buffer;
    }

    public void buildLabel(String labelText) {
        buffer.append(labelText).append(":\n");
    }

    public void op2(String op, String dest, String lhs, String rhs) {
        buffer.append(String.format("  %s %s, %s, %s\n", op, dest, lhs, rhs));
    }

    public void op1(String op, String dest, String lhs) {
        buffer.append(String.format("  %s %s, %s\n", op, dest, lhs));
    }

    public void op0(String op, String dest) {
        buffer.append(String.format("  %s %s\n", op, dest));
    }

    public void op(String op) {
        buffer.append(String.format("  %s\n", op));
    }

    public void newline() {
        buffer.append("\n");
    }
}