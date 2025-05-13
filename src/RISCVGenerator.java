import org.bytedeco.llvm.LLVM.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import static org.bytedeco.llvm.global.LLVM.*;

public class RISCVGenerator {
    // 核心组件
    private final ModuleProcessor moduleProcessor;
    private final CodeGenerator codeGenerator;
    private final RegisterManager registerManager;
    private final MemoryManager memoryManager;
    private final LifetimeAnalyzer lifetimeAnalyzer;

    // 模块与输出信息
    private final LLVMModuleRef module;
    private final String targetFilePath;

    public RISCVGenerator(LLVMModuleRef module, String outputPath) {
        this.module = module;
        this.targetFilePath = outputPath;

        // 初始化组件
        this.codeGenerator = new CodeGenerator();
        this.lifetimeAnalyzer = new LifetimeAnalyzer();
        this.memoryManager = new MemoryManager(16000); // MAX_STACK_SIZE
        this.registerManager = new RegisterManager(lifetimeAnalyzer, memoryManager, codeGenerator);
        this.moduleProcessor = new ModuleProcessor(module, codeGenerator, registerManager,
                memoryManager, lifetimeAnalyzer);
    }

    public void generate() {
        moduleProcessor.processGlobalVariables();
        moduleProcessor.processFunctions();
        writeToFile();
    }

    private void writeToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFilePath))) {
            writer.write(codeGenerator.getAssemblyCode());
        } catch (IOException e) {
            System.err.println("Failed to write assembly to file: " + e.getMessage());
            e.printStackTrace();
        }
    }
}