; ModuleID = 'SysYModule'
source_filename = "SysYModule"

define i32 @f(i32 %0) {
fEntry:
  %i = alloca i32, align 4
  store i32 %0, i32* %i, align 4
  %i_load = load i32, i32* %i, align 4
}

define i32 @main() {
mainEntry:
  %a = alloca i32, align 4
  store i32 1, i32* %a, align 4
  %a_load = load i32, i32* %a, align 4
  %calltmp = call i32 @f(i32 %a_load)
}
