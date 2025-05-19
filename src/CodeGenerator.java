// 代码生成器 - 生成RISC-V汇编代码
public class CodeGenerator {
    private final StringBuilder assemblyCode = new StringBuilder();

    // 在CodeGenerator类中添加安全检查方法
    private String safeReg(String reg) {
        return (reg == null || reg.equals("null")) ? "t0" : reg;
    }

    // 生成标签
    public void emitLabel(String label) {
        assemblyCode.append(label).append(":\n");
    }

    // 生成注释
    public void emitComment(String comment) {
        assemblyCode.append("# ").append(comment).append("\n");
    }

    // 生成无操作数指令
    public void emit(String instruction) {
        assemblyCode.append("\t").append(instruction).append("\n");
    }

    // 生成有1个操作数的指令
    public void emit1(String instruction, String op) {
        assemblyCode.append("\t").append(instruction).append(" ").append(op).append("\n");
    }

    // 生成有2个操作数的指令
    public void emit2(String instruction, String op1, String op2) {
        assemblyCode.append("\t").append(instruction)
                .append(" ").append(safeReg(op1))
                .append(", ").append(safeReg(op2))
                .append("\n");
    }

    // 生成有3个操作数的指令
    public void emit3(String instruction, String op1, String op2, String op3) {
        assemblyCode.append("\t").append(instruction)
                .append(" ").append(safeReg(op1))
                .append(", ").append(safeReg(op2))
                .append(", ").append(safeReg(op3))
                .append("\n");
    }

    // 生成内存加载指令
    public void emitLoad(String reg, int offset) {
        emit2("lw", reg, offset + "(sp)");
    }

    // 生成内存存储指令
    public void emitStore(String reg, int offset) {
        emit2("sw", reg, offset + "(sp)");
    }

    // 生成数据段
    public void emitDataSection() {
        assemblyCode.append(".data\n");
    }

    // 生成代码段
    public void emitTextSection() {
        assemblyCode.append(".text\n");
    }

    // 生成全局变量声明
    public void emitGlobal(String name) {
        emit1(".globl", name);
    }

    // 生成数据定义
    public void emitWord(String value) {
        emit1(".word", value);
    }

    // 生成新行
    public void emitNewLine() {
        assemblyCode.append("\n");
    }

    // 获取生成的汇编代码
    public String getAssemblyCode() {
        return assemblyCode.toString();
    }
}