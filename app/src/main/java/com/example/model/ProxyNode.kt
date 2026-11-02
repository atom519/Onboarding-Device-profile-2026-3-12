package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class ProxyType(val label: String, val badgeColor: Color) {
    VMESS("VMess", ProtocolVmess),
    VLESS("VLESS", ProtocolVless),
    SHADOWSOCKS("Shadowsocks", ProtocolShadowsocks),
    TROJAN("Trojan", ProtocolTrojan),
    HYSTERIA2("Hysteria2", ProtocolHysteria2),
    TUIC("TUIC", ProtocolTuic),
    SOCKS5("SOCKS5", ProtocolSocks5),
    HTTP("HTTP", ProtocolHttp),
    UNKNOWN("Unknown", SlateTextSecondaryDark)
}

enum class PingStatus {
    UNTESTED,
    TESTING,
    ONLINE,
    TIMEOUT,
    ERROR
}

data class ProxyNode(
    val id: String,
    val name: String,
    val type: ProxyType,
    val server: String,
    val port: Int,
    val v2rayLink: String,
    val rawDetails: Map<String, Any?> = emptyMap(),
    val pingStatus: PingStatus = PingStatus.UNTESTED,
    val latencyMs: Long? = null,
    val errorMessage: String? = null
) {
    val isAvailable: Boolean
        get() = pingStatus == PingStatus.ONLINE

    val displayLatency: String
        get() = when (pingStatus) {
            PingStatus.UNTESTED -> "未测试"
            PingStatus.TESTING -> "测速中…"
            PingStatus.ONLINE -> "${latencyMs ?: 0} ms"
            PingStatus.TIMEOUT -> "超时"
            PingStatus.ERROR -> "连接失败"
        }

    val latencyColor: Color
        get() = when (pingStatus) {
            PingStatus.UNTESTED -> LatencyUntested
            PingStatus.TESTING -> CyanPrimary
            PingStatus.ONLINE -> {
                val ms = latencyMs ?: 0
                when {
                    ms < 180 -> LatencyFast
                    ms < 400 -> LatencyMedium
                    else -> LatencySlow
                }
            }
            PingStatus.TIMEOUT, PingStatus.ERROR -> LatencyOffline
        }
}
