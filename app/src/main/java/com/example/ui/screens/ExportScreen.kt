package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PingStatus
import com.example.ui.MainViewModel

@Composable
fun ExportScreen(
    viewModel: MainViewModel,
    onCopyText: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var onlyValid by remember { mutableStateOf(false) }
    var useBase64 by remember { mutableStateOf(false) }

    val totalNodes = uiState.nodes.size
    val validNodes = uiState.nodes.count { it.pingStatus == PingStatus.ONLINE }
    val failedNodes = uiState.nodes.count { it.pingStatus == PingStatus.TIMEOUT || it.pingStatus == PingStatus.ERROR }

    val exportContent = remember(uiState.nodes, onlyValid, useBase64) {
        if (useBase64) {
            viewModel.getBase64Subscription(onlyValid)
        } else {
            viewModel.getExportLinks(onlyValid)
        }
    }

    val targetCount = if (onlyValid) validNodes else totalNodes

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "导出与订阅配置",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "当前总节点 $totalNodes 个，可用节点 $validNodes 个，失效/超时 $failedNodes 个",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Toggles Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "导出选项",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("仅导出测速正常节点", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        Text(
                            text = if (validNodes > 0) "自动过滤不可用与超时节点，共 $validNodes 个" else "提示：尚未测速或无可用节点",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = onlyValid,
                        onCheckedChange = { onlyValid = it },
                        modifier = Modifier.testTag("switch_only_valid")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Base64 订阅格式", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        Text(
                            text = if (useBase64) "输出单行 Base64 订阅编码串" else "输出多行原生 URI (vmess://, vless://...)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useBase64,
                        onCheckedChange = { useBase64 = it },
                        modifier = Modifier.testTag("switch_use_base64")
                    )
                }
            }
        }

        // Preview Area
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "内容预览 ($targetCount 个节点)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(
                        onClick = {
                            onCopyText(exportContent, "已复制导出内容")
                        },
                        enabled = exportContent.isNotEmpty(),
                        modifier = Modifier.testTag("preview_copy_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "复制")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 220.dp)
                ) {
                    Text(
                        text = if (exportContent.isNotEmpty()) exportContent else "（暂无导出内容）",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }

        // Action Buttons
        Button(
            onClick = {
                onCopyText(exportContent, "已复制全部节点链接到剪贴板，可直接在 V2RayN 粘贴导入")
            },
            enabled = exportContent.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("action_copy_all_btn"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("一键复制到剪贴板 (导入 V2RayN)", fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = {
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, exportContent)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "分享 V2RayN 节点列表")
                context.startActivity(shareIntent)
            },
            enabled = exportContent.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("action_share_btn"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("系统分享 (发送至电脑/其他App)")
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
