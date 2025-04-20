; ModuleID = 'module'
source_filename = "module"

@a = global i32 6

define i32 @main() {
mainEntry:
  %a1 = load i32, i32* @a, align 4
  %eq = icmp eq i32 %a1, 6
  %eqtmp = zext i1 %eq to i32
  %ifcond = icmp ne i32 %eqtmp, 0
  br i1 %ifcond, label %then, label %else

then:                                             ; preds = %mainEntry
  store i32 7, i32* @a, align 4
  br label %ifcont

else:                                             ; preds = %mainEntry
  store i32 8, i32* @a, align 4
  br label %ifcont

ifcont:                                           ; preds = %else, %then
  %a11 = load i32, i32* @a, align 4
  ret i32 %a11
}
