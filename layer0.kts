#!/usr/bin/env kscript

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

@file:Include("utils.kts")

val binaryData = File("payloads/layer0.txt").decodeAscii85ToBinary()
val text = binaryData.first.binaryToText(binaryData.second)
text.saveToFile("results/layer0.txt")