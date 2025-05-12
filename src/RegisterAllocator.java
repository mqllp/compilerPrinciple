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
        // 使用 t0-t6 (5-7, 28-31) 和 s2-s11 (18-27) 作为可分配寄存器
        // 保留 a0-a7 用于函数参数和返回值
        // 不使用 sp(2), ra(1), s0/fp(8), s1(9), zero(0), tp(4), gp(3)
        for (int i = 5; i <= 7; i++) freeRegisters.add(i);      // t0-t2
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

            // 存储每个值的首次出现和最后出现位置
            Map<LLVMValueRef, Integer> firstOccurrence = new HashMap<>();
            Map<LLVMValueRef, Integer> lastOccurrence = new HashMap<>();
            int position = 0;

            // 处理函数参数
            int paramCount = LLVMCountParams(func);
            LLVMValueRef[] params = new LLVMValueRef[paramCount];
            for (int i = 0; i < paramCount; i++) {
                params[i] = LLVMGetParam(func, i);
            }

            for (LLVMValueRef param : params) {
                if (LLVMGetValueName(param).getString() != null &&
                        !LLVMGetValueName(param).getString().isEmpty()) {
                    firstOccurrence.put(param, position);
                }
            }

            // 遍历函数中的每个基本块
            for (LLVMBasicBlockRef bb = LLVMGetFirstBasicBlock(func);
                 bb != null;
                 bb = LLVMGetNextBasicBlock(bb)) {

                // 遍历基本块中的每条指令
                for (LLVMValueRef inst = LLVMGetFirstInstruction(bb);
                     inst != null;
                     inst = LLVMGetNextInstruction(inst)) {

                    // 记录指令定义的值
                    if (LLVMGetValueName(inst).getString() != null &&
                            !LLVMGetValueName(inst).getString().isEmpty()) {
                        firstOccurrence.putIfAbsent(inst, position);
                    }

                    // 记录操作数的使用
                    int numOperands = LLVMGetNumOperands(inst);
                    for (int i = 0; i < numOperands; i++) {
                        LLVMValueRef operand = LLVMGetOperand(inst, i);
                        if (operand != null &&
                                LLVMGetValueName(operand).getString() != null &&
                                !LLVMGetValueName(operand).getString().isEmpty()) {

                            // 记录首次出现
                            firstOccurrence.putIfAbsent(operand, position);
                            // 更新最后出现
                            lastOccurrence.put(operand, position);
                        }
                    }

                    position++;
                }
            }

            // 创建存活区间
            for (Map.Entry<LLVMValueRef, Integer> entry : firstOccurrence.entrySet()) {
                LLVMValueRef value = entry.getKey();
                int start = entry.getValue();
                // 如果没有使用记录，则生命周期只有定义点一个位置
                int end = lastOccurrence.getOrDefault(value, start) + 1;

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

            if (active.size() >= freeRegisters.size()) {
                // 没有足够的寄存器，需要溢出
                spillAtInterval(interval);
            } else {
                // 分配一个空闲寄存器
                int reg = freeRegisters.iterator().next();
                freeRegisters.remove(reg);

                // 记录分配
                Location location = new Location(reg);
                interval.setLocation(location);
                allocations.put(interval.getValue(), location);

                // 加入活跃集合
                active.add(interval);
                // 按结束点排序
                active.sort(LiveInterval::compareEndPoint);
            }
        }
    }

    // 辅助函数: 释放已结束的区间
    private void expireOldIntervals(LiveInterval current) {
        // 创建迭代器以便安全移除元素
        Iterator<LiveInterval> iterator = active.iterator();

        // 遍历当前活跃的所有区间
        while (iterator.hasNext()) {
            LiveInterval interval = iterator.next();

            // 如果区间结束点大于等于当前区间的开始点，表示仍然存活
            if (interval.getEnd() > current.getStart()) {
                break; // 因为active已按结束点排序，后续区间也都还存活
            }

            // 区间已结束，释放其寄存器
            if (interval.getLocation().isRegister()) {
                freeRegisters.add(interval.getLocation().getRegister());
            }

            // 从活跃集合中移除
            iterator.remove();
        }
    }

    // 辅助函数: 处理溢出
    private void spillAtInterval(LiveInterval current) {
        // 找到活跃集合中结束点最远的区间（最后一个元素）
        LiveInterval spill = active.get(active.size() - 1);

        if (spill.getEnd() > current.getEnd()) {
            // 如果spill的结束点更远，则让current使用spill的寄存器
            int register = spill.getLocation().getRegister();

            // 为spill分配栈位置
            stackOffset -= WORD_SIZE;
            Location stackLoc = new Location(stackOffset, true);
            spill.setLocation(stackLoc);
            allocations.put(spill.getValue(), stackLoc);

            // 将寄存器分配给current
            Location regLoc = new Location(register);
            current.setLocation(regLoc);
            allocations.put(current.getValue(), regLoc);

            // 更新活跃集合
            active.remove(spill);
            active.add(current);
            active.sort(LiveInterval::compareEndPoint);
        } else {
            // current结束点更远，直接将current分配到栈上
            stackOffset -= WORD_SIZE;
            Location stackLoc = new Location(stackOffset, true);
            current.setLocation(stackLoc);
            allocations.put(current.getValue(), stackLoc);
        }
    }

    // 获取变量的分配位置
    public Location getLocation(LLVMValueRef value) {
        return allocations.get(value);
    }

    // 获取所有分配结果
    public Map<LLVMValueRef, Location> getAllocations() {
        return Collections.unmodifiableMap(allocations);
    }

    // 获取栈空间大小
    public int getStackSize() {
        return Math.abs(stackOffset);
    }
}