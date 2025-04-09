# LLVM JAVA API使用手册
## 准备工作

- 首先你需要安装LLVM，相关安装方法可查看环境配置

- Maven引入与LLVM相关的包（或自行下载jar包），详见LAB4实验要求

- 下面将介绍两种在Java中使用LLVM API的方式，一种基于LLVM4J，一种基于LLVM-Platform，无论采用哪一套API都能完成本次实验的全部内容。

### 方案一：LLVM4J（推荐）

- LLVM4J是Mats Jun Larsen等开发的用于Java平台的LLVM接口。

- 本实验所使用的是助教修改后的LLVM4J，你可以点击这个链接查看源代码，也可以clone下来阅读。

- 你可以在这里查看LLVM4J的各种API的使用示例。

- 下面对LLVM4J的部分你需要了解的API作介绍。

#### LLVM4J中你需要了解的API

1. org.llvm4j.optional.Option

    LLVM4J使用的Option类，类似 C++ 中的 optional 或 Rust 中的 Option。

   - Option.empty() 返回一个包装了空值的Option对象

   - Option.of(value) 返回一个包装了value对象的Option对象

   - unwrap() 返回将Option对象拆包得到的对象

2. org.llvm4j.llvm4j.Context

    LLVM4J的上下文类。

   - newIRBuilder()
     - 返回一个IRBuilder类型对象

   - newModule()

     - 返回一个Module类型对象，你可以粗略地认为我们要生成的一个LLVM IR文件就是一个Module

   - getFunctionType(returnType, parameters, isVariadic)

     - 返回一个 FunctionType 类型对象，表示创建一个返回值类型为 returnType，参数类型数组为 parameters，是否可变参数为 isVariadic 的函数类型

   - newBasicBlock(name)

     - 返回一个名字为name的基本块

   - getInt32Type()

     - 返回一个IntegerType类型对象，表示32位整型

3. org.llvm4j.llvm4j.Module

LLVM4J的模块类，如前所述，你可以认为一个模块就是要生成的一个LLVM IR文件。

- dump(file)

  - 将模块输出到目标文件

- addFunction(name, type)

  - 在模块中添加一个名字为name，类型为type的函数并返回

- addGlobalVariable(name, type, addressSpace)

  - 在模块中添加全局变量，名字为name，类型为type。addressSpace这一参数设置为Option.empty()即可


4. org.llvm4j.llvm4j.IRBuilder

LLVM4J的IR构建器类，你需要使用它生成各种LLVM IR指令。

- positionAfter(basicBlock)

  -  在基本块 basicBlock 后面添加IR

- buildAlloca(type, name) , buildStore(ptr, value), ...

  - 指令构建方法

- buildIntCompare(predicate, lhs, rhs, name)

  - 后三个参数很好理解，第一个参数predicate是IntPredicate枚举类型，你需要传入它的一个具体元素如IntPredicate.NotEqual

- buildIntAdd(lhs, rhs, semantics, name)

  - 第三个参数 semantics 表示对于溢出的处理方式，由于实验中我们并不要求溢出处理，所以直接填写 WrapSemantics.Unspecified 作默认处理即可

5. org.llvm4j.llvm4j.Type

表示LLVM IR的类型对象，其子类有VoidType、IntegerType、FunctionType、ArrayType、PointerType等

6. org.llvm4j.llvm4j.Value

LLVM IR的值对象，包括LLVM IR中的常量、变量、函数等

#### LLVM4J使用例

导入包：
```
import org.llvm4j.llvm4j.*;
import org.llvm4j.optional.Option;
```
定义一些字段：
```
private static final Context context = new Context();

private static final IRBuilder builder = context.newIRBuilder();

private static final Module mod = context.newModule("module");

private static final IntegerType i32 = context.getInt32Type();

private static final ConstantInt zero = i32.getConstant(0, false);

private final File outputFile;
```
生成IR：

```
private static void example() {
	// 添加全局变量
	var gVal = mod.addGlobalVariable("g_var", context.getInt32Type(), Option.empty()).unwrap();
    gVal.setInitializer(zero);

    // 添加函数
    var main = mod.addFunction("main",
                context.getFunctionType(context.getInt32Type(), new Type[] {}, false));

    // 定义基本块
    var entryBlock = context.newBasicBlock(main.getName() + "Entry");
    var labelTrue = context.newBasicBlock("true");
    var labelFalse = context.newBasicBlock("false");

    // 添加基本块到main函数
    main.addBasicBlock(entryBlock);

    // 在entryBlock后面追加IR
    builder.positionAfter(entryBlock);

    // 生成各种指令
    var loadInstruction = builder.buildLoad(gVal, Option.of("tmp"));
    var tmp = loadInstruction.getLValue();

    var icmp = builder.buildIntCompare(IntPredicate.Equal, tmp, zero, Option.of("cmp"));
    var tmp1 = builder.buildZeroExt(icmp, context.getInt32Type(), Option.of("tmp1"));
    var cond = builder.buildIntCompare(IntPredicate.NotEqual, tmp1, zero, Option.of("cmp"));
    var br = builder.buildConditionalBranch(cond, labelTrue, labelFalse);

    main.addBasicBlock(labelTrue);
    builder.positionAfter(labelTrue);
    var ret = builder.buildCall(mod.getFunction("main").unwrap(),
                new Value[]{gVal}, Option.of("res"));
    builder.buildReturn(Option.of(ret));
    main.addBasicBlock(labelFalse);
    builder.positionAfter(labelFalse);

    builder.buildReturn(Option.of(zero));

    // 输出IR
    mod.dump(Option.of(outputFile));
}
```

### 方案二：LLVM-Platform
#### 初始化LLVM

- import LLVM
```
import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;
```
```
//初始化LLVM
LLVMInitializeCore(LLVMGetGlobalPassRegistry());
LLVMLinkInMCJIT();
LLVMInitializeNativeAsmPrinter();
LLVMInitializeNativeAsmParser();
LLVMInitializeNativeTarget();
```
#### 创建模块

- 你可以粗略认为一个文件就是一个模块
```
    //创建module
    LLVMModuleRef module = LLVMModuleCreateWithName("module");

    //初始化IRBuilder，后续将使用这个builder去生成LLVM IR
    LLVMBuilderRef builder = LLVMCreateBuilder();

    //考虑到我们的语言中仅存在int一个基本类型，可以通过下面的语句为LLVM的int型重命名方便以后使用
    LLVMTypeRef i32Type = LLVMInt32Type();
```
#### 创建全局变量
```
    //创建一个常量,这里是常数0
    LLVMValueRef zero = LLVMConstInt(i32Type, 0, /* signExtend */ 0);

    //创建名为globalVar的全局变量
    LLVMValueRef globalVar = LLVMAddGlobal(module, i32Type, /*globalVarName:String*/"globalVar");

    //为全局变量设置初始化器
    LLVMSetInitializer(globalVar, /* constantVal:LLVMValueRef*/zero);
```
#### 生成函数

- 先生成返回值类型
- 多个参数时需先生成函数的参数类型，再生成函数类型
- 用生成的函数类型去生成函数
```
    //生成返回值类型
    LLVMTypeRef returnType = i32Type;

    //生成函数参数类型
    PointerPointer<Pointer> argumentTypes = new PointerPointer<>(2)
                .put(0, i32Type)
                .put(1, i32Type);

    //生成函数类型
    LLVMTypeRef ft = LLVMFunctionType(returnType, argumentTypes, /* argumentCount */ 2, /* isVariadic */ 0);
    //若仅需一个参数也可以使用如下方式直接生成函数类型
    ft = LLVMFunctionType(returnType, i32Type, /* argumentCount */ 1, /* isVariadic */ 0);

    //生成函数，即向之前创建的module中添加函数
    LLVMValueRef function = LLVMAddFunction(module, /*functionName:String*/"function", ft);
```
#### 创建基本块并添加指令

- 基本块即一块只能从头部进入，从尾部退出的代码块
- 基本块的后面跟着终止符指令，内部全部为非终止符指令，这些终止符指令指示在当前块完成后应执行哪个块，最常见的有ret即函数返回，br块跳转，switch多块选择等
- 大多数指令都很容易找到，想具体确定都有哪些可生成的指令可以参考这里->org.bytedeco.llvm.global->LLVM

```
    //通过如下语句在函数中加入基本块，一个函数可以加入多个基本块
    LLVMBasicBlockRef block1 = LLVMAppendBasicBlock(function, /*blockName:String*/"block1");

    LLVMBasicBlockRef block2 = LLVMAppendBasicBlock(function, /*blockName:String*/"block2");

    //选择要在哪个基本块后追加指令
    LLVMPositionBuilderAtEnd(builder, block1);//后续生成的指令将追加在block1的后面

    //获取函数的参数
    LLVMValueRef n = LLVMGetParam(function, /* parameterIndex */0);

    //创建add指令并将结果保存在一个临时变量中
    LLVMValueRef result = LLVMBuildAdd(builder, n, zero, /* varName:String */"result");

    //跳转指令决定跳转到哪个块
    LLVMBuildBr(builder, block2);

    //生成比较指令
    LLVMValueRef condition = LLVMBuildICmp(builder, /*这是个int型常量，表示比较的方式*/LLVMIntEQ, n, zero, "condition = n == 0");
    /* 上面参数中的常量包含如下取值
        LLVMIntEQ,
        LLVMIntNE,
        LLVMIntUGT,
        LLVMIntUGE,
        LLVMIntULT,
        LLVMIntULE,
        LLVMIntSGT,
        LLVMIntSGE,
        LLVMIntSLT,
        LLVMIntSLE,
    */
    //条件跳转指令，选择跳转到哪个块
    LLVMBuildCondBr(builder, 
    /*condition:LLVMValueRef*/ condition, 
    /*ifTrue:LLVMBasicBlockRef*/ ifTrue, 
    /*ifFalse:LLVMBasicBlockRef*/ ifFalse);

    LLVMPositionBuilderAtEnd(builder, block2);//后续生成的指令将追加在block2的后面

    //函数返回指令
    LLVMBuildRet(builder, /*result:LLVMValueRef*/result);
```

- 在基本块中创建并使用局部变量
  - 对于数组变量请使用LLVMVectorType或者LLVMArrayType类型
  - 数组变量使用其中的值时需要使用GetElementPtr指令，请自行到官方文档学习如何生成与使用GEP指令

```
LLVMPositionBuilderAtEnd(builder, block1);

    //int型变量
    //申请一块能存放int型的内存
    LLVMValueRef pointer = LLVMBuildAlloca(builder, i32Type, /*pointerName:String*/"pointer");

    //将数值存入该内存
    LLVMBuildStore(builder, zero, pointer);

    //从内存中将值取出
    LLVMValueRef value = LLVMBuildLoad(builder, pointer, /*varName:String*/"value");


    //数组变量
    //创建可存放200个int的vector类型
    LLVMTypeRef vectorType = LLVMVectorType(i32Type, 200);

    //申请一个可存放该vector类型的内存
    LLVMValueRef vectorPointer = LLVMBuildAlloca(builder, vectorType, "vectorPointor");

```

#### 输出LLVM IR

- 输出到控制台
```
LLVMDumpModule(module);
```
- 输出到文件
```
public static final BytePointer error = new BytePointer();
LLVMPrintModuleToFile(module,"test.ll",error);
```

### 关于如何查找java中使用生成LLVM IR的API

这里以翻译not exp为例。

main.c文件中程序如下:
```
int main() {
    int num = 5;
    return !num;
}
```
利用命令clang -S -emit-llvm main.c -o main.ll -O0将上面的程序生成到LLVM IR，并保存在main.ll文件中，该文件内容如下

```
; ModuleID = 'main.c'
source_filename = "main.c"
target datalayout = "e-m:e-p270:32:32-p271:32:32-p272:64:64-i64:64-f80:128-n8:16:32:64-S128"
target triple = "x86_64-pc-linux-gnu"

; Function Attrs: noinline nounwind optnone sspstrong uwtable
define dso_local i32 @main() #0 {
  %1 = alloca i32, align 4
  %2 = alloca i32, align 4
  store i32 0, i32* %1, align 4
  store i32 5, i32* %2, align 4
  %3 = load i32, i32* %2, align 4
  %4 = icmp ne i32 %3, 0
  %5 = xor i1 %4, true
  %6 = zext i1 %5 to i32
  ret i32 %6
}

attributes #0 = { noinline nounwind optnone sspstrong uwtable "frame-pointer"="all" "min-legal-vector-width"="0" "no-trapping-math"="true" "stack-protector-buffer-size"="8" "target-cpu"="x86-64" "target-features"="+cx8,+fxsr,+mmx,+sse,+sse2,+x87" "tune-cpu"="generic" }

!llvm.module.flags = !{!0, !1, !2, !3, !4}
!llvm.ident = !{!5}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{i32 7, !"PIC Level", i32 2}
!2 = !{i32 7, !"PIE Level", i32 2}
!3 = !{i32 7, !"uwtable", i32 1}
!4 = !{i32 7, !"frame-pointer", i32 2}
!5 = !{!"clang version 13.0.1"}
```

我们主要关注函数定义内容，并且忽略%1 = alloca i32, align 4与store i32 0, i32* %1, align 4。
```
// 对应 int num = 5;
%2 = alloca i32, align 4
store i32 5, i32* %2, align 4
// 对应 return !num;
%3 = load i32, i32* %2, align 4
%4 = icmp ne i32 %3, 0
%5 = xor i1 %4, true
%6 = zext i1 %5 to i32
ret i32 %6
```
可以看到，在翻译return !num;时，首先取出num的值到%3中，然后比较%3的值与0是否不相等（注意返回值%4不是int32类型，而是int1类型），接着用%4与true做异或得到%5，然后将int1类型的%5扩展为int32类型，最后返回。

上面的翻译过程在助教的代码中表现为：
```
// 生成icmp
tmp_ = LLVMBuildICmp(builder, LLVMIntNE, LLVMConstInt(i32Type, 0, 0), tmp_, "tmp_");
// 生成xor
tmp_ = LLVMBuildXor(builder, tmp_, LLVMConstInt(LLVMInt1Type(), 1, 0), "tmp_");
// 生成zext
tmp_ = LLVMBuildZExt(builder, tmp_, i32Type, "tmp_");
```
下面我就上面的程序翻译提出三个问题并给出解答：

- 1、我从来没接触过LLVM IR，怎么知道如何翻译呢？
  - 按照我们的演示过程，你可以自己手动写一个测试文件，并且生成LLVM IR的格式。对着它，你就知道可以选择LLVMBuildICmp、LLVMBuildXor、LLVMBuildZExt这三个方法来生成对应的指令了。
- 2、我如何知道上面三个方法的参数的含义呢？
  - 浏览器搜索。这里提供两个网址https://thedan64.github.io/inkwell///llvm_sys/core/fn.LLVMBuildZExt.html、http://bytedeco.org/javacpp-presets/llvm/apidocs/org/bytedeco/llvm/global/LLVM.html，可以搜索对应的函数，并通过参数名推测各参数的含义。
- 3、对于LLVMBuildICmp方法，我根本不知道它的第二个参数该怎么办设置咋办？上面的网址搜出来的只有这个LLVMBuildICmp(LLVMBuilderRef arg0, int Op, LLVMValueRef LHS, LLVMValueRef RHS, BytePointer Name)，第二个参数应该是个常量，但怎么知道该设置成什么呢？
  - 借助github。
    - 首先搜索LLVMBuildICmp。
    - 点击Code，然后选择Java
    - 可以看到LLVMIntSLE，LLVMIntEQ等出现在了第二个参数的位置上，你可以选择clone这些项目，然后查看LLVMIntSLE，LLVMIntEQ是在哪里定义的。

