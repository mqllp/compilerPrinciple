; ModuleID = 'module'
source_filename = "module"

@c = global i32 0

define i32 @main() {
mainEntry:
  %a = alloca i32, align 4
  store i32 0, i32* %a, align 4
  %b = alloca i32, align 4
  store i32 1, i32* %b, align 4
  ret i32 666
}
