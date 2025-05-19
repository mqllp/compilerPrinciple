; ModuleID = 'module'
source_filename = "module"

define i32 @main() {
mainEntry:
  %x1 = alloca i32, align 4
  store i32 1, i32* %x1, align 4
  %x2 = alloca i32, align 4
  store i32 2, i32* %x2, align 4
  %x3 = alloca i32, align 4
  store i32 3, i32* %x3, align 4
  %x4 = alloca i32, align 4
  store i32 4, i32* %x4, align 4
  %x5 = alloca i32, align 4
  store i32 5, i32* %x5, align 4
  %x6 = alloca i32, align 4
  store i32 6, i32* %x6, align 4
  %x7 = alloca i32, align 4
  store i32 7, i32* %x7, align 4
  %x8 = alloca i32, align 4
  store i32 8, i32* %x8, align 4
  %x9 = alloca i32, align 4
  store i32 9, i32* %x9, align 4
  %x10 = alloca i32, align 4
  store i32 10, i32* %x10, align 4
  %x11 = alloca i32, align 4
  store i32 11, i32* %x11, align 4
  %x12 = alloca i32, align 4
  store i32 12, i32* %x12, align 4
  %x13 = alloca i32, align 4
  store i32 13, i32* %x13, align 4
  %x14 = alloca i32, align 4
  store i32 14, i32* %x14, align 4
  %x15 = alloca i32, align 4
  store i32 15, i32* %x15, align 4
  %x16 = alloca i32, align 4
  store i32 16, i32* %x16, align 4
  %x17 = alloca i32, align 4
  store i32 17, i32* %x17, align 4
  %x18 = alloca i32, align 4
  store i32 18, i32* %x18, align 4
  %x19 = alloca i32, align 4
  store i32 19, i32* %x19, align 4
  %x20 = alloca i32, align 4
  store i32 20, i32* %x20, align 4
  %x21 = alloca i32, align 4
  store i32 21, i32* %x21, align 4
  %x22 = alloca i32, align 4
  store i32 22, i32* %x22, align 4
  %x23 = alloca i32, align 4
  store i32 23, i32* %x23, align 4
  %x24 = alloca i32, align 4
  store i32 24, i32* %x24, align 4
  %x25 = alloca i32, align 4
  store i32 25, i32* %x25, align 4
  %x111 = load i32, i32* %x1, align 4
  %x212 = load i32, i32* %x2, align 4
  %addtmp = add i32 %x111, %x212
  %x31 = load i32, i32* %x3, align 4
  %addtmp3 = add i32 %addtmp, %x31
  %x41 = load i32, i32* %x4, align 4
  %addtmp4 = add i32 %addtmp3, %x41
  %x51 = load i32, i32* %x5, align 4
  %addtmp5 = add i32 %addtmp4, %x51
  %x61 = load i32, i32* %x6, align 4
  %addtmp6 = add i32 %addtmp5, %x61
  %x71 = load i32, i32* %x7, align 4
  %addtmp7 = add i32 %addtmp6, %x71
  %x81 = load i32, i32* %x8, align 4
  %addtmp8 = add i32 %addtmp7, %x81
  %x91 = load i32, i32* %x9, align 4
  %addtmp9 = add i32 %addtmp8, %x91
  %x101 = load i32, i32* %x10, align 4
  %addtmp10 = add i32 %addtmp9, %x101
  %x11111 = load i32, i32* %x11, align 4
  %addtmp12 = add i32 %addtmp10, %x11111
  %x121 = load i32, i32* %x12, align 4
  %addtmp13 = add i32 %addtmp12, %x121
  %x131 = load i32, i32* %x13, align 4
  %addtmp14 = add i32 %addtmp13, %x131
  %x141 = load i32, i32* %x14, align 4
  %addtmp15 = add i32 %addtmp14, %x141
  %x151 = load i32, i32* %x15, align 4
  %addtmp16 = add i32 %addtmp15, %x151
  %x161 = load i32, i32* %x16, align 4
  %addtmp17 = add i32 %addtmp16, %x161
  %x171 = load i32, i32* %x17, align 4
  %addtmp18 = add i32 %addtmp17, %x171
  %x181 = load i32, i32* %x18, align 4
  %addtmp19 = add i32 %addtmp18, %x181
  %x191 = load i32, i32* %x19, align 4
  %addtmp20 = add i32 %addtmp19, %x191
  %x201 = load i32, i32* %x20, align 4
  %addtmp21 = add i32 %addtmp20, %x201
  %x211 = load i32, i32* %x21, align 4
  %addtmp22 = add i32 %addtmp21, %x211
  %x221 = load i32, i32* %x22, align 4
  %addtmp23 = add i32 %addtmp22, %x221
  %x231 = load i32, i32* %x23, align 4
  %addtmp24 = add i32 %addtmp23, %x231
  %x241 = load i32, i32* %x24, align 4
  %addtmp25 = add i32 %addtmp24, %x241
  %x251 = load i32, i32* %x25, align 4
  %addtmp26 = add i32 %addtmp25, %x251
  ret i32 %addtmp26
}
