package com.example.ui

import com.example.util.Base64Util
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.PingStatus
import com.example.model.ProxyNode
import com.example.model.ProxyType
import com.example.parser.ClashParser
import com.example.tester.NodeConnectivityTester
import com.example.util.SampleClashConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

enum class FilterStatus {
    ALL,
    ONLY_ONLINE,
    ONLY_FAILED
}

enum class SortOption(val label: String) {
    ORIGINAL("默认顺序"),
    LATENCY_ASC("延迟由低到高"),
    NAME_ASC("节点名称 A-Z"),
    TYPE("协议类型")
}

data class UiState(
    val inputText: String = SampleClashConfig.SAMPLE_YAML,
    val urlInput: String = "",
    val isLoadingUrl: Boolean = false,
    val nodes: List<ProxyNode> = emptyList(),
    val isTestingPing: Boolean = false,
    val testedCount: Int = 0,
    val totalCount: Int = 0,
    val filterStatus: FilterStatus = FilterStatus.ALL,
    val selectedTypeFilter: ProxyType? = null,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.ORIGINAL,
    val selectedNodeForQr: ProxyNode? = null,
    val selectedNodeForDetail: ProxyNode? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val testTimeoutMs: Int = 2500
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var pingJob: Job? = null
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    init {
        // Automatically convert the initial sample on launch
        convertInput()
    }

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text, errorMessage = null) }
    }

    fun updateUrlInput(url: String) {
        _uiState.update { it.copy(urlInput = url, errorMessage = null) }
    }

    fun loadSample(minimal: Boolean = false) {
        val sample = if (minimal) SampleClashConfig.SAMPLE_MINIMAL else SampleClashConfig.SAMPLE_YAML
        _uiState.update { it.copy(inputText = sample, errorMessage = null) }
        convertInput()
    }

    fun convertInput() {
        val input = _uiState.value.inputText
        if (input.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请输入或粘贴 Clash 配置文件内容") }
            return
        }

        stopPingTest()
        val result = ClashParser.parse(input)
        result.fold(
            onSuccess = { parsedNodes ->
                _uiState.update {
                    it.copy(
                        nodes = parsedNodes,
                        errorMessage = null,
                        infoMessage = "成功转换 ${parsedNodes.size} 个节点",
                        filterStatus = FilterStatus.ALL,
                        selectedTypeFilter = null,
                        testedCount = 0,
                        totalCount = parsedNodes.size
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = error.message ?: "解析失败，请检查配置文件格式"
                    )
                }
            }
        )
    }

    fun fetchSubscription(url: String) {
        val trimmedUrl = url.trim()
        if (!trimmedUrl.startsWith("http://", ignoreCase = true) && !trimmedUrl.startsWith("https://", ignoreCase = true)) {
            _uiState.update { it.copy(errorMessage = "请输入有效的 http:// 或 https:// 订阅链接") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingUrl = true, errorMessage = null) }
            try {
                val request = Request.Builder()
                    .url(trimmedUrl)
                    .header("User-Agent", "ClashforWindows/0.20.39")
                    .build()

                val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    httpClient.newCall(request).execute()
                }

                if (!response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isLoadingUrl = false,
                            errorMessage = "下载订阅失败: HTTP ${response.code}"
                        )
                    }
                    return@launch
                }

                val body = response.body?.string() ?: ""
                if (body.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isLoadingUrl = false,
                            errorMessage = "订阅链接返回空内容"
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        inputText = body,
                        isLoadingUrl = false
                    )
                }
                convertInput()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingUrl = false,
                        errorMessage = "请求订阅失败: ${e.message ?: "网络错误"}"
                    )
                }
            }
        }
    }

    fun startPingTest() {
        val currentNodes = _uiState.value.nodes
        if (currentNodes.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "当前没有可测速的节点，请先转换配置") }
            return
        }

        stopPingTest()

        // Reset status to TESTING
        _uiState.update { state ->
            state.copy(
                isTestingPing = true,
                testedCount = 0,
                totalCount = currentNodes.size,
                nodes = currentNodes.map { it.copy(pingStatus = PingStatus.TESTING, latencyMs = null, errorMessage = null) },
                infoMessage = "正在并发测速 (${currentNodes.size} 个节点)…"
            )
        }

        val timeout = _uiState.value.testTimeoutMs

        pingJob = viewModelScope.launch {
            NodeConnectivityTester.testAllNodes(
                nodes = currentNodes,
                concurrency = 12,
                timeoutMs = timeout,
                onNodeTested = { nodeId, status, latencyMs, error ->
                    _uiState.update { state ->
                        val updated = state.nodes.map { node ->
                            if (node.id == nodeId) {
                                node.copy(pingStatus = status, latencyMs = latencyMs, errorMessage = error)
                            } else {
                                node
                            }
                        }
                        state.copy(nodes = updated)
                    }
                },
                onProgress = { tested, total ->
                    _uiState.update {
                        it.copy(testedCount = tested, totalCount = total)
                    }
                }
            )

            val finalState = _uiState.value
            val onlineCount = finalState.nodes.count { it.pingStatus == PingStatus.ONLINE }
            val failCount = finalState.nodes.size - onlineCount

            _uiState.update {
                it.copy(
                    isTestingPing = false,
                    infoMessage = "测速完成：可用 $onlineCount 个，失效/超时 $failCount 个"
                )
            }
        }
    }

    fun stopPingTest() {
        pingJob?.cancel()
        pingJob = null
        if (_uiState.value.isTestingPing) {
            _uiState.update { it.copy(isTestingPing = false, infoMessage = "测速已停止") }
        }
    }

    fun testSingleNode(nodeId: String) {
        val node = _uiState.value.nodes.find { it.id == nodeId } ?: return
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    nodes = state.nodes.map {
                        if (it.id == nodeId) it.copy(pingStatus = PingStatus.TESTING) else it
                    }
                )
            }
            val (status, latency, error) = NodeConnectivityTester.testNode(node.server, node.port, _uiState.value.testTimeoutMs)
            _uiState.update { state ->
                state.copy(
                    nodes = state.nodes.map {
                        if (it.id == nodeId) it.copy(pingStatus = status, latencyMs = latency, errorMessage = error) else it
                    }
                )
            }
        }
    }

    /**
     * Filter invalid nodes: permanently remove failed/timeout nodes from current list,
     * or toggle filter status to ONLY_ONLINE.
     */
    fun removeInvalidNodes() {
        val currentNodes = _uiState.value.nodes
        val validNodes = currentNodes.filter { it.pingStatus == PingStatus.ONLINE }
        val removedCount = currentNodes.size - validNodes.size

        if (removedCount == 0) {
            _uiState.update { it.copy(infoMessage = "没有需要过滤的失效节点") }
            return
        }

        _uiState.update {
            it.copy(
                nodes = validNodes,
                totalCount = validNodes.size,
                infoMessage = "已自动过滤移除 $removedCount 个失效节点，保留 ${validNodes.size} 个有效节点"
            )
        }
    }

    fun deleteNode(nodeId: String) {
        _uiState.update { state ->
            val updated = state.nodes.filterNot { it.id == nodeId }
            state.copy(nodes = updated, totalCount = updated.size)
        }
    }

    fun setFilterStatus(status: FilterStatus) {
        _uiState.update { it.copy(filterStatus = status) }
    }

    fun setTypeFilter(type: ProxyType?) {
        _uiState.update { it.copy(selectedTypeFilter = type) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun selectNodeForQr(node: ProxyNode?) {
        _uiState.update { it.copy(selectedNodeForQr = node) }
    }

    fun selectNodeForDetail(node: ProxyNode?) {
        _uiState.update { it.copy(selectedNodeForDetail = node) }
    }

    fun dismissInfoMessage() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun dismissErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearAll() {
        stopPingTest()
        _uiState.update {
            it.copy(
                inputText = "",
                nodes = emptyList(),
                testedCount = 0,
                totalCount = 0,
                errorMessage = null,
                infoMessage = "已清空配置和节点"
            )
        }
    }

    fun getFilteredAndSortedNodes(): List<ProxyNode> {
        val state = _uiState.value
        return state.nodes
            .filter { node ->
                // Filter by status
                when (state.filterStatus) {
                    FilterStatus.ALL -> true
                    FilterStatus.ONLY_ONLINE -> node.pingStatus == PingStatus.ONLINE
                    FilterStatus.ONLY_FAILED -> node.pingStatus == PingStatus.TIMEOUT || node.pingStatus == PingStatus.ERROR
                }
            }
            .filter { node ->
                // Filter by protocol
                state.selectedTypeFilter == null || node.type == state.selectedTypeFilter
            }
            .filter { node ->
                // Search query
                if (state.searchQuery.isBlank()) true
                else {
                    node.name.contains(state.searchQuery, ignoreCase = true) ||
                    node.server.contains(state.searchQuery, ignoreCase = true) ||
                    node.type.label.contains(state.searchQuery, ignoreCase = true)
                }
            }
            .let { list ->
                // Sort
                when (state.sortOption) {
                    SortOption.ORIGINAL -> list
                    SortOption.LATENCY_ASC -> list.sortedWith(
                        compareBy<ProxyNode> {
                            when (it.pingStatus) {
                                PingStatus.ONLINE -> it.latencyMs ?: Long.MAX_VALUE
                                PingStatus.UNTESTED -> 999998L
                                PingStatus.TESTING -> 999999L
                                PingStatus.TIMEOUT -> 1000000L
                                PingStatus.ERROR -> 1000001L
                            }
                        }
                    )
                    SortOption.NAME_ASC -> list.sortedBy { it.name }
                    SortOption.TYPE -> list.sortedBy { it.type.name }
                }
            }
    }

    fun getExportLinks(onlyValid: Boolean = false): String {
        val nodesToExport = if (onlyValid) {
            _uiState.value.nodes.filter { it.pingStatus == PingStatus.ONLINE }
        } else {
            getFilteredAndSortedNodes()
        }
        return nodesToExport.joinToString("\n") { it.v2rayLink }
    }

    fun getBase64Subscription(onlyValid: Boolean = false): String {
        val links = getExportLinks(onlyValid)
        return Base64Util.encode(links)
    }
}
