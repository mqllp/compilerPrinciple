  .text
  .data
  .globl x
x:
  .word 1
  .globl y
y:
  .word 2
  .globl z
z:
  .word 3
  .globl a
a:
  .word 4
  .globl b
b:
  .word 5
  .globl c
c:
  .word 6
  .globl d
d:
  .word 7
  .globl e
e:
  .word 8
  .globl f
f:
  .word 9
  .globl g
g:
  .word 10
  .globl h
h:
  .word 11
  .globl i
i:
  .word 12
  .globl j
j:
  .word 13
  .globl k
k:
  .word 14
  .globl l
l:
  .word 15
  .globl m
m:
  .word 16
  .globl n
n:
  .word 17
  .globl o
o:
  .word 18
  .globl p
p:
  .word 19
  .globl q
q:
  .word 20
  .text
  .globl main
main:
  addi sp, sp, 0
mainEntry:
  # 分配局部变量 x1
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 1
  li t0, 1
  sw t0, 0(x5)
  # 分配局部变量 x2
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 2
  li t0, 2
  sw t0, 0(x6)
  # 分配局部变量 x3
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 3
  li t0, 3
  sw t0, 0(x7)
  # 分配局部变量 x4
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 4
  li t0, 4
  sw t0, 0(x9)
  # 分配局部变量 x5
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 5
  li t0, 5
  sw t0, 0(x10)
  # 分配局部变量 x6
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 6
  li t0, 6
  sw t0, 0(x11)
  # 分配局部变量 x7
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 7
  li t0, 7
  sw t0, 0(x12)
  # 分配局部变量 x8
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 8
  li t0, 8
  sw t0, 0(x13)
  # 分配局部变量 x9
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 9
  li t0, 9
  sw t0, 0(x14)
  # 分配局部变量 x10
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 10
  li t0, 10
  sw t0, 0(x15)
  # 分配局部变量 x11
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 11
  li t0, 11
  sw t0, 0(x16)
  # 分配局部变量 x12
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 12
  li t0, 12
  sw t0, -36(sp)
  # 分配局部变量 x13
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 13
  li t0, 13
  sw t0, -32(sp)
  # 分配局部变量 x14
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 14
  li t0, 14
  sw t0, -4(sp)
  # 分配局部变量 x15
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 15
  li t0, 15
  sw t0, -8(sp)
  # 分配局部变量 x16
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 16
  li t0, 16
  sw t0, -12(sp)
  # 分配局部变量 x17
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 17
  li t0, 17
  sw t0, -16(sp)
  # 分配局部变量 x18
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 18
  li t0, 18
  sw t0, -20(sp)
  # 分配局部变量 x19
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 19
  li t0, 19
  sw t0, -24(sp)
  # 分配局部变量 x20
  addi sp, sp, -4
  sw zero, 0(sp)
  # 存储常量 20
  li t0, 20
  sw t0, -28(sp)
  la t3, x
  lw x18, 0(t3)
  li x7, 1
  add x17, x18, x7
  la t1, x
  sw x17, 0(t1)
  la t3, y
  lw x18, 0(t3)
  li x7, 2
  add x17, x18, x7
  la t1, y
  sw x17, 0(t1)
  la t3, z
  lw x18, 0(t3)
  li x7, 3
  add x17, x18, x7
  la t1, z
  sw x17, 0(t1)
  la t3, a
  lw x18, 0(t3)
  li x7, 4
  add x17, x18, x7
  la t1, a
  sw x17, 0(t1)
  la t3, b
  lw x18, 0(t3)
  li x7, 5
  add x17, x18, x7
  la t1, b
  sw x17, 0(t1)
  la t3, c
  lw x18, 0(t3)
  li x7, 6
  add x17, x18, x7
  la t1, c
  sw x17, 0(t1)
  la t3, d
  lw x18, 0(t3)
  li x7, 7
  add x17, x18, x7
  la t1, d
  sw x17, 0(t1)
  la t3, e
  lw x18, 0(t3)
  li x7, 8
  add x17, x18, x7
  la t1, e
  sw x17, 0(t1)
  la t3, f
  lw x18, 0(t3)
  li x7, 9
  add x17, x18, x7
  la t1, f
  sw x17, 0(t1)
  la t3, g
  lw x18, 0(t3)
  li x7, 10
  add x17, x18, x7
  la t1, g
  sw x17, 0(t1)
  la t3, h
  lw x18, 0(t3)
  li x7, 11
  add x17, x18, x7
  la t1, h
  sw x17, 0(t1)
  la t3, i
  lw x18, 0(t3)
  li x7, 12
  add x17, x18, x7
  la t1, i
  sw x17, 0(t1)
  la t3, j
  lw x18, 0(t3)
  li x7, 13
  add x17, x18, x7
  la t1, j
  sw x17, 0(t1)
  la t3, k
  lw x18, 0(t3)
  li x7, 14
  add x17, x18, x7
  la t1, k
  sw x17, 0(t1)
  la t3, l
  lw x18, 0(t3)
  li x7, 15
  add x17, x18, x7
  la t1, l
  sw x17, 0(t1)
  la t3, m
  lw x18, 0(t3)
  li x7, 16
  add x17, x18, x7
  la t1, m
  sw x17, 0(t1)
  la t3, n
  lw x18, 0(t3)
  li x7, 17
  add x17, x18, x7
  la t1, n
  sw x17, 0(t1)
  la t3, o
  lw x18, 0(t3)
  li x7, 18
  add x17, x18, x7
  la t1, o
  sw x17, 0(t1)
  la t3, p
  lw x18, 0(t3)
  li x7, 19
  add x17, x18, x7
  la t1, p
  sw x17, 0(t1)
  la t3, q
  lw x18, 0(t3)
  li x7, 20
  add x17, x18, x7
  la t1, q
  sw x17, 0(t1)
  mv x18, x5
  li x7, 2
  mul x17, x18, x7
  sw x17, 0(x5)
  mv x18, x6
  li x7, 2
  mul x17, x18, x7
  sw x17, 0(x6)
  mv x18, x7
  li x7, 2
  mul x17, x18, x7
  sw x17, 0(x7)
  mv x18, x9
  li x7, 2
  mul x17, x18, x7
  sw x17, 0(x9)
  mv x18, x10
  li x7, 2
  mul x17, x18, x7
  sw x17, 0(x10)
  mv x18, x11
  li x7, 2
  mul x17, x18, x7
  sw x17, 0(x11)
  mv x18, x12
  li x7, 2
  mul x17, x18, x7
  sw x17, 0(x12)
  mv x18, x13
  li x7, 2
  mul x17, x18, x7
  sw x17, 0(x13)
  mv x18, x14
  li x7, 2
  mul x17, x18, x7
  sw x17, 0(x14)
  mv x18, x15
  li x7, 2
  mul x17, x18, x7
  sw x17, 0(x15)
  mv x18, x16
  li x7, 2
  mul x17, x18, x7
  sw x17, 0(x16)
  lw x18, -36(sp)
  li x7, 2
  mul x17, x18, x7
  sw x17, -36(sp)
  lw x18, -32(sp)
  li x7, 2
  mul x17, x18, x7
  sw x17, -32(sp)
  lw x18, -4(sp)
  li x7, 2
  mul x17, x18, x7
  sw x17, -4(sp)
  lw x18, -8(sp)
  li x7, 2
  mul x17, x18, x7
  sw x17, -8(sp)
  lw x18, -12(sp)
  li x7, 2
  mul x17, x18, x7
  sw x17, -12(sp)
  lw x18, -16(sp)
  li x7, 2
  mul x17, x18, x7
  sw x17, -16(sp)
  lw x18, -20(sp)
  li x7, 2
  mul x17, x18, x7
  sw x17, -20(sp)
  lw x18, -24(sp)
  li x7, 2
  mul x17, x18, x7
  sw x17, -24(sp)
  lw x18, -28(sp)
  li x7, 2
  mul x17, x18, x7
  sw x17, -28(sp)
  mv x18, x5
  mv x17, x6
  add x5, x18, x17
  mv x6, x7
  add x17, x5, x6
  mv x7, x9
  add x5, x17, x7
  mv x6, x10
  add x7, x5, x6
  mv x9, x11
  add x5, x7, x9
  mv x6, x12
  add x7, x5, x6
  mv x9, x13
  add x5, x7, x9
  mv x6, x14
  add x7, x5, x6
  mv x9, x15
  add x5, x7, x9
  mv x6, x16
  add x7, x5, x6
  lw x9, -36(sp)
  add x5, x7, x9
  lw x6, -32(sp)
  add x7, x5, x6
  lw x9, -4(sp)
  add x5, x7, x9
  lw x6, -8(sp)
  add x7, x5, x6
  lw x9, -12(sp)
  add x5, x7, x9
  lw x6, -16(sp)
  add x7, x5, x6
  lw x9, -20(sp)
  add x5, x7, x9
  lw x6, -24(sp)
  add x7, x5, x6
  lw x9, -28(sp)
  add x5, x7, x9
  la t3, x
  lw x6, 0(t3)
  add x7, x5, x6
  la t3, y
  lw x9, 0(t3)
  add x5, x7, x9
  la t3, z
  lw x6, 0(t3)
  add x7, x5, x6
  la t3, a
  lw x9, 0(t3)
  add x5, x7, x9
  la t3, b
  lw x6, 0(t3)
  add x7, x5, x6
  la t3, c
  lw x9, 0(t3)
  add x5, x7, x9
  la t3, d
  lw x6, 0(t3)
  add x7, x5, x6
  la t3, e
  lw x9, 0(t3)
  add x5, x7, x9
  la t3, f
  lw x6, 0(t3)
  add x7, x5, x6
  la t3, g
  lw x9, 0(t3)
  add x5, x7, x9
  la t3, h
  lw x6, 0(t3)
  add x7, x5, x6
  la t3, i
  lw x9, 0(t3)
  add x5, x7, x9
  la t3, j
  lw x6, 0(t3)
  add x7, x5, x6
  la t3, k
  lw x9, 0(t3)
  add x5, x7, x9
  la t3, l
  lw x6, 0(t3)
  add x7, x5, x6
  la t3, m
  lw x9, 0(t3)
  add x5, x7, x9
  la t3, n
  lw x6, 0(t3)
  add x7, x5, x6
  la t3, o
  lw x9, 0(t3)
  add x5, x7, x9
  la t3, p
  lw x6, 0(t3)
  add x7, x5, x6
  la t3, q
  lw x9, 0(t3)
  add x5, x7, x9
  mv a0, x5
  addi sp, sp, 36
  li a7, 93
  ecall
