package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.components.NodeDetailDialog
import com.example.ui.components.QrCodeDialog
import com.example.ui.screens.ConvertScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.GuideScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

enum class MainTab(val title: String, val testTag: String) {
    CONVERT("节点转换", "nav_tab_convert"),
    EXPORT("批量导出", "nav_tab_export"),
    GUIDE("使用指南", "nav_tab_guide")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(MainTab.CONVERT) }

    // Display info or error messages via Snackbar
    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissInfoMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            viewModel.dismissErrorMessage()
        }
    }

    val copyToClipboard: (String, String) -> Unit = { text, successNotice ->
        clipboardManager.setText(AnnotatedString(text))
        coroutineScope.launch {
            snackbarHostState.showSnackbar(successNotice)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Clash2V2Ray",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Clash 配置转 V2RayN 节点转换工具",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (currentTab == MainTab.CONVERT) {
                        IconButton(
                            onClick = { viewModel.loadSample() },
                            modifier = Modifier.testTag("top_bar_sample_btn")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "重置示例")
                        }
                        IconButton(
                            onClick = { viewModel.clearAll() },
                            modifier = Modifier.testTag("top_bar_clear_btn")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "清空所有")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("main_bottom_nav"),
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.CONVERT,
                    onClick = { currentTab = MainTab.CONVERT },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.CONVERT) Icons.Filled.SwapHoriz else Icons.Outlined.SwapHoriz,
                            contentDescription = MainTab.CONVERT.title
                        )
                    },
                    label = { Text(MainTab.CONVERT.title) },
                    modifier = Modifier.testTag(MainTab.CONVERT.testTag)
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.EXPORT,
                    onClick = { currentTab = MainTab.EXPORT },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.EXPORT) Icons.Filled.FileDownload else Icons.Outlined.FileDownload,
                            contentDescription = MainTab.EXPORT.title
                        )
                    },
                    label = { Text(MainTab.EXPORT.title) },
                    modifier = Modifier.testTag(MainTab.EXPORT.testTag)
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.GUIDE,
                    onClick = { currentTab = MainTab.GUIDE },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == MainTab.GUIDE) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                            contentDescription = MainTab.GUIDE.title
                        )
                    },
                    label = { Text(MainTab.GUIDE.title) },
                    modifier = Modifier.testTag(MainTab.GUIDE.testTag)
                )
            }
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.CONVERT -> ConvertScreen(
                    viewModel = viewModel,
                    onCopyText = copyToClipboard
                )
                MainTab.EXPORT -> ExportScreen(
                    viewModel = viewModel,
                    onCopyText = copyToClipboard
                )
                MainTab.GUIDE -> GuideScreen()
            }

            // QR Code Dialog
            uiState.selectedNodeForQr?.let { node ->
                QrCodeDialog(
                    node = node,
                    onDismiss = { viewModel.selectNodeForQr(null) },
                    onCopyLink = { link ->
                        copyToClipboard(link, "已复制 ${node.name} 链接")
                    }
                )
            }

            // Node Detail Dialog
            uiState.selectedNodeForDetail?.let { node ->
                NodeDetailDialog(
                    node = node,
                    onDismiss = { viewModel.selectNodeForDetail(null) },
                    onCopyLink = { link ->
                        copyToClipboard(link, "已复制 ${node.name} 链接")
                    }
                )
            }
        }
    }
}
