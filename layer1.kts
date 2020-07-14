#!/usr/bin/env kscript

@file:Include("utils.kts")

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