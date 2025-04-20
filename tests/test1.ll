; ModuleID = 'module'
source_filename = "module"

@a = global i32 10
@b = global i32 20
@x = global i32 5
@y = global i32 10

define void @do_nothing() {
do_nothingEntry:
  ret void
}

define i32 @main() {
mainEntry:
  %i = alloca i32, align 4
  store i32 0, i32* %i, align 4
  br label %whilecond

whilecond:                                        ; preds = %ifcont, %mainEntry
  %i1 = load i32, i32* %i, align 4
  %lt = icmp slt i32 %i1, 10
  %reltmp = zext i1 %lt to i32
  %whilecond1 = icmp ne i32 %reltmp, 0
  br i1 %whilecond1, label %whilebody, label %whileend

whilebody:                                        ; preds = %whilecond
  %i12 = load i32, i32* %i, align 4
  %modtmp = srem i32 %i12, 2
  %eq = icmp eq i32 %modtmp, 0
  %eqtmp = zext i1 %eq to i32
  %ifcond = icmp ne i32 %eqtmp, 0
  br i1 %ifcond, label %then, label %else

whileend:                                         ; preds = %whilecond
  %calltmp = call void @do_nothing()
  %i18 = load i32, i32* %i, align 4
  %iszero = icmp eq i32 %i18, 0
  %nottmp = zext i1 %iszero to i32
  ret i32 %nottmp

then:                                             ; preds = %whilebody
  %x1 = load i32, i32* @x, align 4
  %i13 = load i32, i32* %i, align 4
  %addtmp = add i32 %x1, %i13
  store i32 %addtmp, i32* @x, align 4
  br label %ifcont

else:                                             ; preds = %whilebody
  %x14 = load i32, i32* @x, align 4
  %i15 = load i32, i32* %i, align 4
  %multmp = mul i32 %x14, %i15
  store i32 %multmp, i32* @y, align 4
  br label %ifcont

ifcont:                                           ; preds = %else, %then
  %i16 = load i32, i32* %i, align 4
  %addtmp7 = add i32 %i16, 1
  store i32 %addtmp7, i32* %i, align 4
  br label %whilecond
}

define i32 @sum(i32 %0, i32 %1) {
sumEntry:
  %x = alloca i32, align 4
  store i32 %0, i32* %x, align 4
  %y = alloca i32, align 4
  store i32 %1, i32* %y, align 4
  store i32 10, i32* %x, align 4
  store i32 20, i32* %y, align 4
  %x1 = load i32, i32* %x, align 4
  %y1 = load i32, i32* %y, align 4
  %addtmp = add i32 %x1, %y1
  ret i32 %addtmp
}

define void @test() {
testEntry:
  %result = alloca i32, align 4
  %a1 = load i32, i32* @a, align 4
  %b1 = load i32, i32* @b, align 4
  %calltmp = call i32 @sum(i32 %a1, i32 %b1)
  store i32 %calltmp, i32* %result, align 4
  %result1 = load i32, i32* %result, align 4
  %gt = icmp sgt i32 %result1, 10
  %reltmp = zext i1 %gt to i32
  %and.result = alloca i32, align 4
  %and.left.cond = icmp eq i32 %reltmp, 0
  store i32 0, i32* %and.result, align 4
  br i1 %and.left.cond, label %and.merge, label %and.right

and.right:                                        ; preds = %testEntry
  %result11 = load i32, i32* %result, align 4
  %lt = icmp slt i32 %result11, 15
  %reltmp2 = zext i1 %lt to i32
  %and.right.cond = icmp ne i32 %reltmp2, 0
  %and.right.result = zext i1 %and.right.cond to i32
  store i32 %and.right.result, i32* %and.result, align 4
  br label %and.merge

and.merge:                                        ; preds = %and.right, %testEntry
  %and.load = load i32, i32* %and.result, align 4
  %ifcond = icmp ne i32 %and.load, 0
  br i1 %ifcond, label %then, label %else

then:                                             ; preds = %and.merge
  %result13 = load i32, i32* %result, align 4
  %subtmp = sub i32 %result13, 10
  store i32 %subtmp, i32* %result, align 4
  br label %ifcont

else:                                             ; preds = %and.merge
  %result14 = load i32, i32* %result, align 4
  %lt5 = icmp slt i32 %result14, 20
  %reltmp6 = zext i1 %lt5 to i32
  %ifcond7 = icmp ne i32 %reltmp6, 0
  br i1 %ifcond7, label %then8, label %ifcont9

ifcont:                                           ; preds = %ifcont9, %then
  ret void

then8:                                            ; preds = %else
  %result110 = load i32, i32* %result, align 4
  %addtmp = add i32 %result110, 10
  store i32 %addtmp, i32* %result, align 4
  br label %ifcont9

ifcont9:                                          ; preds = %then8, %else
  br label %ifcont
}

define i32 @factorial(i32 %0) {
factorialEntry:
  %n = alloca i32, align 4
  store i32 %0, i32* %n, align 4
  %n1 = load i32, i32* %n, align 4
  %le = icmp sle i32 %n1, 1
  %reltmp = zext i1 %le to i32
  %ifcond = icmp ne i32 %reltmp, 0
  br i1 %ifcond, label %then, label %else

then:                                             ; preds = %factorialEntry
  br label %ifcont

else:                                             ; preds = %factorialEntry
  %n11 = load i32, i32* %n, align 4
  %n12 = load i32, i32* %n, align 4
  %subtmp = sub i32 %n12, 1
  %calltmp = call i32 @factorial(i32 %subtmp)
  %multmp = mul i32 %n11, %calltmp
  br label %ifcont

ifcont:                                           ; preds = %else, %then
  ret i32 0
}

define void @calculate() {
calculateEntry:
  %num = alloca i32, align 4
  store i32 5, i32* %num, align 4
  %fact = alloca i32, align 4
  %num1 = load i32, i32* %num, align 4
  %calltmp = call i32 @factorial(i32 %num1)
  store i32 %calltmp, i32* %fact, align 4
  %count = alloca i32, align 4
  store i32 0, i32* %count, align 4
  %max = alloca i32, align 4
  store i32 10, i32* %max, align 4
  br label %whilecond

whilecond:                                        ; preds = %whilebody, %calculateEntry
  %count1 = load i32, i32* %count, align 4
  %max1 = load i32, i32* %max, align 4
  %lt = icmp slt i32 %count1, %max1
  %reltmp = zext i1 %lt to i32
  %whilecond1 = icmp ne i32 %reltmp, 0
  br i1 %whilecond1, label %whilebody, label %whileend

whilebody:                                        ; preds = %whilecond
  %count12 = load i32, i32* %count, align 4
  %addtmp = add i32 %count12, 1
  store i32 %addtmp, i32* %count, align 4
  br label %whilecond

whileend:                                         ; preds = %whilecond
  ret void
}
