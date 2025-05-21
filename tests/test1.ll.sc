.data
global_var:
	.word 1

.text
	.globl main
main:
	addi sp, sp, 0
mainEntry:
	li a0, 2
	addi sp, sp, 4
	li a7, 93
	ecall

