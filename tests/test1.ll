; ModuleID = 'module'
source_filename = "module"

@count = global i32 0
@n = global i32 3

define void @hanoi(i32 %0, i32 %1, i32 %2, i32 %3) {
hanoiEntry:
  %n = alloca i32, align 4
  store i32 %0, i32* %n, align 4
  %source = alloca i32, align 4
  store i32 %1, i32* %source, align 4
  %target = alloca i32, align 4
  store i32 %2, i32* %target, align 4
  %auxiliary = alloca i32, align 4
  store i32 %3, i32* %auxiliary, align 4
  %n1 = load i32, i32* %n, align 4
  %eq = icmp eq i32 %n1, 1
  %eqtmp = zext i1 %eq to i32
  %ifcond = icmp ne i32 %eqtmp, 0
  br i1 %ifcond, label %then, label %ifcont

then:                                             ; preds = %hanoiEntry
  %count1 = load i32, i32* @count, align 4
  %addtmp = add i32 %count1, 1
  store i32 %addtmp, i32* @count, align 4
  ret void

ifcont:                                           ; preds = %hanoiEntry
  %n11 = load i32, i32* %n, align 4
  %subtmp = sub i32 %n11, 1
  %source1 = load i32, i32* %source, align 4
  %auxiliary1 = load i32, i32* %auxiliary, align 4
  %target1 = load i32, i32* %target, align 4
  %calltmp = call void @hanoi(i32 %subtmp, i32 %source1, i32 %auxiliary1, i32 %target1)
  %count12 = load i32, i32* @count, align 4
  %addtmp3 = add i32 %count12, 1
  store i32 %addtmp3, i32* @count, align 4
  %n14 = load i32, i32* %n, align 4
  %subtmp5 = sub i32 %n14, 1
  %auxiliary16 = load i32, i32* %auxiliary, align 4
  %target17 = load i32, i32* %target, align 4
  %source18 = load i32, i32* %source, align 4
  %calltmp9 = call void @hanoi(i32 %subtmp5, i32 %auxiliary16, i32 %target17, i32 %source18)
  ret void
}

define i32 @main() {
mainEntry:
  %n1 = load i32, i32* @n, align 4
  %calltmp = call void @hanoi(i32 %n1, i32 1, i32 3, i32 2)
  %count1 = load i32, i32* @count, align 4
  ret i32 %count1
}
