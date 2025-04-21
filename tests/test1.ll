; ModuleID = 'module'
source_filename = "module"

define i32 @main() {
mainEntry:
  %n = alloca i32, align 4
  store i32 3, i32* %n, align 4
  %n1 = load i32, i32* %n, align 4
  %lt = icmp slt i32 %n1, 6
  %reltmp = zext i1 %lt to i32
  %ifcond = icmp ne i32 %reltmp, 0
  br i1 %ifcond, label %then, label %ifcont

then:                                             ; preds = %mainEntry
  %n11 = load i32, i32* %n, align 4
  %addtmp = add i32 %n11, 1
  store i32 %addtmp, i32* %n, align 4
  br label %ifcont

ifcont:                                           ; preds = %then, %mainEntry
  %n12 = load i32, i32* %n, align 4
  ret i32 %n12
}
