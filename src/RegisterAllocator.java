import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;

import java.util.*;

public class RegisterAllocator {
    private List<LiveInterval> intervals;  // 所有区间
    private List<LiveInterval> active;     // 当前活跃区间
    private Set<Integer> freeRegisters;    // 可用寄存器集合
    private Map<LLVMValueRef, Location> allocations;  // 分配结果
    private int stackOffset;              // 当前栈偏移
    private static final int WORD_SIZE = 4;  // 字大小(4字节)

    // 构造函数
    public RegisterAllocator() {
        this.intervals = new ArrayList<>();
        this.active = new ArrayList<>();
        this.freeRegisters = new HashSet<>();
        this.allocations = new HashMap<>();
        this.stackOffset = 0;

        // 初始化可用寄存器 (RISC-V)
        // a0-a7 (10-17), t0-t6 (5-7, 28-31), s1-s11 (9, 18-27)
        // 不使用 sp(2), ra(1), s0/fp(8), zero(0), tp(4), gp(3)
        for (int i = 5; i <= 7; i++) freeRegisters.add(i);      // t0-t2
        for (int i = 9; i <= 9; i++) freeRegisters.add(i);      // s1
        for (int i = 10; i <= 17; i++) freeRegisters.add(i);    // a0-a7
        for (int i = 18; i <= 27; i++) freeRegisters.add(i);    // s2-s11
        for (int i = 28; i <= 31; i++) freeRegisters.add(i);    // t3-t6
    }

    // 构建活跃区间
    public void buildIntervals(LLVMModuleRef module) {
        // 清空之前的数据
        intervals.clear();

        // 遍历模块中的每个函数
        for (LLVMValueRef func = LLVMGetFirstFunction(module);
             func != null;
             func = LLVMGetNextFunction(func)) {

            // 跳过外部函数或声明
            if (LLVMCountBasicBlocks(func) == 0) continue;

            // 为当前函数构建变量的定义和使用信息
            Map<LLVMValueRef, Integer> defPoints = new HashMap<>();
            Map<LLVMValueRef, Integer> lastUsePoints = new HashMap<>();
            int instructionPosition = 0;

            // 遍历函数中的每个基本块
            for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(func);
                 bb != null;
                 bb = LLVMGetNextBasicBlock(bb)) {

                // 遍历基本块中的每条指令
                for (LLVMValueRef inst = LLVMGetFirstInstruction(bb);
                     inst != null;
                     inst = LLVMGetNextInstruction(inst)) {

                    // 如果指令定义了值（有名称），记录其定义点
                    if (LLVMGetValueName(inst).getString() != null &&
                            !LLVMGetValueName(inst).getString().isEmpty()) {
                        defPoints.put(inst, instructionPosition);
                    }

                    // 检查所有操作数的使用
                    int numOperands = LLVMGetNumOperands(inst);
                    for (int i = 0; i < numOperands; i++) {
                        LLVMValueRef operand = LLVMGetOperand(inst, i);

                        // 只关注具有名称的值（变量）
                        if (operand != null &&
                                LLVMGetValueName(operand).getString() != null &&
                                !LLVMGetValueName(operand).getString().isEmpty()) {

                            lastUsePoints.put(operand, instructionPosition);
                        }
                    }

                    instructionPosition++;
                }
            }

            // 根据定义点和最后使用点构建存活区间
            for (Map.Entry<LLVMValueRef, Integer> entry : defPoints.entrySet()) {
                LLVMValueRef value = entry.getKey();
                int start = entry.getValue();

                // 如果有使用记录，end为最后使用点+1；否则为定义点+1
                int end = lastUsePoints.containsKey(value) ?
                        lastUsePoints.get(value) + 1 :
                        start + 1;

                // 创建存活区间
                String name = LLVMGetValueName(value).getString();
                LiveInterval interval = new LiveInterval(value, name, start, end);
                intervals.add(interval);
            }
        }

        // 按开始点排序所有存活区间
        intervals.sort(Comparator.comparingInt(LiveInterval::getStart));
    }

    // 执行分配
    public void allocate() {
        // 清空活跃集合和分配结果
        active.clear();
        allocations.clear();
        stackOffset = 0;

        // 按开始点顺序遍历所有存活区间
        for (LiveInterval interval : intervals) {
            // 释放已结束的区间占用的寄存器
            expireOldIntervals(interval);

            if (active.size() == freeRegisters.size()) {
                // 所有寄存器已用完，执行溢出
                spillAtInterval(interval);
            } else {
                // 分配一个空闲寄存器
                int reg = freeRegisters.iterator().next();
                freeRegisters.remove(reg);

                // 将寄存器分配给当前区间
                Location location = new Location(reg);
                interval.setLocation(location);
                allocations.put(interval.getValue(), location);

                // 将区间加入活跃集合，并按结束点排序
                active.add(interval);
                active.sort(LiveInterval::compareEndPoint);
            }
        }
    }

    // 辅助函数: 释放已结束的区间
    private void expireOldIntervals(LiveInterval i) {
        Iterator<LiveInterval> iterator = active.iterator();
        while (iterator.hasNext()) {
            LiveInterval j = iterator.next();

            // 如果j结束点大于等于i开始点，则j仍存活
            if (j.getEnd() >= i.getStart()) {
                return;
            }

            // j已结束，释放它的寄存器
            if (j.getLocation().isRegister()) {
                freeRegisters.add(j.getLocation().getRegister());
            }
            iterator.remove();
        }
    }

    // 辅助函数: 处理溢出
    private void spillAtInterval(LiveInterval i) {
        // 获取活跃集合中结束点最远的区间
        LiveInterval spill = active.get(active.size() - 1);

        if (spill.getEnd() > i.getEnd()) {
            // spill结束点更远，让i使用spill的寄存器
            Location spillLocation = spill.getLocation();
            int register = spillLocation.getRegister();

            // 为spill分配栈空间
            stackOffset -= WORD_SIZE;
            Location stackLocation = new Location(stackOffset, true);
            spill.setLocation(stackLocation);
            allocations.put(spill.getValue(), stackLocation);

            // i使用spill的寄存器
            Location regLocation = new Location(register);
            i.setLocation(regLocation);
            allocations.put(i.getValue(), regLocation);

            // 更新活跃集合
            active.remove(spill);
            active.add(i);
            active.sort(LiveInterval::compareEndPoint);
        } else {
            // i结束点更远，直接将i溢出到栈
            stackOffset -= WORD_SIZE;
            Location stackLocation = new Location(stackOffset, true);
            i.setLocation(stackLocation);
            allocations.put(i.getValue(), stackLocation);
        }
    }

    // 获取分配位置
    public Location getLocation(LLVMValueRef value) {
        return allocations.get(value);
    }

    // 获取所有分配结果
    public Map<LLVMValueRef, Location> getAllocations() {
        return Collections.unmodifiableMap(allocations);
    }

    // 获取栈空间大小
    public int getStackSize() {
        return -stackOffset;
    }
}