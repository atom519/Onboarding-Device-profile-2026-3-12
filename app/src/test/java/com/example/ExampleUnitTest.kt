package com.example

import com.example.model.PingStatus
import com.example.model.ProxyNode
import com.example.model.ProxyType
import com.example.parser.ClashParser
import com.example.util.SampleClashConfig
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Test
    fun testSampleClashParsing() {
        val result = ClashParser.parse(SampleClashConfig.SAMPLE_YAML)
        if (result.isFailure) {
            println("PARSE ERROR: " + result.exceptionOrNull()?.message)
            result.exceptionOrNull()?.printStackTrace()
        }
        assertTrue("Parsing sample clash yaml should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val nodes = result.getOrNull()
        assertNotNull(nodes)
        assertTrue("Should extract at least 5 nodes", nodes!!.size >= 5)

        // Verify protocol types are correctly identified
        val types = nodes.map { it.type }.toSet()
        assertTrue("Should contain VMess", types.contains(ProxyType.VMESS))
        assertTrue("Should contain VLESS", types.contains(ProxyType.VLESS))
        assertTrue("Should contain Shadowsocks", types.contains(ProxyType.SHADOWSOCKS))
        assertTrue("Should contain Trojan", types.contains(ProxyType.TROJAN))
        assertTrue("Should contain Hysteria2", types.contains(ProxyType.HYSTERIA2))

        // Verify V2RayN URIs are formatted properly
        for (node in nodes) {
            when (node.type) {
                ProxyType.VMESS -> assertTrue(node.v2rayLink.startsWith("vmess://"))
                ProxyType.VLESS -> assertTrue(node.v2rayLink.startsWith("vless://"))
                ProxyType.SHADOWSOCKS -> assertTrue(node.v2rayLink.startsWith("ss://"))
                ProxyType.TROJAN -> assertTrue(node.v2rayLink.startsWith("trojan://"))
                ProxyType.HYSTERIA2 -> assertTrue(node.v2rayLink.startsWith("hysteria2://"))
                else -> {}
            }
        }
    }

    @Test
    fun testFilteringNodes() {
        val node1 = ProxyNode(
            id = UUID.randomUUID().toString(),
            name = "Online-1",
            type = ProxyType.VMESS,
            server = "1.1.1.1",
            port = 443,
            v2rayLink = "vmess://dummy1",
            pingStatus = PingStatus.ONLINE,
            latencyMs = 45L
        )
        val node2 = ProxyNode(
            id = UUID.randomUUID().toString(),
            name = "Offline-1",
            type = ProxyType.SHADOWSOCKS,
            server = "192.0.2.1",
            port = 8888,
            v2rayLink = "ss://dummy2",
            pingStatus = PingStatus.TIMEOUT
        )

        val list = listOf(node1, node2)
        val onlineOnly = list.filter { it.pingStatus == PingStatus.ONLINE }

        assertEquals(1, onlineOnly.size)
        assertEquals("Online-1", onlineOnly.first().name)
    }
}
