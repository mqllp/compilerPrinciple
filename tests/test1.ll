.data

.text
	.globl main
main:
	addi sp, sp, 0
mainEntry:
	li a0, 8
	addi sp, sp, 4
	li a7, 93
	ecall

