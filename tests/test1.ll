; ModuleID = 'module'
source_filename = "module"

@a = global i32 0
@b = global i32 1
@c = global i32 2
@d = global i32 3

define i32 @xyz() {
xyzEntry:
  %n = alloca i32, align 4
  store i32 3, i32* %n, align 4
  %n1 = load i32, i32* %n, align 4
  ret i32 %n1
}

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
  %a1 = load i32, i32* @a, align 4
  %b1 = load i32, i32* @b, align 4
  %addtmp = add i32 %a1, %b1
  %c1 = load i32, i32* @c, align 4
  %addtmp1 = add i32 %addtmp, %c1
  %d1 = load i32, i32* @d, align 4
  %addtmp2 = add i32 %addtmp1, %d1
  %calltmp = call i32 @xyz()
  %addtmp3 = add i32 %addtmp2, %calltmp
  ret i32 %addtmp3
}
