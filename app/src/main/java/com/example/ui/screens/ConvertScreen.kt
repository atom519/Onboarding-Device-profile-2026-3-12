package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PingStatus
import com.example.model.ProxyNode
import com.example.model.ProxyType
import com.example.ui.FilterStatus
import com.example.ui.MainViewModel
import com.example.ui.SortOption
import com.example.ui.components.NodeCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvertScreen(
    viewModel: MainViewModel,
    onCopyText: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var isInputExpanded by remember { mutableStateOf(false) }
    var selectedInputTab by remember { mutableIntStateOf(0) } // 0: Text, 1: Subscription URL

    val filteredNodes = remember(
        uiState.nodes,
        uiState.filterStatus,
        uiState.selectedTypeFilter,
        uiState.searchQuery,
        uiState.sortOption
    ) {
        viewModel.getFilteredAndSortedNodes()
    }

    val totalCount = uiState.nodes.size
    val onlineCount = uiState.nodes.count { it.pingStatus == PingStatus.ONLINE }
    val failedCount = uiState.nodes.count { it.pingStatus == PingStatus.TIMEOUT || it.pingStatus == PingStatus.ERROR }
    val untestedCount = uiState.nodes.count { it.pingStatus == PingStatus.UNTESTED }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "节点状态概览",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "共转换 $totalCount 个节点",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { isInputExpanded = !isInputExpanded },
                            modifier = Modifier.testTag("toggle_input_section")
                        ) {
                            Icon(
                                imageVector = if (isInputExpanded) Icons.Default.ExpandLess else Icons.Default.Tune,
                                contentDescription = "输入配置"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatPill("总节点", totalCount.toString(), MaterialTheme.colorScheme.primary)
                        StatPill("可用节点", onlineCount.toString(), com.example.ui.theme.LatencyFast)
                        StatPill("失效/超时", failedCount.toString(), com.example.ui.theme.LatencyOffline)
                        StatPill("未测速", untestedCount.toString(), com.example.ui.theme.LatencyUntested)
                    }
                }
            }
        }

        // Expandable Input Section
        item {
            AnimatedVisibility(
                visible = isInputExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        PrimaryTabRow(selectedTabIndex = selectedInputTab) {
                            Tab(
                                selected = selectedInputTab == 0,
                                onClick = { selectedInputTab = 0 },
                                text = { Text("配置文本 / YAML") }
                            )
                            Tab(
                                selected = selectedInputTab == 1,
                                onClick = { selectedInputTab = 1 },
                                text = { Text("订阅链接拉取") }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (selectedInputTab == 0) {
                            OutlinedTextField(
                                value = uiState.inputText,
                                onValueChange = { viewModel.updateInputText(it) },
                                label = { Text("粘贴 Clash YAML 配置或 proxies 列表") },
                                placeholder = { Text("proxies:\n  - name: ...\n    type: vmess\n    server: ...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 140.dp, max = 220.dp)
                                    .testTag("clash_input_textfield"),
                                maxLines = 10,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row {
                                    TextButton(
                                        onClick = { viewModel.loadSample(minimal = false) },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("载入完整示例", fontSize = 12.sp)
                                    }
                                    TextButton(
                                        onClick = { viewModel.loadSample(minimal = true) },
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("精简示例", fontSize = 12.sp)
                                    }
                                }

                                Button(
                                    onClick = {
                                        viewModel.convertInput()
                                        isInputExpanded = false
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("action_convert")
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("解析并转换")
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = uiState.urlInput,
                                onValueChange = { viewModel.updateUrlInput(it) },
                                label = { Text("输入 Clash 订阅链接 (http/https)") },
                                placeholder = { Text("https://example.com/api/v1/client/subscribe?token=...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("url_input_textfield"),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.fetchSubscription(uiState.urlInput)
                                    isInputExpanded = false
                                },
                                enabled = !uiState.isLoadingUrl,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("fetch_url_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (uiState.isLoadingUrl) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("正在下载配置…")
                                } else {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("拉取并转换配置")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Speed Test Progress Banner
        if (uiState.isTestingPing) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "正在测速: ${uiState.testedCount}/${uiState.totalCount}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            TextButton(
                                onClick = { viewModel.stopPingTest() },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("停止")
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val progress = if (uiState.totalCount > 0) uiState.testedCount.toFloat() / uiState.totalCount else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Main Actions Row: Ping Test & Filter Invalid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.startPingTest() },
                    enabled = !uiState.isTestingPing && totalCount > 0,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_start_ping"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "批量测速",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (uiState.isTestingPing) "测速中…" else "连通性测速")
                }

                FilledTonalButton(
                    onClick = { viewModel.removeInvalidNodes() },
                    enabled = failedCount > 0,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("action_filter_invalid"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (failedCount > 0) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (failedCount > 0) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = "过滤失效",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("过滤失效($failedCount)")
                }
            }
        }

        // Filter and Search Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Status Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.filterStatus == FilterStatus.ALL,
                        onClick = { viewModel.setFilterStatus(FilterStatus.ALL) },
                        label = { Text("全部 ($totalCount)") },
                        modifier = Modifier.testTag("filter_chip_all")
                    )
                    FilterChip(
                        selected = uiState.filterStatus == FilterStatus.ONLY_ONLINE,
                        onClick = { viewModel.setFilterStatus(FilterStatus.ONLY_ONLINE) },
                        label = { Text("仅可用 ($onlineCount)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = com.example.ui.theme.LatencyFast,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("filter_chip_online")
                    )
                    FilterChip(
                        selected = uiState.filterStatus == FilterStatus.ONLY_FAILED,
                        onClick = { viewModel.setFilterStatus(FilterStatus.ONLY_FAILED) },
                        label = { Text("仅失效 ($failedCount)") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                tint = com.example.ui.theme.LatencyOffline,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("filter_chip_failed")
                    )
                }

                // Protocol Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedTypeFilter == null,
                            onClick = { viewModel.setTypeFilter(null) },
                            label = { Text("全部协议") }
                        )
                    }
                    items(
                        listOf(
                            ProxyType.VMESS,
                            ProxyType.VLESS,
                            ProxyType.SHADOWSOCKS,
                            ProxyType.TROJAN,
                            ProxyType.HYSTERIA2,
                            ProxyType.TUIC
                        )
                    ) { type ->
                        FilterChip(
                            selected = uiState.selectedTypeFilter == type,
                            onClick = {
                                viewModel.setTypeFilter(if (uiState.selectedTypeFilter == type) null else type)
                            },
                            label = { Text(type.label) }
                        )
                    }
                }

                // Search & Sort Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("搜索节点名或 IP", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("search_textfield")
                    )

                    var isSortMenuOpen by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { isSortMenuOpen = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(uiState.sortOption.label, fontSize = 12.sp)
                        }

                        DropdownMenu(
                            expanded = isSortMenuOpen,
                            onDismissRequest = { isSortMenuOpen = false }
                        ) {
                            SortOption.values().forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.label) },
                                    onClick = {
                                        viewModel.setSortOption(opt)
                                        isSortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "节点列表 (${filteredNodes.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                TextButton(
                    onClick = {
                        val links = viewModel.getExportLinks(onlyValid = false)
                        onCopyText(links, "已复制当前列表所有节点链接")
                    },
                    enabled = filteredNodes.isNotEmpty(),
                    modifier = Modifier.testTag("quick_copy_all_btn")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("复制当前全部", fontSize = 12.sp)
                }
            }
        }

        // Empty State or List
        if (filteredNodes.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterListOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (totalCount == 0) "暂无节点，请粘贴 Clash 配置并点击转换" else "当前筛选条件下无节点",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (totalCount == 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadSample() }) {
                                Text("载入示例测试")
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredNodes, key = { it.id }) { node ->
                NodeCard(
                    node = node,
                    onCopyLink = { link ->
                        onCopyText(link, "已复制 ${node.name} 链接")
                    },
                    onShowQr = { viewModel.selectNodeForQr(it) },
                    onShowDetail = { viewModel.selectNodeForDetail(it) },
                    onTestPing = { viewModel.testSingleNode(it) },
                    onDelete = { viewModel.deleteNode(it) }
                )
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
