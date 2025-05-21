; ModuleID = 'module'
source_filename = "module"

define i32 @main() {
mainEntry:
  %a = alloca i32, align 4
  store i32 1, i32* %a, align 4
  br label %whilecond

whilecond:                                        ; preds = %ifcont, %mainEntry
  br i1 true, label %whilebody, label %whileend

whilebody:                                        ; preds = %whilecond
  %a1 = load i32, i32* %a, align 4
  %addtmp = add i32 %a1, 1
  store i32 %addtmp, i32* %a, align 4
  %a11 = load i32, i32* %a, align 4
  %gt = icmp sgt i32 %a11, 100
  %reltmp = zext i1 %gt to i32
  %ifcond = icmp ne i32 %reltmp, 0
  br i1 %ifcond, label %then, label %ifcont

whileend:                                         ; preds = %then, %whilecond
  %a12 = load i32, i32* %a, align 4
  ret i32 %a12

then:                                             ; preds = %whilebody
  br label %whileend

ifcont:                                           ; preds = %whilebody
  br label %whilecond
}
