.data

.text
	.globl main
main:
	addi sp, sp, 0
mainEntry:

	li t0, 6
	mv a0, t0


	li t0, 0
	mv a1, t0


	li t0, 0
	mv a2, t0

	j whilecond

whilecond:
	mv a3, a2

	mv a4, a0

	slt a5, a3, a4

	andi a6, a5, 1

	li t1, 0
	xor a3, a6, t1
	snez a3, a3

	beqz a3, whileend
	j whilebody

whilebody:

	mv a3, a2

	mv a4, a3

	j whilecond3

whileend:
	mv a3, a1

	mv a0, a3
	addi sp, sp, 4
	li a7, 93
	ecall

whilecond3:
	mv a5, a4

	mv a3, a0

	slt a6, a5, a3

	andi a7, a6, 1

	li t1, 0
	xor a3, a7, t1
	snez a3, a3

	beqz a3, whileend5
	j whilebody4

whilebody4:
	mv a5, a4

	mv a3, a0

	li t1, 1
	sub a6, a3, t1

	xor a0, a5, a6
	seqz a0, a0

	andi a3, a0, 1

	li t1, 0
	xor a5, a3, t1
	snez a5, a5

	beqz a5, ifcont
	j then

whileend5:
	mv a0, a2

	li t1, 1
	add a3, a0, t1

	mv a2, a3

	j whilecond

then:
	j whileend5

ifcont:
	mv a0, a4

	li t1, 2
	rem a2, a0, t1

	li t1, 0
	xor a3, a2, t1
	seqz a3, a3

	andi a0, a3, 1

	li t1, 0
	xor a2, a0, t1
	snez a2, a2

	beqz a2, ifcont17
	j then16

then16:
	mv a0, a4

	li t1, 1
	add a2, a0, t1

	mv a4, a2

	j whilecond3

ifcont17:
	mv a0, a1

	li t1, 1
	add a2, a0, t1

	mv a1, a2

	mv a0, a4

	li t1, 1
	add a1, a0, t1

	mv a4, a1

	j whilecond3

