  .data
c:
  .word 0

  .text
  .globl main
main:
  addi sp, sp, -0
mainEntry:
  li a0, 666
  addi sp, sp, 0
  li a7, 93
  ecall
