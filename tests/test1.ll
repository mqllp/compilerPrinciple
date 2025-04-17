; ModuleID = 'module'
source_filename = "module"

@g_var = global i32 2

define i32 @main() {
mainEntry:
  %a = alloca i32, align 4
  store i32 1, i32* %a, align 4
  %a1 = load i32, i32* %a, align 4
  %g_var1 = load i32, i32* @g_var, align 4
  %addtmp = add i32 %a1, %g_var1
  %a2 = alloca i32, align 4
  store i32 1, i32* %a2, align 4
  %a13 = load i32, i32* %a2, align 4
  %g_var14 = load i32, i32* @g_var, align 4
  %addtmp5 = add i32 %a13, %g_var14
  ret i32 %addtmp5
}
