.data
global_var:
	.word 1

.text
	.globl main
main:
	addi sp, sp, 0
mainEntry:

	li t0, 1
	mv a0, t0


	mv a2, a0

	mv a1, a2

	mv a3, a0

	mv a2, a0

	mul a4, a3, a2

	mv a0, a1

	add a2, a4, a0

	mv a0, a2
	addi sp, sp, 4
	li a7, 93
	ecall

