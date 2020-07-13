#!/usr/bin/env kscript

@file:Include("utils.kts")

fun File.decodeLayer0(resultFileName: String) {
    decodeAscii85()
            .toText()
            .saveToFile(resultFileName)
}

fun File.decodeLayer1(resultFileName: String) {
    decodeAscii85()
            .map { byte ->
                val flippedBits = (byte xor 0b11111111u) and 0b01010101u
                val originalBits = byte and 0b10101010u
                val flippedByte = flippedBits or originalBits

                val lastBit = flippedByte and 0b00000001u
                (flippedByte.shiftRight()) or lastBit
            }
            .toText()
            .saveToFile(resultFileName)
}

fun File.decodeLayer2(resultFileName: String) {
    decodeAscii85()
            .filter { byte ->
                val parity = (byte and 0b00000001u).toInt()

                var numberOfOnes = 0
                var shiftedByte = byte
                for (i in 0 until 7) {
                    shiftedByte = shiftedByte.shiftRight()
                    numberOfOnes += (shiftedByte and 0b00000001u).toInt()
                }

                parity % 2 == numberOfOnes % 2
            }
            .map { byte ->
                Integer.toBinaryString(byte.shiftRight().toInt()).padStart(7, '0')
            }
            .joinToString("")
            .chunked(8)
            .map { binaryByte ->
                Integer.parseInt(binaryByte, 2).toUByte()
            }
            .toText()
            .saveToFile(resultFileName)
}

fun File.decodeLayer3(resultFileName: String) {
    val bytes = decodeAscii85()
    val key = UByteArray(32)

    // Start of file:
    "==[ Layer 4/6: " // key[0-14]
            .toCharArray()
            .map { it.toInt().toUByte() }
            .withIndex()
            .forEach {
                key.set(it.index, it.value xor bytes[it.index])
            }

    // One line has 60 characters
    // "...===" now only goes until char 46 -> char 47 to 59 have to be '=', followed by two new lines
    "=============\n\n" // // key[14-29]
            .toCharArray()
            .map { it.toInt().toUByte() }
            .withIndex()
            .forEach {
                key.set(15 + it.index, it.value xor bytes[32 + 15 + it.index])
            }

    // Gets clear when looking at the decrypted file from above
    " ]" // key[30-31]
            .toCharArray()
            .map { it.toInt().toUByte() }
            .withIndex()
            .forEach {
                key.set(30 + it.index, it.value xor bytes[30 + it.index])
            }

    bytes
            .withIndex()
            .map { it.value xor key.get(it.index % 32) }
            .toText()
            .saveToFile(resultFileName)
}
