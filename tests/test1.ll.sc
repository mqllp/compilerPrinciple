.data

.text
	.globl main
main:
	addi sp, sp, 0
mainEntry:

	li t0, 1
	mv a0, t0

	j whilecond

whilecond:
	mv a1, a0

	li t1, 10
	slt a2, a1, t1

	andi a3, a2, 1

	li t1, 0
	xor a1, a3, t1
	snez a1, a1

	beqz a1, whileend
	j whilebody

whilebody:
	mv a2, a0

	li t1, 1
	add a1, a2, t1

	mv a0, a1

	j whilecond

whileend:

	mv a2, a0

	mv a1, a2


	li t0, 2
	mv a0, t0


	mv a3, a0

	mv a2, a3

	mv a0, a1

	mv a0, a0
	addi sp, sp, 4
	li a7, 93
	ecall

