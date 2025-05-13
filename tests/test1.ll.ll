; ModuleID = 'module'
source_filename = "module"

define i32 @main() {
mainEntry:
  %a = alloca i32, align 4
  store i32 666, i32* %a, align 4
  %a1 = load i32, i32* %a, align 4
  %gt = icmp sgt i32 %a1, 555
  %reltmp = zext i1 %gt to i32
  %ifcond = icmp ne i32 %reltmp, 0
  br i1 %ifcond, label %then, label %else

then:                                             ; preds = %mainEntry
  %a11 = load i32, i32* %a, align 4
  ret i32 %a11

else:                                             ; preds = %mainEntry
  ret i32 222

ifcont:                                           ; No predecessors!
  ret i32 0
}
