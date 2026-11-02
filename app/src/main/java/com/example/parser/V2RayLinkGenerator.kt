package com.example.parser

import com.example.util.Base64Util
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object V2RayLinkGenerator {

    private fun encodeUriComponent(s: String): String {
        return URLEncoder.encode(s, StandardCharsets.UTF_8.name())
            .replace("+", "%20")
    }

    private fun toBase64(s: String): String {
        return Base64Util.encode(s)
    }

    /**
     * Converts a Clash proxy map into standard V2RayN VMess URL format.
     */
    fun generateVmess(map: Map<String, Any?>): String {
        val name = map["name"]?.toString() ?: "VMess Node"
        val server = map["server"]?.toString() ?: ""
        val port = map["port"]?.toString() ?: "443"
        val uuid = map["uuid"]?.toString() ?: ""
        val alterId = map["alterId"]?.toString() ?: "0"
        val cipher = map["cipher"]?.toString() ?: "auto"

        val network = (map["network"]?.toString() ?: "tcp").lowercase()
        val tls = map["tls"]?.toString()?.toBooleanStrictOrNull() ?: false
        val servername = map["servername"]?.toString() ?: map["sni"]?.toString() ?: ""

        // Transport options
        var path = ""
        var host = ""

        val wsOpts = map["ws-opts"] as? Map<*, *>
        if (wsOpts != null) {
            path = wsOpts["path"]?.toString() ?: ""
            val headers = wsOpts["headers"] as? Map<*, *>
            host = headers?.get("Host")?.toString() ?: headers?.get("host")?.toString() ?: ""
        } else {
            path = map["ws-path"]?.toString() ?: ""
            val headers = map["ws-headers"] as? Map<*, *>
            host = headers?.get("Host")?.toString() ?: headers?.get("host")?.toString() ?: ""
        }

        val grpcOpts = map["grpc-opts"] as? Map<*, *>
        val grpcServiceName = grpcOpts?.get("grpc-service-name")?.toString() ?: ""
        if (grpcServiceName.isNotEmpty()) {
            path = grpcServiceName
        }

        val json = JSONObject().apply {
            put("v", "2")
            put("ps", name)
            put("add", server)
            put("port", port)
            put("id", uuid)
            put("aid", alterId)
            put("scy", cipher)
            put("net", network)
            put("type", "none")
            put("host", host)
            put("path", path)
            put("tls", if (tls) "tls" else "")
            put("sni", servername)
            put("alpn", "")
        }

        return "vmess://" + toBase64(json.toString())
    }

    /**
     * Converts a Clash proxy map into V2RayN VLESS URL format.
     */
    fun generateVless(map: Map<String, Any?>): String {
        val name = map["name"]?.toString() ?: "VLESS Node"
        val server = map["server"]?.toString() ?: ""
        val port = map["port"]?.toString() ?: "443"
        val uuid = map["uuid"]?.toString() ?: ""

        val network = (map["network"]?.toString() ?: "tcp").lowercase()
        val tls = map["tls"]?.toString()?.toBooleanStrictOrNull() ?: false
        val servername = map["servername"]?.toString() ?: map["sni"]?.toString() ?: ""
        val flow = map["flow"]?.toString() ?: ""

        var path = ""
        var host = ""

        val wsOpts = map["ws-opts"] as? Map<*, *>
        if (wsOpts != null) {
            path = wsOpts["path"]?.toString() ?: ""
            val headers = wsOpts["headers"] as? Map<*, *>
            host = headers?.get("Host")?.toString() ?: headers?.get("host")?.toString() ?: ""
        }

        val grpcOpts = map["grpc-opts"] as? Map<*, *>
        val grpcServiceName = grpcOpts?.get("grpc-service-name")?.toString() ?: ""

        val realityOpts = map["reality-opts"] as? Map<*, *>
        val publicKey = realityOpts?.get("public-key")?.toString() ?: ""
        val shortId = realityOpts?.get("short-id")?.toString() ?: ""

        val queryParams = mutableListOf<String>()
        queryParams.add("type=$network")

        if (realityOpts != null && publicKey.isNotEmpty()) {
            queryParams.add("security=reality")
            queryParams.add("pbk=${encodeUriComponent(publicKey)}")
            if (shortId.isNotEmpty()) queryParams.add("sid=${encodeUriComponent(shortId)}")
            val fp = map["client-fingerprint"]?.toString() ?: "chrome"
            queryParams.add("fp=$fp")
        } else if (tls) {
            queryParams.add("security=tls")
        } else {
            queryParams.add("security=none")
        }

        if (flow.isNotEmpty()) {
            queryParams.add("flow=${encodeUriComponent(flow)}")
        }
        if (servername.isNotEmpty()) {
            queryParams.add("sni=${encodeUriComponent(servername)}")
        }
        if (host.isNotEmpty()) {
            queryParams.add("host=${encodeUriComponent(host)}")
        }
        if (path.isNotEmpty()) {
            queryParams.add("path=${encodeUriComponent(path)}")
        }
        if (grpcServiceName.isNotEmpty()) {
            queryParams.add("serviceName=${encodeUriComponent(grpcServiceName)}")
            queryParams.add("mode=gun")
        }

        val queryString = if (queryParams.isNotEmpty()) "?" + queryParams.joinToString("&") else ""
        return "vless://$uuid@$server:$port$queryString#${encodeUriComponent(name)}"
    }

    /**
     * Converts a Clash proxy map into SIP002 Shadowsocks URL format.
     */
    fun generateShadowsocks(map: Map<String, Any?>): String {
        val name = map["name"]?.toString() ?: "SS Node"
        val server = map["server"]?.toString() ?: ""
        val port = map["port"]?.toString() ?: "8388"
        val cipher = map["cipher"]?.toString() ?: "aes-256-gcm"
        val password = map["password"]?.toString() ?: ""

        val userInfo = toBase64("$cipher:$password")

        val plugin = map["plugin"]?.toString() ?: ""
        val pluginOpts = map["plugin-opts"] as? Map<*, *>
        var pluginQuery = ""
        if (plugin.isNotEmpty()) {
            val optsList = mutableListOf<String>()
            pluginOpts?.forEach { (k, v) ->
                if (k != null && v != null) {
                    optsList.add("$k=$v")
                }
            }
            val pluginStr = if (optsList.isNotEmpty()) "$plugin;${optsList.joinToString(";")}" else plugin
            pluginQuery = "/?plugin=${encodeUriComponent(pluginStr)}"
        }

        return "ss://$userInfo@$server:$port$pluginQuery#${encodeUriComponent(name)}"
    }

    /**
     * Converts a Clash proxy map into Trojan URL format.
     */
    fun generateTrojan(map: Map<String, Any?>): String {
        val name = map["name"]?.toString() ?: "Trojan Node"
        val server = map["server"]?.toString() ?: ""
        val port = map["port"]?.toString() ?: "443"
        val password = map["password"]?.toString() ?: ""
        val sni = map["sni"]?.toString() ?: map["servername"]?.toString() ?: ""
        val network = (map["network"]?.toString() ?: "tcp").lowercase()

        val queryParams = mutableListOf<String>()
        queryParams.add("security=tls")
        if (sni.isNotEmpty()) {
            queryParams.add("sni=${encodeUriComponent(sni)}")
        }
        if (network != "tcp") {
            queryParams.add("type=$network")
        }

        val wsOpts = map["ws-opts"] as? Map<*, *>
        if (wsOpts != null) {
            val path = wsOpts["path"]?.toString() ?: ""
            val headers = wsOpts["headers"] as? Map<*, *>
            val host = headers?.get("Host")?.toString() ?: headers?.get("host")?.toString() ?: ""
            if (path.isNotEmpty()) queryParams.add("path=${encodeUriComponent(path)}")
            if (host.isNotEmpty()) queryParams.add("host=${encodeUriComponent(host)}")
        }

        val grpcOpts = map["grpc-opts"] as? Map<*, *>
        val grpcServiceName = grpcOpts?.get("grpc-service-name")?.toString() ?: ""
        if (grpcServiceName.isNotEmpty()) {
            queryParams.add("serviceName=${encodeUriComponent(grpcServiceName)}")
        }

        val queryString = if (queryParams.isNotEmpty()) "?" + queryParams.joinToString("&") else ""
        return "trojan://${encodeUriComponent(password)}@$server:$port$queryString#${encodeUriComponent(name)}"
    }

    /**
     * Converts a Clash proxy map into Hysteria2 URL format.
     */
    fun generateHysteria2(map: Map<String, Any?>): String {
        val name = map["name"]?.toString() ?: "Hy2 Node"
        val server = map["server"]?.toString() ?: ""
        val port = map["port"]?.toString() ?: "443"
        val auth = map["password"]?.toString() ?: map["auth"]?.toString() ?: ""
        val sni = map["sni"]?.toString() ?: ""
        val insecure = map["skip-cert-verify"]?.toString()?.toBooleanStrictOrNull() ?: false

        val queryParams = mutableListOf<String>()
        if (sni.isNotEmpty()) {
            queryParams.add("sni=${encodeUriComponent(sni)}")
        }
        if (insecure) {
            queryParams.add("insecure=1")
        }

        val queryString = if (queryParams.isNotEmpty()) "?" + queryParams.joinToString("&") else ""
        return "hysteria2://${encodeUriComponent(auth)}@$server:$port/$queryString#${encodeUriComponent(name)}"
    }

    /**
     * Converts a Clash proxy map into TUIC URL format.
     */
    fun generateTuic(map: Map<String, Any?>): String {
        val name = map["name"]?.toString() ?: "TUIC Node"
        val server = map["server"]?.toString() ?: ""
        val port = map["port"]?.toString() ?: "443"
        val uuid = map["uuid"]?.toString() ?: ""
        val password = map["password"]?.toString() ?: ""
        val sni = map["sni"]?.toString() ?: ""
        val cc = map["congestion-controller"]?.toString() ?: "bbr"

        val queryParams = mutableListOf(
            "congestion_control=$cc",
            "alpn=h3"
        )
        if (sni.isNotEmpty()) queryParams.add("sni=${encodeUriComponent(sni)}")

        val userPart = if (password.isNotEmpty()) "$uuid:$password" else uuid
        val queryString = "?" + queryParams.joinToString("&")
        return "tuic://${encodeUriComponent(userPart)}@$server:$port$queryString#${encodeUriComponent(name)}"
    }

    /**
     * Converts Socks5 / HTTP
     */
    fun generateSocks5(map: Map<String, Any?>): String {
        val name = map["name"]?.toString() ?: "Socks5 Node"
        val server = map["server"]?.toString() ?: ""
        val port = map["port"]?.toString() ?: "1080"
        val user = map["username"]?.toString() ?: ""
        val pass = map["password"]?.toString() ?: ""

        val auth = if (user.isNotEmpty()) "${encodeUriComponent(user)}:${encodeUriComponent(pass)}@" else ""
        return "socks5://$auth$server:$port#${encodeUriComponent(name)}"
    }

    fun generateHttp(map: Map<String, Any?>): String {
        val name = map["name"]?.toString() ?: "HTTP Node"
        val server = map["server"]?.toString() ?: ""
        val port = map["port"]?.toString() ?: "8080"
        val user = map["username"]?.toString() ?: ""
        val pass = map["password"]?.toString() ?: ""

        val auth = if (user.isNotEmpty()) "${encodeUriComponent(user)}:${encodeUriComponent(pass)}@" else ""
        return "http://$auth$server:$port#${encodeUriComponent(name)}"
    }
}
