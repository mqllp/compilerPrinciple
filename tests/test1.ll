.data

.text
	.globl main
main:
	addi sp, sp, 0
mainEntry:

	li t0, 666
	mv a0, t0

	mv a1, a0

	li t1, 555
	slt a2, t1, a1

	andi a3, a2, 1

	li t1, 0
	xor a1, a3, t1
	snez a1, a1

	beqz a1, else
	j then

then:
	mv a2, a0

	mv a0, a2
	addi sp, sp, 4
	li a7, 93
	ecall

else:
	li a0, 222
	addi sp, sp, 4
	li a7, 93
	ecall

ifcont:
	li a0, 0
	addi sp, sp, 4
	li a7, 93
	ecall

