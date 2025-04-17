; ModuleID = 'SysYModule'
source_filename = "SysYModule"

@g_var = global i32 2

define i32 @main() {
mainEntry:
  %a = alloca i32, align 4
  store i32 1, i32* %a, align 4
  %a_load = load i32, i32* %a, align 4
  %g_var_load = load i32, i32* @g_var, align 4
  %addtmp = add i32 %a_load, %g_var_load
}
