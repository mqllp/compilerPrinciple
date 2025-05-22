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


	li t0, 14
	mv a1, t0

	mv a2, a0

	li t1, 1
	xor a1, a2, t1
	seqz a1, a1

	andi a3, a1, 1

	li t1, 0
	xor a2, a3, t1
	snez a2, a2

	beqz a2, else
	j then

then:
	li t0, 8
	mv a0, t0

	j ifcont

else:
	li t0, 0
	mv a0, t0

	j ifcont

ifcont:
	mv a1, a0

	mv a2, a0

	mul a3, a1, a2

	li t1, 14
	add a0, a3, t1

	mv a0, a0
	addi sp, sp, 4
	li a7, 93
	ecall

