; ModuleID = 'module'
source_filename = "module"

define i32 @main() {
mainEntry:
  %n = alloca i32, align 4
  store i32 3, i32* %n, align 4
  %n1 = load i32, i32* %n, align 4
  ret i32 %n1
}
