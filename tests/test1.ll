; ModuleID = 'module'
source_filename = "module"

define i32 @a() {
aEntry:
  ret i32 1
}

define i32 @main() {
mainEntry:
  %b = alloca i32, align 4
  %c = alloca i32, align 4
  %a = alloca i32, align 4
  store i32 6, i32* %a, align 4
  store i32 5, i32* %b, align 4
  %a1 = load i32, i32* %a, align 4
  %b1 = load i32, i32* %b, align 4
  %addtmp = add i32 %a1, %b1
  store i32 %addtmp, i32* %c, align 4
  %c1 = load i32, i32* %c, align 4
  %eq = icmp eq i32 %c1, 10
  %eqtmp = zext i1 %eq to i32
  %ifcond = icmp ne i32 %eqtmp, 0
  br i1 %ifcond, label %then, label %else

then:                                             ; preds = %mainEntry
  %a11 = load i32, i32* %a, align 4
  br label %ifcont

else:                                             ; preds = %mainEntry
  br label %ifcont

ifcont:                                           ; preds = %else, %then
  ret i32 0

unreachable:                                      ; No predecessors!
}
