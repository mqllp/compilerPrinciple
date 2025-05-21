; ModuleID = 'module'
source_filename = "module"

@global_var = global i32 1

define i32 @main() {
mainEntry:
  %num = alloca i32, align 4
  store i32 6, i32* %num, align 4
  %c = alloca i32, align 4
  store i32 6, i32* %c, align 4
  ret i32 42
}
