; ModuleID = 'module'
source_filename = "module"

define i32 @main() {
mainEntry:
  %num = alloca i32, align 4
  store i32 1, i32* %num, align 4
  br label %whilecond

whilecond:                                        ; preds = %whilebody, %mainEntry
  %num1 = load i32, i32* %num, align 4
  %lt = icmp slt i32 %num1, 10
  %reltmp = zext i1 %lt to i32
  %whilecond1 = icmp ne i32 %reltmp, 0
  br i1 %whilecond1, label %whilebody, label %whileend

whilebody:                                        ; preds = %whilecond
  %num12 = load i32, i32* %num, align 4
  %addtmp = add i32 %num12, 1
  store i32 %addtmp, i32* %num, align 4
  br label %whilecond

whileend:                                         ; preds = %whilecond
  %result = alloca i32, align 4
  %num13 = load i32, i32* %num, align 4
  store i32 %num13, i32* %result, align 4
  %a = alloca i32, align 4
  store i32 2, i32* %a, align 4
  %c = alloca i32, align 4
  %a1 = load i32, i32* %a, align 4
  store i32 %a1, i32* %c, align 4
  %result1 = load i32, i32* %result, align 4
  ret i32 %result1
}
