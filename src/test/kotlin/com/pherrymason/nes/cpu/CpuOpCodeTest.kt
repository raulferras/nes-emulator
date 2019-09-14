package com.pherrymason.nes.cpu

import com.pherrymason.nes.Address
import com.pherrymason.nes.RAM
import com.pherrymason.nes.NesByte
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@ExperimentalUnsignedTypes
class CpuOpCodeTest {
    var ram = RAM()
    var cpu = CPU6502(ram)

    @Test
    fun ANDTest() {
        val instruction = InstructionDescription.fromInstructionCode(InstructionCode.AND, AddressingMode.Immediate)
        ram.write(Address(0), instruction.opcode)

        // --------------------------------------------------
        // Case 1
        ram.write(Address(1), NesByte(0x8F))
        cpu.registers.a = NesByte(0x8F)
        cpu.clock()

        assertEquals(cpu.registers.ps.zeroFlag, false)
        assertEquals(cpu.registers.ps.negativeFlag, true)
        assertEquals(cpu.registers.a.byte, NesByte(0x8F).byte, "acumulator failed")

        // --------------------------------------------------
        // Case 2: Zero flag properly set
        cpu.reset()
        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte(0x8F))
        cpu.registers.a = NesByte(0x70)
        cpu.clock()

        assertEquals(cpu.registers.ps.zeroFlag, true)
        assertEquals(cpu.registers.ps.negativeFlag, false)
        assertEquals(cpu.registers.a.byte, NesByte(0x00).byte)

        // --------------------------------------------------
        // Case 3: Negative flag properly set
        cpu.reset()
        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte(0x8F))
        cpu.registers.a = NesByte(0x8F)
        cpu.clock()

        assertEquals(cpu.registers.ps.zeroFlag, false)
        assertEquals(cpu.registers.ps.negativeFlag, true)
        assertEquals(cpu.registers.a.byte, NesByte(0x8F).byte)
    }

    @Test
    fun BRKTest() {
        val instruction = InstructionDescription.fromInstructionCode(InstructionCode.BRK, AddressingMode.Implied)

        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte(0x00))
        // Setup an address in vector 0xFFFE-0xFFFF
        ram.write(Address(0xFFFE), NesByte(0x03))
        ram.write(Address(0xFFFF), NesByte(0xFF))


        cpu.clock()
        assertEquals(cpu.registers.pc, Address(0xFF03), "PC does not point correctly")

        // The stack should contain the value of PC
        val loByte = ram.read(ram.STACK_ADDRESS + 0xFF)
        val hiByte = ram.read(ram.STACK_ADDRESS + 0xFE)

        assertEquals(Address(loByte, hiByte), Address(0x02), "Stack does not contain expected value")

        // The stack should contain the value of Processor Status
        val psDump = ram.read(ram.STACK_ADDRESS + 0xFD)
        val expectedDump = NesByte(0x14)
        assertEquals(expectedDump, psDump, "Processor status was not copied to the Stack")

        assertEquals(true, cpu.registers.ps.interruptDisabled, "Interrupt flag is not set")
        assertEquals(false, cpu.registers.ps.breakCommand, "Break flag is not set")
    }

    @Test
    fun BCCTest() {
        // If the carry flag is clear then add the relative displacement to the
        // program counter to cause a branch to a new location
        // This scenario should jump to 0x010 if carry flag.
        // else it should jump to 0x02
        val instruction = InstructionDescription.fromInstructionCode(InstructionCode.BCC, AddressingMode
            .Relative)

        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte( 8 + 128))

        // Scenario 1: No carry flag
        cpu.registers.ps.carryBit = true
        cpu.clock()
        assertEquals(Address(0x02), cpu.registers.pc)

        // Scenario 2: Carry flag is set
        cpu.reset()
        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte( 8 + 128))
        cpu.clock()
        assertEquals(Address(10), cpu.registers.pc)

        // Scenario 3: TODO Memory page trespassed causes +1 cycle
    }

    @Test
    fun BCSTest() {
        // If the carry flag is set then add the relative displacement to the
        // program counter to cause a branch to a new location
        // This scenario should jump to 0x010 if carry flag.
        // else it should jump to 0x02
        val instruction = InstructionDescription.fromInstructionCode(InstructionCode.BCS, AddressingMode
            .Relative)

        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte( 8 + 128))

        // Scenario 1: No carry flag
        cpu.clock()
        assertEquals(Address(0x02), cpu.registers.pc)

        // Scenario 2: Carry flag is set
        cpu.reset()
        cpu.registers.ps.carryBit = true
        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte( 8 + 128))
        cpu.clock()
        assertEquals(Address(10), cpu.registers.pc)

        // Scenario 3: TODO Memory page trespassed causes +1 cycle
    }

    @Test
    fun BEQTest() {
        // If the zero flag is set then add the relative displacement to the program counter
        // to cause a branch to a new location
        val instruction = InstructionDescription.fromInstructionCode(InstructionCode.BEQ, AddressingMode.Relative)

        // Scenario 1: Zero flag is set
        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte(8 + 128))
        cpu.registers.ps.zeroFlag = true
        cpu.clock()
        assertEquals(Address(10), cpu.registers.pc)

        // Scenario 2: Zero flag is not set
        cpu.reset()
        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte(8 + 128))
        cpu.clock()
        assertEquals(Address(2), cpu.registers.pc)
    }

    @Test
    fun BNETest() {
        // If the zero flag is clear then add the relative displacement to the program
        // counter to cause a branch to a new location.
        val instruction = InstructionDescription.fromInstructionCode(InstructionCode.BNE, AddressingMode.Relative)

        // Scenario 1: Zero flag is clear
        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte(8 + 128))
        cpu.registers.ps.zeroFlag = false
        cpu.clock()
        assertEquals(Address(10), cpu.registers.pc)

        // Scenario 2: Zero flag is set
        cpu.reset()
        cpu.registers.ps.zeroFlag = true
        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte(8 + 128))
        cpu.clock()
        assertEquals(Address(2), cpu.registers.pc)
    }

    @Test
    fun BPLTest() {
        // If the negative flag is clear then add the relative displacement to the
        // program counter to cause a branch to a new location.
        val instruction = InstructionDescription.fromInstructionCode(InstructionCode.BPL, AddressingMode.Relative)

        // Scenario 1: Zero flag is clear
        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte(8 + 128))
        cpu.registers.ps.negativeFlag = false
        cpu.clock()
        assertEquals(Address(10), cpu.registers.pc)

        // Scenario 2: Zero flag is set
        cpu.reset()
        cpu.registers.ps.negativeFlag = true
        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte(8 + 128))
        cpu.clock()
        assertEquals(Address(2), cpu.registers.pc)
    }

    @Test
    fun BMITest() {
        // If the negative flag is set then add the relative displacement to the program counter to cause a branch to a
        // new location.
        val instruction = InstructionDescription.fromInstructionCode(InstructionCode.BMI, AddressingMode.Relative)

        // Scenario 1: Zero flag is set
        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte(8 + 128))
        cpu.registers.ps.negativeFlag = true
        cpu.clock()
        assertEquals(Address(10), cpu.registers.pc)

        // Scenario 2: Zero flag is not set
        cpu.reset()
        cpu.registers.ps.negativeFlag = false
        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte(8 + 128))
        cpu.clock()
        assertEquals(Address(2), cpu.registers.pc)
    }

    @Test
    fun BVCTest() {
        // If the negative flag is set then add the relative displacement to the program counter to cause a branch to a
        // new location.
        val instruction = InstructionDescription.fromInstructionCode(InstructionCode.BVC, AddressingMode.Relative)

        // Scenario 1: Zero flag is set
        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte(8 + 128))
        cpu.registers.ps.overflowFlag = false
        cpu.clock()
        assertEquals(Address(10), cpu.registers.pc)

        // Scenario 2: Zero flag is not set
        cpu.reset()
        cpu.registers.ps.overflowFlag = true
        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte(8 + 128))
        cpu.clock()
        assertEquals(Address(2), cpu.registers.pc)
    }

    @Test
    fun BVSTest() {
        // If the negative flag is set then add the relative displacement to the program counter to cause a branch to a
        // new location.
        val instruction = InstructionDescription.fromInstructionCode(InstructionCode.BVS, AddressingMode.Relative)

        // Scenario 1: Zero flag is set
        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte(8 + 128))
        cpu.registers.ps.overflowFlag = true
        cpu.clock()
        assertEquals(Address(10), cpu.registers.pc)

        // Scenario 2: Zero flag is not set
        cpu.reset()
        cpu.registers.ps.overflowFlag = false
        ram.write(Address(0), instruction.opcode)
        ram.write(Address(1), NesByte(8 + 128))
        cpu.clock()
        assertEquals(Address(2), cpu.registers.pc)
    }

    @Test
    fun CLCTest() {
        // Set the carry flag to zero.
        val instruction = InstructionDescription.fromInstructionCode(InstructionCode.CLC, AddressingMode.Implied)

        ram.write(Address(0), instruction.opcode)
        cpu.registers.ps.carryBit = true
        cpu.clock()

        assertEquals(false, cpu.registers.ps.carryBit)
    }
}