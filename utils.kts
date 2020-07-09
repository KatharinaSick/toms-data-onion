#!/usr/bin/env kscript

import java.io.File

fun File.decodeAscii85ToBinary(): Pair<String, Int> {
    val encodedString = readText()
            .substringAfter("<~")
            .substringBefore("~>")
            .replace("\\s".toRegex(), "")
            .replace("\n".toRegex(), "")
            .replace("z", "!!!!!")

    val result = StringBuilder()
    var padding = 0

    encodedString.chunked(5).forEach { asciiGroup ->
        // If the last group of the text to decode is smaller than 5, it needs to be padded with the biggest available character ('u')
        if (asciiGroup.length < 5) {
            padding = 5 - asciiGroup.length
        }
        asciiGroup.padEnd(5, 'u')

        val base85 = asciiGroup.chars().toArray().map { it - 33 }
        val value = base85.withIndex().sumBy { it.value * Math.pow(85.0, 4.0 - it.index).toInt() }
        val binary = Integer.toBinaryString(value).padStart(32, '0')

        result.append(binary)
    }

    return Pair(result.toString(), padding)
}

fun String.binaryToText(padding: Int): String {
    var text = chunked(8)
            .map { Integer.parseInt(it, 2).toChar() }
            .joinToString("")
    if (padding > 0) {
        text = text.substring(0, text.length - padding)
    }
    return text
}

fun String.saveToFile(fileName: String) {
    val file = File(fileName)
    file.createNewFile()
    file.writeText(this)
}