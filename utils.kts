#!/usr/bin/env kscript

import java.io.File

fun File.decodeAscii85(): List<UByte> {
    val encodedString = readText()
            .substringAfter("<~")
            .substringBefore("~>")
            .replace("\\s".toRegex(), "")
            .replace("\n".toRegex(), "")
            .replace("z", "!!!!!")

    val result = ArrayList<UByte>()
    var padding = 5

    encodedString.chunked(5).forEach {
        // If the last group of the text to decode is smaller than 5, it needs to be padded with the biggest available character ('u')
        if (it.length < 5) {
            padding = 5 - it.length
        }
        val asciiCharacters = it.padEnd(5, 'u')

        val base85 = asciiCharacters.chars().toArray().map { it - 33 }
        val value = base85.withIndex().sumBy { it.value * Math.pow(85.0, 4.0 - it.index).toInt() }
        val binary = Integer.toBinaryString(value).padStart(32, '0')

        result.addAll(
                binary
                        .chunked(8)
                        .filterIndexed { index, _ -> index < padding }
                        .map {
                            Integer.parseInt(it, 2).toUByte()
                        }
        )
    }

    return result
}

fun List<UByte>.toText(): String {
    return map { it.toInt().toChar() }.joinToString("")
}

fun String.saveToFile(fileName: String) {
    val file = File(fileName)
    file.createNewFile()
    file.writeText(this)
}