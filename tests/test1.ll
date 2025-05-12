.data:
a:
  .word 1
b:
  .word 0

.text:
  .globl main
main:
  addi sp, sp, 0
mainEntry:

  li t0, 3
  sw t0, 0(sp)

  lw a1, 0(sp)

  la t0, a
  lw a2, 0(t0)

  add a3, a1, a2

  li t1, 1
  add a4, a3, t1

  la t1, b
  sw a4, 0(t1)


  li t0, 10
  sw t0, 4(sp)

  la t0, a
  lw a2, 0(t0)

  la t0, b
  lw a3, 0(t0)

  add a4, a2, a3

  lw a5, 0(sp)

  add a2, a4, a5

  lw a0, 4(sp)

  add a3, a2, a0

  mv a0, a3
  addi sp, sp, 0
  li a7, 93
  ecall

