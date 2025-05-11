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
  %d = alloca i32, align 4
  store i32 10, i32* %d, align 4
  %a12 = load i32, i32* @a, align 4
  %b1 = load i32, i32* @b, align 4
  %addtmp3 = add i32 %a12, %b1
  %c14 = load i32, i32* %c, align 4
  %addtmp5 = add i32 %addtmp3, %c14
  %d1 = load i32, i32* %d, align 4
  %addtmp6 = add i32 %addtmp5, %d1
  ret i32 %addtmp6
}
