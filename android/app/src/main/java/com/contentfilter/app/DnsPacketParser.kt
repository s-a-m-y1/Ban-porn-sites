package com.contentfilter.app

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal DNS packet parser/creator for UDP DNS queries (port 53).
 * - Detects IPv4 UDP packets destined to port 53
 * - Extracts the queried domain name
 * - Builds NXDOMAIN responses (RCODE=3) for blocked domains
 * - Wraps upstream resolver answers back into full UDP/IP packets
 */
class DnsPacketParser {

    fun isDnsPacket(packet: ByteArray): Boolean {
        if (packet.size < 28) return false
        val versionIhl = packet[0].toInt() and 0xFF
        if (versionIhl shr 4 != 4) return false // not IPv4
        val ihl = (versionIhl and 0x0F) * 4
        if (ihl < 20 || packet.size < ihl + 8) return false
        if (packet[9].toInt() and 0xFF != 17) return false // not UDP
        val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or
            (packet[ihl + 3].toInt() and 0xFF)
        return dstPort == 53
    }

    private fun ihl(packet: ByteArray): Int = (packet[0].toInt() and 0x0F) * 4

    fun dnsPayload(packet: ByteArray): ByteArray {
        val start = ihl(packet) + 8
        return packet.copyOfRange(start, packet.size)
    }

    /** Extract the queried domain name from a DNS query packet, or null. */
    fun extractQueryDomain(packet: ByteArray): String? {
        if (!isDnsPacket(packet)) return null
        val dnsStart = ihl(packet) + 8
        val dns = dnsPayload(packet)
        if (dns.size < 12) return null

        val flags = ((dns[2].toInt() and 0xFF) shl 8) or (dns[3].toInt() and 0xFF)
        if (flags and 0x8000 != 0) return null // already a response

        val qdcount = ((dns[4].toInt() and 0xFF) shl 8) or (dns[5].toInt() and 0xFF)
        if (qdcount < 1) return null

        val sb = StringBuilder()
        var pos = 12
        while (true) {
            if (pos >= dns.size) return null
            val labelLen = dns[pos].toInt() and 0xFF
            if (labelLen == 0) break
            if (labelLen and 0xC0 != 0) return null // compression pointer, bail
            pos++
            if (pos + labelLen > dns.size) return null
            if (sb.isNotEmpty()) sb.append('.')
            for (i in 0 until labelLen) {
                val c = dns[pos + i].toInt() and 0xFF
                if (c !in 0x21..0x7E) return null
                sb.append(c.toChar())
            }
            pos += labelLen
        }
        return sb.toString().lowercase().ifEmpty { null }
    }

    /** Client (source) IPv4 address of the query packet. */
    fun clientAddress(packet: ByteArray): ByteArray =
        packet.copyOfRange(12, 16)

    /** Client (source) UDP port of the query packet. */
    fun clientPort(packet: ByteArray): Int {
        val ihl = ihl(packet)
        return ((packet[ihl].toInt() and 0xFF) shl 8) or
            (packet[ihl + 1].toInt() and 0xFF)
    }

    /**
     * Build an NXDOMAIN response for a given DNS query packet:
     * swaps IP addresses and UDP ports, sets QR=1 and RCODE=3.
     */
    fun buildNxDomainResponse(queryPacket: ByteArray): ByteArray {
        val response = queryPacket.copyOf()

        // swap IPv4 src/dst
        for (i in 0 until 4) {
            val t = response[12 + i]
            response[12 + i] = response[16 + i]
            response[16 + i] = t
        }

        // swap UDP ports
        val ihl = ihl(queryPacket)
        for (i in 0 until 2) {
            val t = response[ihl + i]
            response[ihl + i] = response[ihl + 2 + i]
            response[ihl + 2 + i] = t
        }
        // UDP checksum = 0 (allowed for IPv4)
        response[ihl + 6] = 0
        response[ihl + 7] = 0

        // DNS: QR=1, RCODE=3
        val dnsStart = ihl + 8
        val flagsHi = response[dnsStart + 2].toInt() and 0xFF
        response[dnsStart + 2] = ((flagsHi or 0x80) and 0xFF).toByte()
        val flagsLo = response[dnsStart + 3].toInt() and 0xFF
        response[dnsStart + 3] = ((flagsLo and 0xF0) or 0x03).toByte()

        // recompute IPv4 header checksum
        response[10] = 0
        response[11] = 0
        val checksum = computeIpv4Checksum(response, ihl)
        response[10] = ((checksum shr 8) and 0xFF).toByte()
        response[11] = (checksum and 0xFF).toByte()

        return response
    }

    /**
     * Wrap an upstream resolver's DNS answer into a full UDP/IP packet
     * addressed back to the original client (as if from our DNS server).
     */
    fun buildDnsResponse(queryPacket: ByteArray, dnsAnswer: ByteArray): ByteArray {
        val ihl = ihl(queryPacket)
        val clientAddr = clientAddress(queryPacket)
        val clientPort = clientPort(queryPacket)

        val totalLen = 20 + 8 + dnsAnswer.size
        val out = ByteBuffer.allocate(totalLen).order(ByteOrder.BIG_ENDIAN)

        // IPv4 header
        out.put(0x45.toByte())
        out.put(0) // TOS
        out.putShort(totalLen.toShort())
        out.putInt(0x00000000) // id, flags, frag
        out.put(64.toByte()) // TTL
        out.put(17.toByte()) // UDP
        out.putShort(0) // checksum (filled later)
        out.put(10.toByte()); out.put(111.toByte()); out.put(0.toByte()); out.put(2.toByte()) // 10.111.0.2
        out.put(clientAddr[0]); out.put(clientAddr[1]); out.put(clientAddr[2]); out.put(clientAddr[3])

        // UDP header
        out.putShort(53.toShort()) // src port
        out.putShort(clientPort.toShort()) // dst port
        out.putShort((8 + dnsAnswer.size).toShort()) // length
        out.putShort(0) // checksum (0 OK for IPv4)

        // DNS payload
        out.put(dnsAnswer)

        val packet = out.array()
        val checksum = computeIpv4Checksum(packet, 20)
        packet[10] = ((checksum shr 8) and 0xFF).toByte()
        packet[11] = (checksum and 0xFF).toByte()
        return packet
    }

    private fun computeIpv4Checksum(header: ByteArray, ihl: Int): Int {
        var sum = 0
        var i = 0
        while (i < ihl) {
            if (i == 10) {
                i += 2
                continue
            }
            sum += (((header[i].toInt() and 0xFF) shl 8) or
                (header[i + 1].toInt() and 0xFF))
            i += 2
        }
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return sum.inv() and 0xFFFF
    }
}
