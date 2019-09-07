package com.pherrymason.nes

@ExperimentalUnsignedTypes
class RAM {
    val memory: MutableMap<Address, NesByte> = mutableMapOf()

    fun read(address: Address): NesByte {
        return memory.getOrDefault(address, NesByte(0x00))
    }

    fun read(address: Int): NesByte {
        return memory.getOrDefault(Address(address), NesByte(0x00))
    }

    fun write(address: Address, value: NesByte) {
        memory[address] = value
    }

    fun write(address: Int, value: Int) {
        memory[Address(address)] = NesByte(value)
    }
}