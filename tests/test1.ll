.data
a:
	.word 1
b:
	.word 0

.text
	.globl main
main:
mainEntry:

	li t0, 3
	mv a0, t0

	mv a1, a0

	la t0, a
	lw a2, 0(t0)

	add a3, a1, a2

	li t1, 1
	add a4, a3, t1

	la t1, b
	sw a4, 0(t1)


	li t0, 10
	mv a1, t0

	la t0, a
	lw a2, 0(t0)

	la t0, b
	lw a3, 0(t0)

	add a4, a2, a3

	mv a5, a0

	add a2, a4, a5

	mv a0, a1

	add a3, a2, a0

	mv a0, a3
	addi sp, sp
	li a7, 93
	ecall

