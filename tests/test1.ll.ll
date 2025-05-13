; ModuleID = 'module'
source_filename = "module"

define i32 @main() {
mainEntry:
  %kMax = alloca i32, align 4
  store i32 6, i32* %kMax, align 4
  %result = alloca i32, align 4
  store i32 0, i32* %result, align 4
  %i = alloca i32, align 4
  store i32 0, i32* %i, align 4
  br label %whilecond

whilecond:                                        ; preds = %whileend5, %mainEntry
  %i1 = load i32, i32* %i, align 4
  %kMax1 = load i32, i32* %kMax, align 4
  %lt = icmp slt i32 %i1, %kMax1
  %reltmp = zext i1 %lt to i32
  %whilecond1 = icmp ne i32 %reltmp, 0
  br i1 %whilecond1, label %whilebody, label %whileend

whilebody:                                        ; preds = %whilecond
  %j = alloca i32, align 4
  %i12 = load i32, i32* %i, align 4
  store i32 %i12, i32* %j, align 4
  br label %whilecond3

whileend:                                         ; preds = %whilecond
  %result124 = load i32, i32* %result, align 4
  ret i32 %result124

whilecond3:                                       ; preds = %ifcont17, %then16, %whilebody
  %j1 = load i32, i32* %j, align 4
  %kMax16 = load i32, i32* %kMax, align 4
  %lt7 = icmp slt i32 %j1, %kMax16
  %reltmp8 = zext i1 %lt7 to i32
  %whilecond9 = icmp ne i32 %reltmp8, 0
  br i1 %whilecond9, label %whilebody4, label %whileend5

whilebody4:                                       ; preds = %whilecond3
  %j110 = load i32, i32* %j, align 4
  %kMax111 = load i32, i32* %kMax, align 4
  %subtmp = sub i32 %kMax111, 1
  %eq = icmp eq i32 %j110, %subtmp
  %eqtmp = zext i1 %eq to i32
  %ifcond = icmp ne i32 %eqtmp, 0
  br i1 %ifcond, label %then, label %ifcont

whileend5:                                        ; preds = %then, %whilecond3
  %i122 = load i32, i32* %i, align 4
  %addtmp23 = add i32 %i122, 1
  store i32 %addtmp23, i32* %i, align 4
  br label %whilecond

then:                                             ; preds = %whilebody4
  br label %whileend5

ifcont:                                           ; preds = %whilebody4
  %j112 = load i32, i32* %j, align 4
  %modtmp = srem i32 %j112, 2
  %eq13 = icmp eq i32 %modtmp, 0
  %eqtmp14 = zext i1 %eq13 to i32
  %ifcond15 = icmp ne i32 %eqtmp14, 0
  br i1 %ifcond15, label %then16, label %ifcont17

then16:                                           ; preds = %ifcont
  %j118 = load i32, i32* %j, align 4
  %addtmp = add i32 %j118, 1
  store i32 %addtmp, i32* %j, align 4
  br label %whilecond3

ifcont17:                                         ; preds = %ifcont
  %result1 = load i32, i32* %result, align 4
  %addtmp19 = add i32 %result1, 1
  store i32 %addtmp19, i32* %result, align 4
  %j120 = load i32, i32* %j, align 4
  %addtmp21 = add i32 %j120, 1
  store i32 %addtmp21, i32* %j, align 4
  br label %whilecond3
}
