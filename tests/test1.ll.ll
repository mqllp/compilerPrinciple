; ModuleID = 'module'
source_filename = "module"

@x = global i32 1
@y = global i32 2
@z = global i32 3
@a = global i32 4
@b = global i32 5
@c = global i32 6
@d = global i32 7
@e = global i32 8
@f = global i32 9
@g = global i32 10
@h = global i32 11
@i = global i32 12
@j = global i32 13
@k = global i32 14
@l = global i32 15
@m = global i32 16
@n = global i32 17
@o = global i32 18
@p = global i32 19
@q = global i32 20

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
  %x110 = load i32, i32* @x, align 4
  %addtmp = add i32 %x110, 1
  store i32 %addtmp, i32* @x, align 4
  %y1 = load i32, i32* @y, align 4
  %addtmp11 = add i32 %y1, 2
  store i32 %addtmp11, i32* @y, align 4
  %z1 = load i32, i32* @z, align 4
  %addtmp12 = add i32 %z1, 3
  store i32 %addtmp12, i32* @z, align 4
  %a1 = load i32, i32* @a, align 4
  %addtmp13 = add i32 %a1, 4
  store i32 %addtmp13, i32* @a, align 4
  %b1 = load i32, i32* @b, align 4
  %addtmp14 = add i32 %b1, 5
  store i32 %addtmp14, i32* @b, align 4
  %c1 = load i32, i32* @c, align 4
  %addtmp15 = add i32 %c1, 6
  store i32 %addtmp15, i32* @c, align 4
  %d1 = load i32, i32* @d, align 4
  %addtmp16 = add i32 %d1, 7
  store i32 %addtmp16, i32* @d, align 4
  %e1 = load i32, i32* @e, align 4
  %addtmp17 = add i32 %e1, 8
  store i32 %addtmp17, i32* @e, align 4
  %f1 = load i32, i32* @f, align 4
  %addtmp18 = add i32 %f1, 9
  store i32 %addtmp18, i32* @f, align 4
  %g1 = load i32, i32* @g, align 4
  %addtmp19 = add i32 %g1, 10
  store i32 %addtmp19, i32* @g, align 4
  %h1 = load i32, i32* @h, align 4
  %addtmp20 = add i32 %h1, 11
  store i32 %addtmp20, i32* @h, align 4
  %i1 = load i32, i32* @i, align 4
  %addtmp21 = add i32 %i1, 12
  store i32 %addtmp21, i32* @i, align 4
  %j1 = load i32, i32* @j, align 4
  %addtmp22 = add i32 %j1, 13
  store i32 %addtmp22, i32* @j, align 4
  %k1 = load i32, i32* @k, align 4
  %addtmp23 = add i32 %k1, 14
  store i32 %addtmp23, i32* @k, align 4
  %l1 = load i32, i32* @l, align 4
  %addtmp24 = add i32 %l1, 15
  store i32 %addtmp24, i32* @l, align 4
  %m1 = load i32, i32* @m, align 4
  %addtmp25 = add i32 %m1, 16
  store i32 %addtmp25, i32* @m, align 4
  %n1 = load i32, i32* @n, align 4
  %addtmp26 = add i32 %n1, 17
  store i32 %addtmp26, i32* @n, align 4
  %o1 = load i32, i32* @o, align 4
  %addtmp27 = add i32 %o1, 18
  store i32 %addtmp27, i32* @o, align 4
  %p1 = load i32, i32* @p, align 4
  %addtmp28 = add i32 %p1, 19
  store i32 %addtmp28, i32* @p, align 4
  %q1 = load i32, i32* @q, align 4
  %addtmp29 = add i32 %q1, 20
  store i32 %addtmp29, i32* @q, align 4
  %x1130 = load i32, i32* %x1, align 4
  %multmp = mul i32 %x1130, 2
  store i32 %multmp, i32* %x1, align 4
  %x21 = load i32, i32* %x2, align 4
  %multmp31 = mul i32 %x21, 2
  store i32 %multmp31, i32* %x2, align 4
  %x31 = load i32, i32* %x3, align 4
  %multmp32 = mul i32 %x31, 2
  store i32 %multmp32, i32* %x3, align 4
  %x41 = load i32, i32* %x4, align 4
  %multmp33 = mul i32 %x41, 2
  store i32 %multmp33, i32* %x4, align 4
  %x51 = load i32, i32* %x5, align 4
  %multmp34 = mul i32 %x51, 2
  store i32 %multmp34, i32* %x5, align 4
  %x61 = load i32, i32* %x6, align 4
  %multmp35 = mul i32 %x61, 2
  store i32 %multmp35, i32* %x6, align 4
  %x71 = load i32, i32* %x7, align 4
  %multmp36 = mul i32 %x71, 2
  store i32 %multmp36, i32* %x7, align 4
  %x81 = load i32, i32* %x8, align 4
  %multmp37 = mul i32 %x81, 2
  store i32 %multmp37, i32* %x8, align 4
  %x91 = load i32, i32* %x9, align 4
  %multmp38 = mul i32 %x91, 2
  store i32 %multmp38, i32* %x9, align 4
  %x101 = load i32, i32* %x10, align 4
  %multmp39 = mul i32 %x101, 2
  store i32 %multmp39, i32* %x10, align 4
  %x111 = load i32, i32* %x11, align 4
  %multmp40 = mul i32 %x111, 2
  store i32 %multmp40, i32* %x11, align 4
  %x121 = load i32, i32* %x12, align 4
  %multmp41 = mul i32 %x121, 2
  store i32 %multmp41, i32* %x12, align 4
  %x131 = load i32, i32* %x13, align 4
  %multmp42 = mul i32 %x131, 2
  store i32 %multmp42, i32* %x13, align 4
  %x141 = load i32, i32* %x14, align 4
  %multmp43 = mul i32 %x141, 2
  store i32 %multmp43, i32* %x14, align 4
  %x151 = load i32, i32* %x15, align 4
  %multmp44 = mul i32 %x151, 2
  store i32 %multmp44, i32* %x15, align 4
  %x161 = load i32, i32* %x16, align 4
  %multmp45 = mul i32 %x161, 2
  store i32 %multmp45, i32* %x16, align 4
  %x171 = load i32, i32* %x17, align 4
  %multmp46 = mul i32 %x171, 2
  store i32 %multmp46, i32* %x17, align 4
  %x181 = load i32, i32* %x18, align 4
  %multmp47 = mul i32 %x181, 2
  store i32 %multmp47, i32* %x18, align 4
  %x191 = load i32, i32* %x19, align 4
  %multmp48 = mul i32 %x191, 2
  store i32 %multmp48, i32* %x19, align 4
  %x201 = load i32, i32* %x20, align 4
  %multmp49 = mul i32 %x201, 2
  store i32 %multmp49, i32* %x20, align 4
  %x1150 = load i32, i32* %x1, align 4
  %x2151 = load i32, i32* %x2, align 4
  %addtmp52 = add i32 %x1150, %x2151
  %x3153 = load i32, i32* %x3, align 4
  %addtmp54 = add i32 %addtmp52, %x3153
  %x4155 = load i32, i32* %x4, align 4
  %addtmp56 = add i32 %addtmp54, %x4155
  %x5157 = load i32, i32* %x5, align 4
  %addtmp58 = add i32 %addtmp56, %x5157
  %x6159 = load i32, i32* %x6, align 4
  %addtmp60 = add i32 %addtmp58, %x6159
  %x7161 = load i32, i32* %x7, align 4
  %addtmp62 = add i32 %addtmp60, %x7161
  %x8163 = load i32, i32* %x8, align 4
  %addtmp64 = add i32 %addtmp62, %x8163
  %x9165 = load i32, i32* %x9, align 4
  %addtmp66 = add i32 %addtmp64, %x9165
  %x10167 = load i32, i32* %x10, align 4
  %addtmp68 = add i32 %addtmp66, %x10167
  %x11169 = load i32, i32* %x11, align 4
  %addtmp70 = add i32 %addtmp68, %x11169
  %x12171 = load i32, i32* %x12, align 4
  %addtmp72 = add i32 %addtmp70, %x12171
  %x13173 = load i32, i32* %x13, align 4
  %addtmp74 = add i32 %addtmp72, %x13173
  %x14175 = load i32, i32* %x14, align 4
  %addtmp76 = add i32 %addtmp74, %x14175
  %x15177 = load i32, i32* %x15, align 4
  %addtmp78 = add i32 %addtmp76, %x15177
  %x16179 = load i32, i32* %x16, align 4
  %addtmp80 = add i32 %addtmp78, %x16179
  %x17181 = load i32, i32* %x17, align 4
  %addtmp82 = add i32 %addtmp80, %x17181
  %x18183 = load i32, i32* %x18, align 4
  %addtmp84 = add i32 %addtmp82, %x18183
  %x19185 = load i32, i32* %x19, align 4
  %addtmp86 = add i32 %addtmp84, %x19185
  %x20187 = load i32, i32* %x20, align 4
  %addtmp88 = add i32 %addtmp86, %x20187
  %x189 = load i32, i32* @x, align 4
  %addtmp90 = add i32 %addtmp88, %x189
  %y191 = load i32, i32* @y, align 4
  %addtmp92 = add i32 %addtmp90, %y191
  %z193 = load i32, i32* @z, align 4
  %addtmp94 = add i32 %addtmp92, %z193
  %a195 = load i32, i32* @a, align 4
  %addtmp96 = add i32 %addtmp94, %a195
  %b197 = load i32, i32* @b, align 4
  %addtmp98 = add i32 %addtmp96, %b197
  %c199 = load i32, i32* @c, align 4
  %addtmp100 = add i32 %addtmp98, %c199
  %d1101 = load i32, i32* @d, align 4
  %addtmp102 = add i32 %addtmp100, %d1101
  %e1103 = load i32, i32* @e, align 4
  %addtmp104 = add i32 %addtmp102, %e1103
  %f1105 = load i32, i32* @f, align 4
  %addtmp106 = add i32 %addtmp104, %f1105
  %g1107 = load i32, i32* @g, align 4
  %addtmp108 = add i32 %addtmp106, %g1107
  %h1109 = load i32, i32* @h, align 4
  %addtmp110 = add i32 %addtmp108, %h1109
  %i1111 = load i32, i32* @i, align 4
  %addtmp112 = add i32 %addtmp110, %i1111
  %j1113 = load i32, i32* @j, align 4
  %addtmp114 = add i32 %addtmp112, %j1113
  %k1115 = load i32, i32* @k, align 4
  %addtmp116 = add i32 %addtmp114, %k1115
  %l1117 = load i32, i32* @l, align 4
  %addtmp118 = add i32 %addtmp116, %l1117
  %m1119 = load i32, i32* @m, align 4
  %addtmp120 = add i32 %addtmp118, %m1119
  %n1121 = load i32, i32* @n, align 4
  %addtmp122 = add i32 %addtmp120, %n1121
  %o1123 = load i32, i32* @o, align 4
  %addtmp124 = add i32 %addtmp122, %o1123
  %p1125 = load i32, i32* @p, align 4
  %addtmp126 = add i32 %addtmp124, %p1125
  %q1127 = load i32, i32* @q, align 4
  %addtmp128 = add i32 %addtmp126, %q1127
  ret i32 %addtmp128
}
