.data
global_var:
	.word 1

.text
	.globl main
main:
	addi sp, sp, 0
mainEntry:

	li t0, 6
	mv a0, t0


	li t0, 6
	mv a1, t0

	li a0, 42
	addi sp, sp, 4
	li a7, 93
	ecall

