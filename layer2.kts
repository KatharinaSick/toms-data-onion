#!/usr/bin/env kscript

@file:Include("utils.kts")

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