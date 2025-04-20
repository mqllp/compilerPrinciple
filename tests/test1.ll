; ModuleID = 'module'
source_filename = "module"

@a = global i32 6

define i32 @main() {
mainEntry:
  %a1 = load i32, i32* @a, align 4
  %gt = icmp sgt i32 %a1, 6
  %reltmp = zext i1 %gt to i32
  %ifcond = icmp ne i32 %reltmp, 0
  br i1 %ifcond, label %then, label %else

then:                                             ; preds = %mainEntry
  store i32 9, i32* @a, align 4
  br label %ifcont

else:                                             ; preds = %mainEntry
  store i32 222, i32* @a, align 4
  br label %ifcont

ifcont:                                           ; preds = %else, %then
  %a11 = load i32, i32* @a, align 4
  ret i32 %a11
}
