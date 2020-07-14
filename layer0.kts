#!/usr/bin/env kscript

@file:Include("utils.kts")

fun File.decodeLayer0(resultFileName: String) {
    decodeAscii85()
            .toText()
            .saveToFile(resultFileName)
}