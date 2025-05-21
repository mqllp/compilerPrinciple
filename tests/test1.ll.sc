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
	li t0, -1
	beqz t0, whileend
	j whilebody

whilebody:
	mv a1, a0

	li t1, 1
	add a2, a1, t1

	mv a0, a2

	mv a1, a0

	li t1, 100
	slt a2, t1, a1

	andi a3, a2, 1

	li t1, 0
	xor a1, a3, t1
	snez a1, a1

	beqz a1, ifcont
	j then

whileend:
	mv a2, a0

	mv a0, a2
	addi sp, sp, 4
	li a7, 93
	ecall

then:
	j whileend

ifcont:
	j whilecond

