.data

.text
	.globl main
main:
	addi sp, sp, -16
mainEntry:

	li t0, 1
	mv a0, t0


	li t0, 2
	mv a1, t0


	li t0, 3
	mv a2, t0


	li t0, 4
	mv a3, t0


	li t0, 5
	mv a4, t0


	li t0, 6
	mv a5, t0


	li t0, 7
	mv a6, t0


	li t0, 8
	mv a7, t0


	li t0, 9
	mv s0, t0


	li t0, 10
	mv s1, t0


	li t0, 11
	mv s2, t0


	li t0, 12
	mv s3, t0


	li t0, 13
	mv s4, t0


	li t0, 14
	mv s5, t0


	li t0, 15
	mv s6, t0


	li t0, 16
	mv s7, t0


	li t0, 17
	mv s8, t0


	li t0, 18
	mv s9, t0


	li t0, 19
	mv s10, t0


	li t0, 20
	mv s11, t0


	li t0, 21
	mv t2, t0


	li t0, 22
	mv t3, t0


	li t0, 23
	mv t4, t0


	li t0, 24
	mv t5, t0


	li t0, 25
	mv t6, t0

	sw a0, 0(sp)
# Spill x1 to stack offset 0
	sw a1, 4(sp)
# Spill x2 to stack offset 4
	lw a1, 0(sp)
# Reload x1 from stack offset 0
	mv a0, a1

	sw a2, 8(sp)
# Spill x3 to stack offset 8
	sw a3, 12(sp)
# Spill x4 to stack offset 12
	lw a3, 4(sp)
# Reload x2 from stack offset 4
	mv a2, a3

	add a1, a0, a2

	sw a4, 0(sp)
# Spill x5 to stack offset 0
	lw a4, 8(sp)
# Reload x3 from stack offset 8
	mv a3, a4

	add a0, a1, a3

	lw a4, 12(sp)
# Reload x4 from stack offset 12
	mv a2, a4

	add a1, a0, a2

	lw a4, 0(sp)
# Reload x5 from stack offset 0
	mv a3, a4

	add a0, a1, a3

	mv a2, a5

	add a1, a0, a2

	mv a3, a6

	add a0, a1, a3

	mv a2, a7

	add a1, a0, a2

	mv a3, s0

	add a0, a1, a3

	mv a2, s1

	add a1, a0, a2

	mv a3, s2

	add a0, a1, a3

	mv a2, s3

	add a1, a0, a2

	mv a3, s4

	add a0, a1, a3

	mv a2, s5

	add a1, a0, a2

	mv a3, s6

	add a0, a1, a3

	mv a2, s7

	add a1, a0, a2

	mv a3, s8

	add a0, a1, a3

	mv a2, s9

	add a1, a0, a2

	mv a3, s10

	add a0, a1, a3

	mv a2, s11

	add a1, a0, a2

	mv a3, t2

	add a0, a1, a3

	mv a2, t3

	add a1, a0, a2

	mv a3, t4

	add a0, a1, a3

	mv a2, t5

	add a1, a0, a2

	mv a3, t6

	add a0, a1, a3

	mv a0, a0
	addi sp, sp, 4
	li a7, 93
	ecall

