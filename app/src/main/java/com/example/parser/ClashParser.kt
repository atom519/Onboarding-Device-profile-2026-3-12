package com.example.parser

import com.example.model.PingStatus
import com.example.model.ProxyNode
import com.example.model.ProxyType
import com.example.util.Base64Util
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.nio.charset.StandardCharsets
import java.util.UUID

object ClashParser {

    /**
     * Parse raw string which can be:
     * 1. Standard Clash YAML configuration (with proxies: key)
     * 2. YAML list of proxies directly
     * 3. Base64-encoded Clash YAML or subscriptions
     * 4. Direct list of V2Ray URIs (vmess://, vless://, ss://, etc.)
     */
    fun parse(input: String): Result<List<ProxyNode>> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("配置内容为空"))
        }

        // Try direct lines check if user pasted raw V2rayN links
        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val uriPrefixes = listOf("vmess://", "vless://", "ss://", "trojan://", "hysteria2://", "hy2://", "tuic://", "socks5://", "http://")
        val isDirectUriList = lines.isNotEmpty() && lines.all { line ->
            uriPrefixes.any { line.startsWith(it, ignoreCase = true) }
        }

        if (isDirectUriList) {
            val directNodes = parseDirectUris(lines)
            if (directNodes.isNotEmpty()) {
                return Result.success(directNodes)
            }
        }

        // Check if content is Base64 encoded
        var contentToParse = trimmed
        if (!trimmed.contains("\n") && !trimmed.contains(":") && trimmed.length > 20) {
            try {
                val decodedStr = Base64Util.decodeToString(trimmed).trim()
                if (decodedStr.isNotEmpty() && (decodedStr.contains("proxies:") || decodedStr.contains("name:") || uriPrefixes.any { decodedStr.startsWith(it, ignoreCase = true) })) {
                    contentToParse = decodedStr
                }
            } catch (_: Exception) {
                // Not base64, continue
            }
        }

        return try {
            val options = LoaderOptions().apply {
                codePointLimit = 32 * 1024 * 1024 // 32MB max
            }
            val yaml = Yaml(SafeConstructor(options))
            val obj = yaml.load<Any>(contentToParse)

            val rawProxies = extractProxiesList(obj)
            if (rawProxies.isEmpty()) {
                // If YAML didn't find proxies, try parsing as lines of links
                val fallbackLines = contentToParse.lines().map { it.trim() }.filter { it.isNotEmpty() }
                val fallbackNodes = parseDirectUris(fallbackLines)
                if (fallbackNodes.isNotEmpty()) {
                    return Result.success(fallbackNodes)
                }
                return Result.failure(IllegalArgumentException("未在配置中找到有效代理节点 (缺少 proxies 列表)"))
            }

            val nodes = mutableListOf<ProxyNode>()
            for ((index, item) in rawProxies.withIndex()) {
                if (item is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    val map = item as Map<String, Any?>
                    val node = convertProxyMapToNode(map, index)
                    if (node != null) {
                        nodes.add(node)
                    }
                }
            }

            if (nodes.isEmpty()) {
                Result.failure(IllegalArgumentException("解析完成，但未能识别出支持的节点类型"))
            } else {
                Result.success(nodes)
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Clash 配置解析失败: ${e.message ?: "格式错误"}", e))
        }
    }

    private fun extractProxiesList(obj: Any?): List<*> {
        if (obj is List<*>) {
            return obj
        }
        if (obj is Map<*, *>) {
            val proxies = obj["proxies"] ?: obj["Proxies"]
            if (proxies is List<*>) {
                return proxies
            }
        }
        return emptyList<Any>()
    }

    private fun convertProxyMapToNode(map: Map<String, Any?>, index: Int): ProxyNode? {
        val name = map["name"]?.toString() ?: "Node-${index + 1}"
        val typeStr = (map["type"]?.toString() ?: "").lowercase()
        val server = map["server"]?.toString() ?: return null
        val port = when (val p = map["port"]) {
            is Number -> p.toInt()
            is String -> p.toIntOrNull() ?: 443
            else -> 443
        }

        val id = UUID.randomUUID().toString()

        val (type, link) = when (typeStr) {
            "vmess" -> {
                ProxyType.VMESS to V2RayLinkGenerator.generateVmess(map)
            }
            "vless" -> {
                ProxyType.VLESS to V2RayLinkGenerator.generateVless(map)
            }
            "ss", "shadowsocks" -> {
                ProxyType.SHADOWSOCKS to V2RayLinkGenerator.generateShadowsocks(map)
            }
            "trojan" -> {
                ProxyType.TROJAN to V2RayLinkGenerator.generateTrojan(map)
            }
            "hysteria2", "hy2", "hysteria" -> {
                ProxyType.HYSTERIA2 to V2RayLinkGenerator.generateHysteria2(map)
            }
            "tuic" -> {
                ProxyType.TUIC to V2RayLinkGenerator.generateTuic(map)
            }
            "socks5" -> {
                ProxyType.SOCKS5 to V2RayLinkGenerator.generateSocks5(map)
            }
            "http" -> {
                ProxyType.HTTP to V2RayLinkGenerator.generateHttp(map)
            }
            else -> {
                return null
            }
        }

        return ProxyNode(
            id = id,
            name = name,
            type = type,
            server = server,
            port = port,
            v2rayLink = link,
            rawDetails = map,
            pingStatus = PingStatus.UNTESTED
        )
    }

    private fun parseDirectUris(lines: List<String>): List<ProxyNode> {
        val result = mutableListOf<ProxyNode>()
        for ((idx, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val type = when {
                trimmed.startsWith("vmess://", ignoreCase = true) -> ProxyType.VMESS
                trimmed.startsWith("vless://", ignoreCase = true) -> ProxyType.VLESS
                trimmed.startsWith("ss://", ignoreCase = true) -> ProxyType.SHADOWSOCKS
                trimmed.startsWith("trojan://", ignoreCase = true) -> ProxyType.TROJAN
                trimmed.startsWith("hysteria2://", ignoreCase = true) || trimmed.startsWith("hy2://", ignoreCase = true) -> ProxyType.HYSTERIA2
                trimmed.startsWith("tuic://", ignoreCase = true) -> ProxyType.TUIC
                trimmed.startsWith("socks5://", ignoreCase = true) -> ProxyType.SOCKS5
                trimmed.startsWith("http://", ignoreCase = true) -> ProxyType.HTTP
                else -> null
            }

            if (type != null) {
                val (server, port, name) = extractBasicInfoFromUri(trimmed, type, idx)
                result.add(
                    ProxyNode(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        type = type,
                        server = server,
                        port = port,
                        v2rayLink = trimmed,
                        pingStatus = PingStatus.UNTESTED
                    )
                )
            }
        }
        return result
    }

    private fun extractBasicInfoFromUri(uri: String, type: ProxyType, index: Int): Triple<String, Int, String> {
        var defaultName = "Node-${index + 1}"
        var defaultServer = "unknown"
        var defaultPort = 443

        try {
            if (type == ProxyType.VMESS) {
                val b64 = uri.substringAfter("vmess://")
                val decoded = Base64Util.decodeToString(b64)
                val json = org.json.JSONObject(decoded)
                defaultName = json.optString("ps", defaultName)
                defaultServer = json.optString("add", defaultServer)
                defaultPort = json.optInt("port", defaultPort)
            } else {
                val hashIdx = uri.indexOf('#')
                if (hashIdx != -1) {
                    val rawName = uri.substring(hashIdx + 1)
                    defaultName = java.net.URLDecoder.decode(rawName, StandardCharsets.UTF_8.name())
                }
                val withoutScheme = uri.substringAfter("://").substringBefore('#').substringBefore('?')
                val hostPort = if (withoutScheme.contains('@')) withoutScheme.substringAfter('@') else withoutScheme
                val parts = hostPort.split(':')
                if (parts.isNotEmpty()) defaultServer = parts[0]
                if (parts.size > 1) defaultPort = parts[1].toIntOrNull() ?: 443
            }
        } catch (_: Exception) {
            // fallback defaults
        }

        return Triple(defaultServer, defaultPort, defaultName)
    }
}
