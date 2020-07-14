#!/usr/bin/env kscript

@file:Include("utils.kts")

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
