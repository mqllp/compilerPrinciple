; ModuleID = 'module'
source_filename = "module"

define i32 @main() {
mainEntry:
  %n = alloca i32, align 4
  store i32 3, i32* %n, align 4
  br label %whilecond

whilecond:                                        ; preds = %whilebody, %mainEntry
  %n1 = load i32, i32* %n, align 4
  %lt = icmp slt i32 %n1, 6
  %reltmp = zext i1 %lt to i32
  %whilecond1 = icmp ne i32 %reltmp, 0
  br i1 %whilecond1, label %whilebody, label %whileend

whilebody:                                        ; preds = %whilecond
  %n12 = load i32, i32* %n, align 4
  %addtmp = add i32 %n12, 1
  store i32 %addtmp, i32* %n, align 4
  br label %whilecond

whileend:                                         ; preds = %whilecond
  %n13 = load i32, i32* %n, align 4
  ret i32 %n13
}
