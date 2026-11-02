package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun GuideScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "V2RayN 节点转换使用指南",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "一键将 Clash 配置转换为各类客户端通用的节点链接",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Section 1: How to Import into V2RayN
        GuideCard(
            title = "1. 如何将转换后的节点导入 V2RayN？",
            icon = Icons.Default.Download
        ) {
            Text(
                text = "• 快捷键导入：在转换页面点击「复制当前全部」，打开电脑端的 V2RayN 主界面，直接按下键盘快捷键 Ctrl + V 即可瞬间批量导入所有节点。\n\n" +
                       "• 菜单导入：在 V2RayN 顶部菜单栏，点击「服务器」 -> 「从剪贴板批量导入URL (Ctrl+V)」。\n\n" +
                       "• 手机扫码导入：在节点卡片上点击二维码图标，使用手机端 V2RayNG、NekoBox 或 Shadowrocket 扫描屏幕上的二维码即可直接添加单个节点。\n\n" +
                       "• 订阅导入：在「批量导出」页面开启「Base64 订阅格式」，复制内容可直接作为自定义订阅源使用。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Section 2: Supported Protocols
        GuideCard(
            title = "2. 支持转换的代理协议类型",
            icon = Icons.Default.NetworkCheck
        ) {
            Text(
                text = "本工具覆盖了目前主流的代理协议转换：\n\n" +
                       "• VMess (vmess://)：标准 V2RayN Base64 JSON 格式，完整映射 ws-opts、path、host、tls、servername、alterId、cipher 等。\n\n" +
                       "• VLESS (vless://)：支持 XTLS、Reality (pbk, sid, fp)、WebSocket 及 gRPC 传输模式。\n\n" +
                       "• Shadowsocks (ss://)：遵循 SIP002 标准规范，支持 AEAD 加密及 v2ray-plugin / obfs 插件参数。\n\n" +
                       "• Trojan (trojan://)：支持 SNI、ALPN、WebSocket 和 gRPC 传输层。\n\n" +
                       "• Hysteria2 (hysteria2://)：支持新一代基于 UDP/QUIC 的极速协议及免验证证书选项。\n\n" +
                       "• TUIC (tuic://)：支持拥塞控制算法 (bbr) 与 ALPN。\n\n" +
                       "• Socks5 / HTTP：标准代理链接。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Section 3: Automatic Dead Node Filtering
        GuideCard(
            title = "3. 节点测速与失效过滤原理",
            icon = Icons.Default.Speed
        ) {
            Text(
                text = "• 真连接 TCP 握手：点击「连通性测速」时，系统会使用协程高并发发起对目标节点服务器与端口的 TCP Socket 连接测试。\n\n" +
                       "• 延迟分级：\n" +
                       "  - 绿色 (<180ms)：极速节点\n" +
                       "  - 黄色 (180ms-400ms)：正常可用\n" +
                       "  - 橙色 (>400ms)：高延迟\n" +
                       "  - 红色 (超时/不可用)：无法连接或端口被拦截\n\n" +
                       "• 过滤失效：测速完成后，点击「过滤失效」按钮或在导出页面勾选「仅导出测速正常节点」，工具将自动剔除超时与无效服务器，保证导出的节点全部可用！",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun GuideCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
