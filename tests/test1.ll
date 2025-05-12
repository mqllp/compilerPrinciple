; ModuleID = 'module'
source_filename = "module"

@a = global i32 1
@b = global i32 0

define i32 @main() {
mainEntry:
  %c = alloca i32, align 4
  store i32 3, i32* %c, align 4
  %c1 = load i32, i32* %c, align 4
  %a1 = load i32, i32* @a, align 4
  %addtmp = add i32 %c1, %a1
  %addtmp1 = add i32 %addtmp, 1
  store i32 %addtmp1, i32* @b, align 4
  %a12 = load i32, i32* @a, align 4
  ret i32 %a12
}
