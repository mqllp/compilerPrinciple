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

  lw a2, 4(sp)

  add a0, a1, a2

  li t1, 1
  add a3, a0, t1

  sw a3, 8(sp)

  lw a0, 4(sp)

  mv a0, a0
  addi sp, sp, 0
  li a7, 93
  ecall

