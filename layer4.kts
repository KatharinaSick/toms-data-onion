#!/usr/bin/env kscript

@file:Include("utils.kts")

fun File.decodeLayer4(resultFileName: String) {
    val bytes = decodeAscii85()

    val result = ArrayList<UByte>()
    parsePacket(bytes, 0, result)

    result
            .toText()
            .saveToFile(resultFileName)
}

val ipHeaderSize = 20
val udpHeaderSize = 8

fun parsePacket(allBytes: MutableList<UByte>, startIndex: Int, result: ArrayList<UByte>) {
    val udpStartIndex = startIndex + ipHeaderSize
    val dataStartIndex = udpStartIndex + udpHeaderSize

    if (dataStartIndex >= allBytes.size - 1) {
        return
    }

    val ipHeader = allBytes.subList(startIndex, udpStartIndex)
    val udpHeader = allBytes.subList(udpStartIndex, dataStartIndex)

    val packetSize = Integer.parseInt(
            ipHeader
                    .subList(2, 4)
                    .map { Integer.toBinaryString(it.toInt()).padStart(8, '0') }
                    .joinToString(""),
            2
    )

    val dataSize = packetSize - ipHeaderSize - udpHeaderSize
    val endIndex = dataStartIndex + dataSize
    if (endIndex >= allBytes.size - 1) {
        return
    }
    val data = allBytes.subList(dataStartIndex, endIndex)

    if (isPacketValid(ipHeader, udpHeader, data)) {
        result.addAll(data)
    }

    // parse next packet
    parsePacket(allBytes, startIndex + packetSize, result)
}

/*
IP HEADER: 20 Bytes
 - Byte 0:
   - Version: 4 Bit
   - IP Header Length: 4 Bit
 - Byte 1: Type of Service
 - Bytes 2-3: Total Length
 - Bytes 4-5: Identification
 - Bytes 6-7:
   - Flags: 3 Bit
   - Fragment Offset: 13 Bit
 - Byte 8: Time to Live
 - Byte 9: Protocol
 - Bytes 10-11: Header Checksum
 - Bytes 12-15: Source Address
 - Bytes 16-19: Destination Address

UDP HEADER: 8 Bytes
 - Byte 0-1: Source Port
 - Byte 2-3: Destination Port
 - Byte 4-5: Length
 - Byte 6-7: Checksum
 */
fun isPacketValid(ipHeader: MutableList<UByte>, udpHeader: MutableList<UByte>, data: MutableList<UByte>): Boolean {
    // ======================================================
    // === The packet was sent FROM any port of 10.1.1.10 ===
    // ======================================================
    if (!doesIpAddressMatch(ipHeader.subList(12, 16), listOf(10, 1, 1, 10))) return false

    // =======================================================
    // === The packet was sent TO port 42069 of 10.1.1.200 ===
    // =======================================================
    if (!doesIpAddressMatch(ipHeader.subList(16, 20), listOf(10, 1, 1, 200))) return false
    if (!doesPortMatch(udpHeader.subList(2, 4), 42069)) return false

    // ===========================================
    // === The IPv4 header checksum is correct ===
    // ===========================================
    val ipChecksum = (ipHeader[10].toUInt() shl 8) or ipHeader[11].toUInt()
    // replace the checksum bytes with 0 for the calculation
    val ipHeaderWithoutChecksum = ArrayList<UByte>()
    ipHeaderWithoutChecksum.addAll(ipHeader)
    ipHeaderWithoutChecksum.set(10, 0u.toUByte())
    ipHeaderWithoutChecksum.set(11, 0u.toUByte())

    if (calculateChecksum(ipHeaderWithoutChecksum) != ipChecksum) return false

    // ==========================================
    // === The UDP header checksum is correct ===
    // ==========================================
    val checksumBase = ArrayList<UByte>()
    // pseudo header
    checksumBase.addAll(ipHeader.subList(12, 20)) // Source & Destination
    checksumBase.add(0u.toUByte()) // 0
    checksumBase.add(17u.toUByte()) // Protocol - UDP = 17
    checksumBase.addAll(udpHeader.subList(4, 6)) // UDP Length
    // udp header without checksum
    checksumBase.addAll(udpHeader.subList(0, 6))
    // data
    checksumBase.addAll(data)

    val udpChecksum = (udpHeader[6].toUInt() shl 8) or udpHeader[7].toUInt()
    if (udpChecksum != 0u) { // no checksum was calculated before sending the packet
        if (checksumBase.size % 2 != 0) checksumBase.add(0u.toUByte())
        if ( calculateChecksum(checksumBase) != udpChecksum) return false
    }

    return true
}

fun calculateChecksum(bytes: List<UByte>): UInt {
    var checksum = 0u

    for (i in bytes.indices step 2) {
        checksum += (bytes[i].toUInt() shl 8) or bytes[i + 1].toUInt()

        val carry = (checksum and 0b10000000000000000u) shr 16
        if (carry == 1u) {
            checksum = checksum and 0b01111111111111111u
            checksum += carry
        }
    }

    return checksum xor 0b1111111111111111u // invert
}


fun doesIpAddressMatch(bytes: List<UByte>, ipToMatch: List<Int>): Boolean {
    if (bytes.size != 4 || bytes.size != ipToMatch.size) {
        return false
    }

    bytes.withIndex().forEach {
        if (it.value.toInt() != ipToMatch[it.index]) {
            return false
        }
    }
    return true
}

fun doesPortMatch(bytes: List<UByte>, portToMatch: Int): Boolean {
    if (bytes.size != 2) {
        return false
    }

    return bytes
            .map { Integer.toBinaryString(it.toInt()).padStart(8, '0') }
            .joinToString("")
            .equals(portToMatch.toString(2).padStart(16, '0'))
}
