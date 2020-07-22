#!/usr/bin/env kscript

import java.io.File

@file:Include("utils.kts")

val registers: HashMap<String, UInt> = HashMap()

fun File.decodeLayer6(resultFileName: String) {
    val memory = decodeAscii85()
    val output = ArrayList<UByte>()

    readCommands@ while (true) {
        val curIndex = getRegisterValue("pc").toInt()

        // 1. Reads one instruction from memory, at the address stored in the `pc` register.
        val command = Command.fromByte(memory[curIndex])
                ?: throw RuntimeException("No command for byte ${memory[curIndex].toString(16)} at position ${curIndex} found")

        // 2. Adds the byte size of the instruction to the `pc` register.
        registers["pc"] = curIndex.toUInt() + command.size.toUInt()

        // 3. Executes the instruction.
        when (command) {
            Command.ADD_A_X_B -> registers["a"] = (getRegisterValue("a") + getRegisterValue("b")) % 256u
            Command.APTR_IMM8 -> registers["ptr"] = (getRegisterValue("ptr")) + memory[curIndex + 1].toUInt()
            Command.CMP -> registers["f"] = if (getRegisterValue("a") == getRegisterValue("b")) 0u else 1u
            Command.HALT -> break@readCommands
            Command.JEZ_IMM32 -> {
                if (getRegisterValue("f") == 0u) {
                    registers["pc"] = toUInt(memory[curIndex + 1], memory[curIndex + 2], memory[curIndex + 3], memory[curIndex + 4])
                }
            }
            Command.JNZ_IMM32 -> {
                if (getRegisterValue("f") != 0u) {
                    registers["pc"] = toUInt(memory[curIndex + 1], memory[curIndex + 2], memory[curIndex + 3], memory[curIndex + 4])
                }
            }
            Command.MV_DEST_X_SRC -> {
                val binaryByte = memory[curIndex].toString(2).padStart(8, '0')
                val destination = getRegister(binaryByte.substring(2, 5).toInt(2))
                val source = getRegister(binaryByte.substring(5).toInt(2))
                if (destination.toIntOrNull() != null) {
                    memory[destination.toInt()] = getRegisterValue(source).toUByte()
                } else if (source.toIntOrNull() != null) {
                    registers[destination] = memory[source.toInt()]?.toUInt() ?: 0u
                } else {
                    registers[destination] = getRegisterValue(source)
                }
            }
            Command.MV32_DEST_X_SRC -> {
                val binaryByte = memory[curIndex].toString(2).padStart(8, '0')
                val destination = get32BitRegister(binaryByte.substring(2, 5).toInt(2))
                val source = get32BitRegister(binaryByte.substring(5).toInt(2))
                registers[destination] = getRegisterValue(source)
            }
            Command.MVI_DEST_X_IMM8 -> {
                val binaryByte = memory[curIndex].toString(2).padStart(8, '0')
                val destination = getRegister(binaryByte.substring(2, 5).toInt(2))
                if (destination.toIntOrNull() != null) {
                    memory[destination.toInt()] = memory[curIndex + 1]
                } else {
                    registers[destination] = memory[curIndex + 1]!!.toUInt()
                }
            }
            Command.MVI32_DEST_X_IMM32 -> {
                val binaryByte = memory[curIndex].toString(2).padStart(8, '0')
                val destination = get32BitRegister(binaryByte.substring(2, 5).toInt(2))
                registers[destination] = toUInt(memory[curIndex + 1], memory[curIndex + 2], memory[curIndex + 3], memory[curIndex + 4])
            }
            Command.OUT_A -> {
                output.add(getRegisterValue("a").toUByte())
            }
            Command.SUB_A_X_B -> {
                var result = getRegisterValue("a") - getRegisterValue("b")
                if (result < 0u) {
                    result += 255u
                }
                registers["a"] = result
            }
            Command.XOR_A_X_B -> {
                registers["a"] = getRegisterValue("a") xor getRegisterValue("b")
            }
        }

    }

    output
            .toText()
            .saveToFile(resultFileName)
}

fun toUInt(byte1: UByte, byte2: UByte, byte3: UByte, byte4: UByte): UInt {
    var value = 0

    value = value or (byte1.toInt() and 0x000000FF)
    value = value or (byte2.toInt() and 0x000000FF shl 8)
    value = value or (byte3.toInt() and 0x000000FF shl 16)
    value = value or (byte4.toInt() and 0x000000FF shl 24)

    return value.toUInt()
}

fun getRegisterValue(register: String): UInt {
    return registers[register] ?: 0u
}

fun getRegister(value: Int): String {
    return when (value) {
        1 -> "a"
        2 -> "b"
        3 -> "c"
        4 -> "d"
        5 -> "e"
        6 -> "f"
        7 -> ((registers["ptr"] ?: 0u) + (registers["c"] ?: 0u)).toString()
        else -> throw RuntimeException("can't get register for $value")
    }
}

fun get32BitRegister(value: Int): String {
    return when (value) {
        1 -> "la"
        2 -> "lb"
        3 -> "lc"
        4 -> "ld"
        5 -> "ptr"
        6 -> "pc"
        else -> throw RuntimeException("can't get register for $value")
    }
}

enum class Command(val opCode: UByte? = null, val opCodePattern: String? = null, val size: Int) {
    ADD_A_X_B(opCode = 0xC2.toUByte(), size = 1), // Sets `a` to the sum of `a` and `b`, modulo 255.
    APTR_IMM8(opCode = 0xE1.toUByte(), size = 2), // Sets `ptr` to the sum of `ptr` and `imm8`. Overflow behaviour is undefined.
    CMP(opCode = 0xC1.toUByte(), size = 1), // Sets `f` to zero if `a` and `b` are equal, otherwise sets `f` to 0x01.
    HALT(opCode = 0x01.toUByte(), size = 1), // Stops the execution of the virtual machine. Indicates that the program has finished successfully.
    JEZ_IMM32(opCode = 0x21.toUByte(), size = 5), // If `f` is equal to zero, sets `pc` to `imm32`. Otherwise does nothing.
    JNZ_IMM32(opCode = 0x22.toUByte(), size = 5), //  If `f` is not equal to zero, sets `pc` to `imm32`. Otherwise does nothing.
    MV_DEST_X_SRC(opCodePattern = "01******", size = 1), // Sets `{dest}` to the value of `{src}`.
    MV32_DEST_X_SRC(opCodePattern = "10******", size = 1), // Sets `{dest}` to the value of `{src}`.
    MVI_DEST_X_IMM8(opCodePattern = "01***000", size = 2), // Sets `{dest}` to the value of `imm8`.
    MVI32_DEST_X_IMM32(opCodePattern = "10***000", size = 5), // Sets `{dest}` to the value of `imm32`.
    OUT_A(opCode = 0x02.toUByte(), size = 1), // Appends the value of `a` to the output stream.
    SUB_A_X_B(opCode = 0xC3.toUByte(), size = 1), // Sets `a` to the result of subtracting `b` from `a`. If subtraction would result in a negative number, 255 is added to ensure that the result is non-negative.
    XOR_A_X_B(opCode = 0xC4.toUByte(), size = 1); // Sets `a` to the bitwise exclusive OR of `a` and `b`.

    companion object {
        private val opCodeMap = values().associateBy(Command::opCode)

        fun fromByte(byte: UByte): Command? {
            val command = opCodeMap[byte]
            if (command != null) {
                return command
            }

            val binaryByte = byte.toString(2).padStart(8, '0')
            val startingBits = binaryByte.substring(0, 2)
            val lastBits = binaryByte.substring(5)
            return when (startingBits) {
                "01" -> if (lastBits == "000") MVI_DEST_X_IMM8 else MV_DEST_X_SRC
                "10" -> if (lastBits == "000") MVI32_DEST_X_IMM32 else MV32_DEST_X_SRC
                else -> null
            }
        }
    }
}