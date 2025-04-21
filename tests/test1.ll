; ModuleID = 'module'
source_filename = "module"

define i32 @main() {
mainEntry:
  %n = alloca i32, align 4
  store i32 3, i32* %n, align 4
  %n1 = load i32, i32* %n, align 4
  %gt = icmp sgt i32 %n1, 1
  %reltmp = zext i1 %gt to i32
  %ifcond = icmp ne i32 %reltmp, 0
  br i1 %ifcond, label %then, label %else

then:                                             ; preds = %mainEntry
  br label %ifcont

else:                                             ; preds = %mainEntry
  br label %ifcont

ifcont:                                           ; preds = %else, %then
  ret i32 0
}
