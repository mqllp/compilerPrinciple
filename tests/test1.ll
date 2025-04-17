; ModuleID = 'module'
source_filename = "module"

@a = global i32 0
@count = global i32 0

define i32 @main() {
mainEntry:
  br label %whilecond

whilecond:                                        ; preds = %ifcont, %mainEntry
  %a1 = load i32, i32* @a, align 4
  %le = icmp sle i32 %a1, 0
  %reltmp = zext i1 %le to i32
  %whilecond1 = icmp ne i32 %reltmp, 0
  br i1 %whilecond1, label %whilebody, label %whileend

whilebody:                                        ; preds = %whilecond
  %a12 = load i32, i32* @a, align 4
  %subtmp = sub i32 %a12, 1
  store i32 %subtmp, i32* @a, align 4
  %count1 = load i32, i32* @count, align 4
  %addtmp = add i32 %count1, 1
  store i32 %addtmp, i32* @count, align 4
  %a13 = load i32, i32* @a, align 4
  %lt = icmp slt i32 %a13, -20
  %reltmp4 = zext i1 %lt to i32
  %ifcond = icmp ne i32 %reltmp4, 0
  br i1 %ifcond, label %then, label %ifcont

whileend:                                         ; preds = %then, %whilecond
  %count15 = load i32, i32* @count, align 4
  ret i32 %count15

then:                                             ; preds = %whilebody
  br label %whileend

ifcont:                                           ; preds = %whilebody
  br label %whilecond
}
