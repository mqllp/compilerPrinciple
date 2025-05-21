; ModuleID = 'module'
source_filename = "module"

@global_var = global i32 1

define i32 @main() {
mainEntry:
  %num = alloca i32, align 4
  store i32 1, i32* %num, align 4
  %c = alloca i32, align 4
  %num1 = load i32, i32* %num, align 4
  store i32 %num1, i32* %c, align 4
  %num11 = load i32, i32* %num, align 4
  %num12 = load i32, i32* %num, align 4
  %multmp = mul i32 %num11, %num12
  %c1 = load i32, i32* %c, align 4
  %addtmp = add i32 %multmp, %c1
  ret i32 %addtmp
}
