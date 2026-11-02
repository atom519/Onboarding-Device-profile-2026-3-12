package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PingStatus
import com.example.model.ProxyNode
import com.example.model.ProxyType

@Composable
fun ProtocolBadge(type: ProxyType, modifier: Modifier = Modifier) {
    Surface(
        color = type.badgeColor.copy(alpha = 0.15f),
        contentColor = type.badgeColor,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = type.label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun LatencyBadge(
    status: PingStatus,
    latencyMs: Long?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val (badgeText, badgeColor) = when (status) {
        PingStatus.UNTESTED -> "未测速" to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        PingStatus.TESTING -> "测速中" to MaterialTheme.colorScheme.primary
        PingStatus.ONLINE -> "${latencyMs ?: 0}ms" to com.example.ui.theme.LatencyFast
        PingStatus.TIMEOUT -> "超时" to com.example.ui.theme.LatencyOffline
        PingStatus.ERROR -> "不可用" to com.example.ui.theme.LatencyOffline
    }

    Surface(
        color = badgeColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        badgeColor.copy(
                            alpha = if (status == PingStatus.TESTING) alphaAnim else 1f
                        )
                    )
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = badgeText,
                color = badgeColor,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}

@Composable
fun NodeCard(
    node: ProxyNode,
    onCopyLink: (String) -> Unit,
    onShowQr: (ProxyNode) -> Unit,
    onShowDetail: (ProxyNode) -> Unit,
    onTestPing: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("node_card_${node.id}")
            .clickable { onShowDetail(node) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Protocol Badge, Latency, and Single Ping Test
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ProtocolBadge(type = node.type)
                    Spacer(modifier = Modifier.width(8.dp))
                    LatencyBadge(status = node.pingStatus, latencyMs = node.latencyMs)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onTestPing(node.id) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("ping_single_${node.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "单节点测速",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { onShowQr(node) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("qr_button_${node.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "查看二维码",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Node Name
            Text(
                text = node.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Host & Port
            Text(
                text = "${node.server}:${node.port}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (node.errorMessage != null && (node.pingStatus == PingStatus.ERROR || node.pingStatus == PingStatus.TIMEOUT)) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "原因: ${node.errorMessage}",
                    style = MaterialTheme.typography.labelSmall,
                    color = com.example.ui.theme.LatencyOffline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Actions: Copy Link, Details, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { onCopyLink(node.v2rayLink) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("copy_link_${node.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制链接",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("复制 V2RayN 链接", fontSize = 12.sp)
                }

                Row {
                    TextButton(
                        onClick = { onShowDetail(node) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("详情", fontSize = 12.sp)
                    }

                    IconButton(
                        onClick = { onDelete(node.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "删除节点",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
