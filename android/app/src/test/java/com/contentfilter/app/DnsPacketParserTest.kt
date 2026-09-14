package com.contentfilter.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure-JVM tests for the DNS packet parser used by the VPN service's
 * packet pump. Each test builds a real IPv4/UDP/DNS query byte-by-byte
 * (no fixtures, no Android framework).
 */
class DnsPacketParserTest {

    private val parser = DnsPacketParser()

    // ---------------------------------------------------------------------
    // Packet-building helpers (independent of the code under test)
    // ---------------------------------------------------------------------

    private fun buildQuery(
        domain: String,
        qtype: Int = 1,
        srcPort: Int = 42000,
        dstPort: Int = 53,
        dnsId: Int = 0x1234,
        flags: Int = 0x0100, // standard query with RD=1
        qdcount: Int = 1,
        srcIp: ByteArray = byteArrayOf(0xC0.toByte(), 0xA8.toByte(), 0x00, 0x0E), // 192.168.0.14
        dstIp: ByteArray = byteArrayOf(8, 8, 8, 8),
        protocol: Int = 17,
        versionIhl: Int = 0x45,
    ): ByteArray {
        val qname = encodeQname(domain)
        val dnsLen = 12 + qname.size + 4
        val ihl = (versionIhl and 0x0F) * 4
        val total = ihl + 8 + dnsLen

        val buf = ByteBuffer.allocate(total).order(ByteOrder.BIG_ENDIAN)
        // --- IPv4 header ---
        buf.put(versionIhl.toByte())
        buf.put(0)                                  // TOS
        buf.putShort(total.toShort())               // total length
        buf.putShort(0x0001)                        // identification
        buf.putShort(0)                             // flags / fragment offset
        buf.put(64)                                 // TTL
        buf.put(protocol.toByte())                  // protocol
        buf.putShort(0)                             // header checksum (not needed to be valid for parsing)
        buf.put(srcIp[0]); buf.put(srcIp[1]); buf.put(srcIp[2]); buf.put(srcIp[3])
        buf.put(dstIp[0]); buf.put(dstIp[1]); buf.put(dstIp[2]); buf.put(dstIp[3])
        while (buf.position() < ihl) buf.put(0)     // IP option padding when IHL > 5
        // --- UDP header ---
        buf.putShort(srcPort.toShort())
        buf.putShort(dstPort.toShort())
        buf.putShort((8 + dnsLen).toShort())
        buf.putShort(0)                             // checksum
        // --- DNS message ---
        buf.putShort(dnsId.toShort())
        buf.putShort(flags.toShort())
        buf.putShort(qdcount.toShort())
        buf.putShort(0)                             // ANCOUNT
        buf.putShort(0)                             // NSCOUNT
        buf.putShort(0)                             // ARCOUNT
        buf.put(qname)
        buf.putShort(qtype.toShort())               // QTYPE (A)
        buf.putShort(1)                             // QCLASS (IN)
        return buf.array()
    }

    /** Standard DNS QNAME encoding: <len><label>...<len><label> 0x00. */
    private fun encodeQname(domain: String): ByteArray {
        val bytes = mutableListOf<Byte>()
        if (domain.isNotEmpty()) {
            for (label in domain.split('.')) {
                bytes.add(label.length.toByte())
                for (c in label) bytes.add(c.code.toByte())
            }
        }
        bytes.add(0)
        return bytes.toByteArray()
    }

    private fun ihlOf(packet: ByteArray) = (packet[0].toInt() and 0x0F) * 4

    private fun u16(b: ByteArray, off: Int): Int =
        ((b[off].toInt() and 0xFF) shl 8) or (b[off + 1].toInt() and 0xFF)

    /** Independent IPv4 checksum verifier: the one's-complement sum over the
     *  whole header (checksum field included) must complement to zero. */
    private fun ipv4ChecksumIsValid(packet: ByteArray, ihl: Int): Boolean {
        var sum = 0
        var i = 0
        while (i < ihl) {
            sum += u16(packet, i)
            i += 2
        }
        while (sum shr 16 != 0) sum = (sum and 0xFFFF) + (sum shr 16)
        return (sum.inv() and 0xFFFF) == 0
    }

    // ---------------------------------------------------------------------
    // isDnsPacket — classification of IPv4/UDP traffic to port 53
    // ---------------------------------------------------------------------

    @Test
    fun isDnsPacketAcceptsStandardQuery() {
        assertTrue(parser.isDnsPacket(buildQuery("example.com")))
    }

    @Test
    fun isDnsPacketRejectsPacketsShorterThanIpv4PlusUdpHeader() {
        assertFalse(parser.isDnsPacket(ByteArray(27)))
        assertFalse(parser.isDnsPacket(ByteArray(0)))
        // a valid query truncated below the 28-byte minimum
        val query = buildQuery("example.com")
        assertFalse(parser.isDnsPacket(query.copyOf(27)))
    }

    @Test
    fun isDnsPacketRejectsNonIpv4() {
        val v6 = buildQuery("example.com").also { it[0] = 0x60 }
        assertFalse(parser.isDnsPacket(v6))
    }

    @Test
    fun isDnsPacketRejectsNonUdpProtocols() {
        val tcp = buildQuery("example.com", protocol = 6)
        assertFalse(parser.isDnsPacket(tcp))
    }

    @Test
    fun isDnsPacketRejectsOtherDestinationPorts() {
        assertFalse(parser.isDnsPacket(buildQuery("example.com", dstPort = 8080)))
        // mDNS on 5353 must not be captured by the filter either
        assertFalse(parser.isDnsPacket(buildQuery("example.com", dstPort = 5353)))
    }

    @Test
    fun isDnsPacketHonorsExplicitLength() {
        val query = buildQuery("example.com")
        // the pump reuses a 32767-byte read buffer and passes the read length
        val padded = query + ByteArray(64)
        assertTrue(parser.isDnsPacket(padded, query.size))
        assertTrue(parser.isDnsPacket(padded, padded.size))
        assertFalse(parser.isDnsPacket(padded, 27))
        assertFalse(parser.isDnsPacket(padded, 12))
    }

    @Test
    fun isDnsPacketHandlesIpOptions() {
        // IHL=6 -> 24-byte IPv4 header (one option word)
        assertTrue(parser.isDnsPacket(buildQuery("example.com", versionIhl = 0x46)))
    }

    @Test
    fun isDnsPacketRejectsTruncatedHeaderForLargeIhl() {
        // IHL=15 claims a 60-byte header but the packet (57 bytes) is shorter
        // than header + UDP, so the port bytes cannot be trusted
        val query = buildQuery("example.com").also { it[0] = 0x4F }
        assertFalse(parser.isDnsPacket(query))
    }

    // ---------------------------------------------------------------------
    // extractQueryDomain — the domain the client is asking about
    // ---------------------------------------------------------------------

    @Test
    fun extractsSimpleDomain() {
        assertEquals("example.com", parser.extractQueryDomain(buildQuery("example.com")))
    }

    @Test
    fun lowercasesExtractedDomain() {
        assertEquals("example.com", parser.extractQueryDomain(buildQuery("ExAmPlE.CoM")))
        assertEquals(
            "www.example.com",
            parser.extractQueryDomain(buildQuery("WwW.ExAmPlE.CoM")),
        )
    }

    @Test
    fun extractsDeepMultiLabelDomain() {
        val domain = "a.b.cdn.example.co.uk"
        assertEquals(domain, parser.extractQueryDomain(buildQuery(domain)))
    }

    @Test
    fun extractsSingleLabelName() {
        assertEquals("localhost", parser.extractQueryDomain(buildQuery("localhost")))
    }

    @Test
    fun returnsNullForNonDnsPacket() {
        assertNull(parser.extractQueryDomain(buildQuery("example.com", protocol = 6)))
        assertNull(parser.extractQueryDomain(ByteArray(27)))
    }

    @Test
    fun returnsNullForResponsePackets() {
        // QR=1 -> this is an answer, not a question
        assertNull(parser.extractQueryDomain(buildQuery("example.com", flags = 0x8180)))
    }

    @Test
    fun returnsNullWhenQuestionCountIsZero() {
        assertNull(parser.extractQueryDomain(buildQuery("example.com", qdcount = 0)))
    }

    @Test
    fun returnsNullForRootQuery() {
        // empty QNAME (just the 0x00 terminator) -> no domain to block on
        assertNull(parser.extractQueryDomain(buildQuery("")))
    }

    @Test
    fun returnsNullOnCompressionPointerInQname() {
        val query = buildQuery("example.com")
        val dnsStart = ihlOf(query) + 8
        query[dnsStart + 12] = 0xC0.toByte() // first label length becomes a pointer
        assertNull(parser.extractQueryDomain(query))
    }

    @Test
    fun returnsNullOnInvalidCharacterInLabel() {
        val query = buildQuery("example.com")
        val dnsStart = ihlOf(query) + 8
        query[dnsStart + 13] = 0x80.toByte() // inside the first label
        assertNull(parser.extractQueryDomain(query))
    }

    @Test
    fun returnsNullWhenLabelRunsPastEndOfPacket() {
        val query = buildQuery("example.com")
        val truncated = query.copyOf(query.size - 10)
        assertNull(parser.extractQueryDomain(truncated))
    }

    @Test
    fun returnsNullWhenDnsHeaderIsTruncated() {
        val query = buildQuery("example.com")
        val dnsStart = ihlOf(query) + 8
        // keep a legal IPv4+UDP header but cut the DNS message below 12 bytes
        val truncated = query.copyOf(dnsStart + 11)
        assertTrue(parser.isDnsPacket(truncated))
        assertNull(parser.extractQueryDomain(truncated))
    }

    // ---------------------------------------------------------------------
    // client address / port and payload location
    // ---------------------------------------------------------------------

    @Test
    fun clientAddressReturnsSourceIp() {
        val srcIp = byteArrayOf(10, 0, 0, 7)
        val query = buildQuery("example.com", srcIp = srcIp)
        assertArrayEquals(srcIp, parser.clientAddress(query))
    }

    @Test
    fun clientPortReturnsSourcePort() {
        assertEquals(42000, parser.clientPort(buildQuery("example.com")))
    }

    @Test
    fun clientPortHandlesPortsAbove32767() {
        assertEquals(50000, parser.clientPort(buildQuery("example.com", srcPort = 50000)))
    }

    @Test
    fun dnsPayloadStartPointsPastIpv4AndUdpHeaders() {
        val query = buildQuery("example.com")
        assertEquals(28, parser.dnsPayloadStart(query))
    }

    @Test
    fun dnsPayloadStartAccountsForIpOptions() {
        val query = buildQuery("example.com", versionIhl = 0x46)
        assertEquals(32, parser.dnsPayloadStart(query))
    }

    @Test
    fun dnsPayloadStartIsNullForNonDns() {
        assertNull(parser.dnsPayloadStart(buildQuery("example.com", dstPort = 8080)))
    }

    @Test
    fun dnsPayloadIsTheTailOfThePacket() {
        val query = buildQuery("example.com", dnsId = 0xABCD)
        val payload = parser.dnsPayload(query)
        assertEquals(query.size - 28, payload.size)
        assertEquals(0xABCD, u16(payload, 0)) // DNS id preserved
        // first QNAME label length byte follows the 12-byte DNS header
        assertEquals(7, payload[12].toInt() and 0xFF) // "example"
    }

    // ---------------------------------------------------------------------
    // buildNxDomainResponse — the "blocked" answer
    // ---------------------------------------------------------------------

    @Test
    fun nxResponseSwapsIpv4Addresses() {
        val srcIp = byteArrayOf(10, 0, 0, 7)
        val dstIp = byteArrayOf(8, 8, 8, 8)
        val query = buildQuery("example.com", srcIp = srcIp, dstIp = dstIp)
        val response = parser.buildNxDomainResponse(query)
        assertArrayEquals(dstIp, response.copyOfRange(12, 16)) // from original dst
        assertArrayEquals(srcIp, response.copyOfRange(16, 20)) // to original src
    }

    @Test
    fun nxResponseSwapsUdpPorts() {
        val query = buildQuery("example.com", srcPort = 42000)
        val response = parser.buildNxDomainResponse(query)
        val ihl = ihlOf(response)
        assertEquals(53, u16(response, ihl))       // from the original dst port
        assertEquals(42000, u16(response, ihl + 2)) // to the original src port
    }

    @Test
    fun nxResponseSetsQrBitAndRcode3() {
        val query = buildQuery("example.com")
        val response = parser.buildNxDomainResponse(query)
        val dnsStart = ihlOf(response) + 8
        val flags = u16(response, dnsStart + 2)
        assertEquals(0x8000, flags and 0x8000) // QR=1: it is a response
        assertEquals(3, flags and 0x000F)       // RCODE=3: NXDOMAIN
        // original query opcode and RD flag survive
        assertEquals(0x0100 and 0x7FF0, flags and 0x7FF0)
    }

    @Test
    fun nxResponseZeroesUdpChecksum() {
        val response = parser.buildNxDomainResponse(buildQuery("example.com"))
        val ihl = ihlOf(response)
        assertEquals(0, u16(response, ihl + 6))
    }

    @Test
    fun nxResponseHasValidIpv4Checksum() {
        val response = parser.buildNxDomainResponse(buildQuery("example.com"))
        assertTrue(ipv4ChecksumIsValid(response, ihlOf(response)))
    }

    @Test
    fun nxResponsePreservesDnsIdAndQuestion() {
        val query = buildQuery("example.com", dnsId = 0xBEEF)
        val response = parser.buildNxDomainResponse(query)
        val dnsStart = ihlOf(response) + 8
        assertEquals(0xBEEF, u16(response, dnsStart)) // client matches replies by id
        // everything after the flags is untouched (counts + question section)
        assertArrayEquals(
            query.copyOfRange(dnsStart + 4, query.size),
            response.copyOfRange(dnsStart + 4, response.size),
        )
    }

    @Test
    fun nxResponseDoesNotMutateTheQuery() {
        val query = buildQuery("example.com")
        val snapshot = query.copyOf()
        parser.buildNxDomainResponse(query)
        assertArrayEquals(snapshot, query)
    }

    @Test
    fun nxResponseHandlesIpOptions() {
        val query = buildQuery("example.com", srcPort = 51000, versionIhl = 0x46)
        val response = parser.buildNxDomainResponse(query)
        val ihl = ihlOf(response) // 24
        assertEquals(53, u16(response, ihl))
        assertEquals(51000, u16(response, ihl + 2))
        val dnsStart = ihl + 8 // 32
        assertEquals(0x8000, u16(response, dnsStart + 2) and 0x8000)
        assertEquals(3, u16(response, dnsStart + 2) and 0x000F)
        assertTrue(ipv4ChecksumIsValid(response, ihl))
    }

    // ---------------------------------------------------------------------
    // buildDnsResponse — wrapping an upstream answer for the client
    // ---------------------------------------------------------------------

    @Test
    fun dnsResponseHasCorrectTotalAndUdpLengths() {
        val query = buildQuery("example.com")
        val answer = ByteArray(90) { (it % 251).toByte() }
        val response = parser.buildDnsResponse(query, answer)
        assertEquals(28 + answer.size, response.size)
        assertEquals(28 + answer.size, u16(response, 2)) // IPv4 total length
        assertEquals(8 + answer.size, u16(response, 24)) // UDP length
    }

    @Test
    fun dnsResponseIsAddressedBackToTheClient() {
        val srcIp = byteArrayOf(10, 0, 0, 7)
        val query = buildQuery("example.com", srcIp = srcIp, srcPort = 51000)
        val response = parser.buildDnsResponse(query, ByteArray(40))
        // from the filter's resolver address...
        assertArrayEquals(byteArrayOf(10, 111, 0, 2), response.copyOfRange(12, 16))
        // ...to the original client
        assertArrayEquals(srcIp, response.copyOfRange(16, 20))
        assertEquals(53, u16(response, 20))    // src port
        assertEquals(51000, u16(response, 22)) // dst port
    }

    @Test
    fun dnsResponseIsIpv4UdpWithValidChecksum() {
        val response = parser.buildDnsResponse(buildQuery("example.com"), ByteArray(40))
        assertEquals(0x45, response[0].toInt() and 0xFF) // IPv4, IHL=5
        assertEquals(17, response[9].toInt() and 0xFF)  // UDP
        assertEquals(0, u16(response, 26))              // UDP checksum 0 (allowed)
        assertTrue(ipv4ChecksumIsValid(response, 20))
    }

    @Test
    fun dnsResponseCarriesAnswerIntact() {
        val answer = ByteArray(64) { (it * 3).toByte() }
        val response = parser.buildDnsResponse(buildQuery("example.com"), answer)
        assertArrayEquals(answer, response.copyOfRange(28, response.size))
    }
}
