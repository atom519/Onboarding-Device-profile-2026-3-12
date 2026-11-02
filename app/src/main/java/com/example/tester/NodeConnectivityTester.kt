package com.example.tester

import com.example.model.PingStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

object NodeConnectivityTester {

    /**
     * Tests connectivity to a single proxy node via TCP socket handshake.
     * Returns Triple(PingStatus, LatencyMs?, ErrorMessage?)
     */
    suspend fun testNode(
        server: String,
        port: Int,
        timeoutMs: Int = 2500
    ): Triple<PingStatus, Long?, String?> = withContext(Dispatchers.IO) {
        val socket = Socket()
        val startTime = System.currentTimeMillis()
        try {
            val address = InetSocketAddress(server, port)
            socket.connect(address, timeoutMs)
            val latency = System.currentTimeMillis() - startTime
            Triple(PingStatus.ONLINE, latency, null)
        } catch (e: SocketTimeoutException) {
            Triple(PingStatus.TIMEOUT, null, "连接超时 (${timeoutMs}ms)")
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("ECONNREFUSED", ignoreCase = true) == true -> "端口拒绝连接"
                e.message?.contains("ENETUNREACH", ignoreCase = true) == true -> "网络不可达"
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "无法解析域名"
                else -> e.message ?: "连接失败"
            }
            Triple(PingStatus.ERROR, null, msg)
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Batch test nodes with concurrency limit and real-time callbacks.
     */
    suspend fun testAllNodes(
        nodes: List<com.example.model.ProxyNode>,
        concurrency: Int = 12,
        timeoutMs: Int = 2500,
        onNodeTested: (nodeId: String, status: PingStatus, latencyMs: Long?, error: String?) -> Unit,
        onProgress: (testedCount: Int, totalCount: Int) -> Unit
    ) = coroutineScope {
        val semaphore = Semaphore(concurrency)
        var testedCount = 0

        val jobs = nodes.map { node ->
            launch {
                semaphore.withPermit {
                    val (status, latency, error) = testNode(node.server, node.port, timeoutMs)
                    onNodeTested(node.id, status, latency, error)
                    synchronized(this@NodeConnectivityTester) {
                        testedCount++
                        onProgress(testedCount, nodes.size)
                    }
                }
            }
        }
        jobs.joinAll()
    }
}
