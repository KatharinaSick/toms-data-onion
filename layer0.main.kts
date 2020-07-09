#!/usr/bin/env kotlin

import java.io.File
import java.lang.StringBuilder
import kotlin.math.asin
import kotlin.math.pow

/**
 * Solves Layer 0 of Tom's Data Onion (https://www.tomdalling.com/toms-data-onion/)
 *
 * Instructions:
 * ==[ Layer 0/6: ASCII85 ]====================================
 *
 * ASCII85 is a binary-to-text encoding. These encodings are
 * useful when you need to send arbitrary binary data as text,
 * such as sending an image as an email attachment, or
 * embedding obfuscated data in a text file. It takes four
 * bytes of binary data, and converts them into five printable
 * ASCII characters. The encoding only uses 85 "safe" ASCII
 * characters, hence its name.
 *
 *     ----------------------------------------------------
 *
 * This payload has been encoded with Adobe-flavoured ASCII85.
 * All subsequent layers are ASCII85 encoded just like this
 * one, but they require additional processing in order to be
 * solved.
 *
 * Decode the payload below to proceed!
 */

val encodedString = File("payloads/layer0.txt")
        .readText()
        .substringAfter("<~")
        .substringBefore("~>")
        .replace("\\s".toRegex(), "")
        .replace("\n".toRegex(), "")
        .replace("z", "!!!!!")

val result = StringBuilder()

// TODO last chunk
encodedString.chunked(5).forEach { asciiGroup ->
    // If the last group of the text to decode is smaller than 5, it needs to be padded with the biggest available character ('u')
    val padding = 5 - asciiGroup.length
    asciiGroup.padEnd(5, 'u')

    val base85 = asciiGroup.chars().toArray().map { it - 33 }
    val value = base85.withIndex().sumBy { it.value * 85.0.pow(4 - it.index).toInt() }
    val bitPattern = Integer.toBinaryString(value).padStart(32, '0')
    val decodedAsciiValues = bitPattern.chunked(8).map { Integer.parseInt(it, 2) }
    val decodedString = decodedAsciiValues.map { it.toChar() }.joinToString("")

    // remove the padding if the current asciiGroup is smaller than 5
    if (padding > 0) {
        decodedString.substring(0, decodedString.length - padding + 1)
    }
    result.append(decodedString)
}

print(result.toString())
val resultFile = File("results/layer0.txt")
resultFile.createNewFile()
resultFile.writeText(result.toString())