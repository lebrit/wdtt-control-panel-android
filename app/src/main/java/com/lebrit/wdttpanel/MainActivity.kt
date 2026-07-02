package com.lebrit.wdttpanel

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lebrit.wdttpanel.data.NotificationHelper
import com.lebrit.wdttpanel.data.PanelSyncWorker
import com.lebrit.wdttpanel.data.ProfileTools
import com.lebrit.wdttpanel.model.AppTab
import com.lebrit.wdttpanel.model.AppUiState
import com.lebrit.wdttpanel.model.ConnectionStatus
import com.lebrit.wdttpanel.model.CreateUserMode
import com.lebrit.wdttpanel.model.GeneratedLinks
import com.lebrit.wdttpanel.model.HashMode
import com.lebrit.wdttpanel.model.LogsMeta
import com.lebrit.wdttpanel.model.OverviewSummary
import com.lebrit.wdttpanel.model.QwdttProfile
import com.lebrit.wdttpanel.model.ServerProfile
import com.lebrit.wdttpanel.model.UserBulkAction
import com.lebrit.wdttpanel.model.UserSummary
import com.lebrit.wdttpanel.ui.WdttViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.ensureChannel(this)
        PanelSyncWorker.schedule(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
        val launchImport = importTextFromIntent(intent)
        setContent {
            WdttTheme {
                val viewModel: WdttViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(launchImport) {
                    if (!launchImport.isNullOrBlank()) viewModel.importProfiles(launchImport)
                }
                WdttApp(state = state, viewModel = viewModel)
            }
        }
    }

    private fun importTextFromIntent(intent: Intent?): String? =
        when (intent?.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }
}

@Composable
private fun WdttTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFF7DD3FC),
            secondary = Color(0xFF5EEAD4),
            tertiary = Color(0xFFFBBF24),
            background = Color(0xFF0F172A),
            surface = Color(0xFF111827),
            surfaceVariant = Color(0xFF1F2937),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF2563EB),
            secondary = Color(0xFF0F766E),
            tertiary = Color(0xFFD97706),
            background = Color(0xFFF4F7FB),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE5E7EB),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WdttApp(state: AppUiState, viewModel: WdttViewModel) {
    var showServerDialog by remember { mutableStateOf(false) }
    var editingServer by remember { mutableStateOf<ServerProfile?>(null) }
    var showCreateUser by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message, state.error) {
        val text = state.error ?: state.message
        if (!text.isNullOrBlank()) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("WDTT Panel", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = state.activeServer?.name ?: "Нет активного сервера",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    ServerSwitcher(state = state, onSwitch = viewModel::switchServer)
                    IconButton(
                        onClick = viewModel::refresh,
                        enabled = state.activeServer != null && !state.loading,
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                    IconButton(
                        onClick = {
                            editingServer = null
                            showServerDialog = true
                        },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить сервер")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                tabItems().forEach { item ->
                    NavigationBarItem(
                        selected = state.selectedTab == item.tab,
                        onClick = { viewModel.selectTab(item.tab) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, maxLines = 1) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.servers.isEmpty() -> EmptyServers(
                    onAdd = {
                        editingServer = null
                        showServerDialog = true
                    },
                )
                state.selectedTab == AppTab.Dashboard -> DashboardScreen(
                    state = state,
                    onSwitch = viewModel::switchServer,
                    onService = viewModel::serviceAction,
                    onAddServer = {
                        editingServer = null
                        showServerDialog = true
                    },
                )
                state.selectedTab == AppTab.Users -> UsersScreen(
                    users = state.users,
                    onCreate = { showCreateUser = true },
                    onDelete = viewModel::deleteUser,
                    onUnbind = viewModel::unbindUser,
                    onResetTraffic = viewModel::resetTraffic,
                    onSetEnabled = viewModel::setUserEnabled,
                    onBulk = viewModel::bulkUserAction,
                )
                state.selectedTab == AppTab.Profiles -> ProfilesScreen(
                    state = state,
                    onExportProfile = viewModel::exportProfile,
                    onExportSubscription = viewModel::exportSubscription,
                    onImport = viewModel::importProfiles,
                    onLoadHashes = viewModel::loadVkHashes,
                    onAddHashes = viewModel::addVkHashes,
                    onDeleteHash = viewModel::deleteVkHash,
                )
                state.selectedTab == AppTab.Logs -> LogsScreen(
                    lines = state.logs,
                    meta = state.logsMeta,
                    onLoad = viewModel::loadLogs,
                )
                state.selectedTab == AppTab.Servers -> ServersScreen(
                    state = state,
                    onAdd = {
                        editingServer = null
                        showServerDialog = true
                    },
                    onEdit = {
                        editingServer = it
                        showServerDialog = true
                    },
                    onDelete = viewModel::deleteServer,
                    onSwitch = viewModel::switchServer,
                )
            }

            if (state.loading) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 3.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Синхронизация", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }

    if (showServerDialog) {
        ServerDialog(
            server = editingServer,
            onDismiss = { showServerDialog = false },
            onSave = { id, name, url, username, password, insecure ->
                showServerDialog = false
                viewModel.saveServer(id, name, url, username, password, insecure)
            },
        )
    }
    if (showCreateUser) {
        CreateUserDialog(
            onDismiss = { showCreateUser = false },
            onCreateManual = { label, password, hash, ports, days, unlimited, disabled ->
                showCreateUser = false
                viewModel.createUser(label, password, hash, ports, days, unlimited, disabled)
            },
            onCreateAuto = { label ->
                showCreateUser = false
                viewModel.createAutoUser(label)
            },
            onCreateBulk = { count, hash, hashMode, labelPrefix, ports, days, unlimited, disabled ->
                showCreateUser = false
                viewModel.createUsersBulk(count, hash, hashMode, labelPrefix, ports, days, unlimited, disabled)
            },
        )
    }
    state.generatedLinks?.let { links ->
        GeneratedLinksDialog(links = links, onDismiss = viewModel::clearGeneratedLinks)
    }
}

@Composable
private fun ServerSwitcher(state: AppUiState, onSwitch: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    if (state.servers.size < 2) return
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Выбрать сервер")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.servers.forEach { server ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(server.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                server.baseUrl,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSwitch(server.id)
                    },
                    leadingIcon = {
                        Icon(
                            if (server.id == state.activeServerId) Icons.Default.CheckCircle else Icons.Default.Storage,
                            contentDescription = null,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun EmptyServers(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ) {
            Icon(
                Icons.Default.Storage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(18.dp)
                    .size(38.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text("Серверы не добавлены", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Добавьте панель WDTT, чтобы видеть состояние сервиса, пользователей и журналы.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Добавить сервер")
        }
    }
}

@Composable
private fun DashboardScreen(
    state: AppUiState,
    onSwitch: (String) -> Unit,
    onService: (String) -> Unit,
    onAddServer: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ServerHero(
                activeServer = state.activeServer,
                servers = state.servers,
                overview = state.overview,
                onSwitch = onSwitch,
                onAddServer = onAddServer,
            )
        }
        item { MetricGrid(overview = state.overview) }
        item { ResourcePanel(overview = state.overview) }
        item { ServicePanel(overview = state.overview, onService = onService) }
        item { HealthPanel(overview = state.overview) }
    }
}

@Composable
private fun ServerHero(
    activeServer: ServerProfile?,
    servers: List<ServerProfile>,
    overview: OverviewSummary?,
    onSwitch: (String) -> Unit,
    onAddServer: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = CircleShape,
                    color = if (overview?.serviceActive == true) Color(0xFF16A34A) else MaterialTheme.colorScheme.tertiary,
                ) {
                    Icon(
                        if (overview?.serviceActive == true) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .padding(14.dp)
                            .size(28.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        activeServer?.name ?: "WDTT Panel",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        activeServer?.baseUrl ?: "Сервер не выбран",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ConnectionStatusChip(activeServer?.lastStatus ?: ConnectionStatus.Unknown)
            }

            FlowStatusRow {
                if (!activeServer?.version.isNullOrBlank()) {
                    StatusPill(Icons.Default.Security, "v${activeServer?.version}")
                }
                if (!overview?.publicHost.isNullOrBlank()) {
                    StatusPill(Icons.Default.Dns, overview?.publicHost.orEmpty())
                }
                if (!overview?.nat.isNullOrBlank()) {
                    StatusPill(Icons.Default.Tune, overview?.nat.orEmpty())
                }
                if (!overview?.uptime.isNullOrBlank()) {
                    StatusPill(Icons.Default.History, overview?.uptime.orEmpty())
                }
                if ((activeServer?.lastCheckedAt ?: 0L) > 0) {
                    StatusPill(Icons.Default.Refresh, timeLabel(activeServer?.lastCheckedAt ?: 0L))
                }
                if ((activeServer?.lastLatencyMs ?: 0L) > 0) {
                    StatusPill(Icons.Default.Speed, "${activeServer?.lastLatencyMs} ms")
                }
            }

            if (servers.size > 1) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(servers, key = { it.id }) { server ->
                        FilterChip(
                            selected = server.id == activeServer?.id,
                            onClick = { onSwitch(server.id) },
                            label = { Text(server.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = {
                                Icon(
                                    if (server.id == activeServer?.id) Icons.Default.CheckCircle else Icons.Default.Storage,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                    item {
                        OutlinedButton(onClick = onAddServer) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Сервер")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricGrid(overview: OverviewSummary?) {
    val metrics = listOf(
        Metric("Активные", overview?.let { "${it.active}/${it.total}" } ?: "-", Icons.Default.Wifi),
        Metric("Пользователи", overview?.users?.toString() ?: "-", Icons.Default.Groups),
        Metric("Устройства", overview?.let { "${it.onlineDevices}/${it.devices}" } ?: "-", Icons.Default.Devices),
        Metric("TX", overview?.let { "${it.uploadGb} GB" } ?: "-", Icons.Default.ArrowUpward),
        Metric("RX", overview?.let { "${it.downloadGb} GB" } ?: "-", Icons.Default.ArrowDownward),
        Metric("NAT", overview?.nat?.ifBlank { "-" } ?: "-", Icons.Default.Dns),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { metric ->
                    MetricTile(metric = metric, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricTile(metric: Metric, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
            ) {
                Icon(
                    metric.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(metric.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(metric.value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ResourcePanel(overview: OverviewSummary?) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionTitle(Icons.Default.Speed, "Ресурсы")
            ResourceMeter("CPU", overview?.cpuPercent ?: "-", Icons.Default.Speed)
            ResourceMeter("RAM", overview?.memoryPercent ?: "-", Icons.Default.Memory)
            ResourceMeter("Диск", overview?.diskPercent ?: "-", Icons.Default.Storage)
        }
    }
}

@Composable
private fun ResourceMeter(label: String, rawPercent: String, icon: ImageVector) {
    val fraction = percentFraction(rawPercent)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(percentLabel(rawPercent), style = MaterialTheme.typography.labelMedium)
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        )
    }
}

@Composable
private fun ServicePanel(overview: OverviewSummary?, onService: (String) -> Unit) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(Icons.Default.PowerSettingsNew, "Сервис")
                Spacer(Modifier.weight(1f))
                AssistChip(
                    onClick = {},
                    label = { Text(if (overview?.serviceActive == true) "Работает" else "Остановлен") },
                    leadingIcon = {
                        Icon(
                            if (overview?.serviceActive == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                )
            }
            FlowStatusRow {
                FilledTonalButton(onClick = { onService("start") }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Запуск")
                }
                FilledTonalButton(onClick = { onService("restart") }) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Рестарт")
                }
                OutlinedButton(onClick = { onService("stop") }) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Стоп")
                }
            }
        }
    }
}

@Composable
private fun HealthPanel(overview: OverviewSummary?) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(Icons.Default.Security, "Проверки")
            FlowStatusRow {
                HealthPill("systemd", overview?.serviceExists == true)
                HealthPill("wdtt-server", overview?.binaryExists == true)
                HealthPill("IPv4 forwarding", overview?.ipForward == "1")
                if (!overview?.tlsMode.isNullOrBlank()) StatusPill(Icons.Default.Key, overview?.tlsMode.orEmpty())
                if ((overview?.httpsPort ?: 443) != 443) StatusPill(Icons.Default.Security, "HTTPS ${overview?.httpsPort}")
            }
        }
    }
}

@Composable
private fun UsersScreen(
    users: List<UserSummary>,
    onCreate: () -> Unit,
    onDelete: (UserSummary) -> Unit,
    onUnbind: (UserSummary) -> Unit,
    onResetTraffic: (UserSummary) -> Unit,
    onSetEnabled: (UserSummary, Boolean) -> Unit,
    onBulk: (UserBulkAction, List<String>, String) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(UserFilter.All) }
    var sort by remember { mutableStateOf(UserSort.Name) }
    var selectedPasswords by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDelete by remember { mutableStateOf<UserSummary?>(null) }
    var pendingBulk by remember { mutableStateOf<UserBulkAction?>(null) }
    var bulkDays by remember { mutableStateOf("30") }

    LaunchedEffect(users) {
        val existing = users.map { it.password }.toSet()
        selectedPasswords = selectedPasswords.filter { it in existing }.toSet()
    }

    val filteredUsers = remember(users, search, filter, sort) {
        users
            .filter { it.matches(search) }
            .filter { user ->
                when (filter) {
                    UserFilter.All -> true
                    UserFilter.Online -> user.connected
                    UserFilter.Bound -> user.bound
                    UserFilter.Disabled -> user.deactivated
                    UserFilter.Expired -> user.expired
                    UserFilter.Expiring -> user.expiresAt in 1..(System.currentTimeMillis() / 1000 + 7 * 86400)
                    UserFilter.Inactive -> user.lastActivityAt == 0L || user.lastActivityAt < (System.currentTimeMillis() / 1000 - 7 * 86400)
                    UserFilter.HeavyTraffic -> user.totalBytes >= 1024L * 1024L * 1024L
                }
            }
            .let { list ->
                when (sort) {
                    UserSort.Name -> list.sortedBy { it.displayName.lowercase() }
                    UserSort.Traffic -> list.sortedByDescending { it.totalBytes }
                    UserSort.Status -> list.sortedWith(compareBy<UserSummary> { !it.connected }.thenBy { it.deactivated }.thenBy { it.displayName.lowercase() })
                }
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Пользователи", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${users.count { it.connected }} онлайн · ${users.count { it.bound }} привязано · ${users.size} всего",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = onCreate) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Создать")
                }
            }
        }
        item {
            UserControls(
                search = search,
                onSearch = { search = it },
                filter = filter,
                onFilter = { filter = it },
                sort = sort,
                onSort = { sort = it },
            )
        }
        if (selectedPasswords.isNotEmpty()) {
            item {
                BulkActionPanel(
                    count = selectedPasswords.size,
                    days = bulkDays,
                    onDays = { bulkDays = it },
                    onAction = { pendingBulk = it },
                    onClear = { selectedPasswords = emptySet() },
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { selectedPasswords = filteredUsers.map { it.password }.toSet() },
                    enabled = filteredUsers.isNotEmpty(),
                ) {
                    Icon(Icons.Default.SelectAll, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Выбрать")
                }
                OutlinedButton(
                    onClick = { selectedPasswords = emptySet() },
                    enabled = selectedPasswords.isNotEmpty(),
                ) {
                    Icon(Icons.Default.ClearAll, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Снять")
                }
            }
        }
        if (filteredUsers.isEmpty()) {
            item { EmptyCard("Пользователей по выбранным условиям нет") }
        }
        items(filteredUsers, key = { it.password }) { user ->
            UserCard(
                user = user,
                selected = user.password in selectedPasswords,
                onSelected = { selected ->
                    selectedPasswords = if (selected) {
                        selectedPasswords + user.password
                    } else {
                        selectedPasswords - user.password
                    }
                },
                onDelete = { pendingDelete = user },
                onUnbind = { onUnbind(user) },
                onResetTraffic = { onResetTraffic(user) },
                onSetEnabled = { enabled -> onSetEnabled(user, enabled) },
            )
        }
    }

    pendingDelete?.let { user ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить пользователя") },
            text = { Text(user.displayName) },
            confirmButton = {
                Button(onClick = {
                    pendingDelete = null
                    onDelete(user)
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Отмена") }
            },
        )
    }
    pendingBulk?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingBulk = null },
            title = { Text(bulkActionTitle(action)) },
            text = { Text("Пользователей: ${selectedPasswords.size}") },
            confirmButton = {
                Button(onClick = {
                    pendingBulk = null
                    val passwords = selectedPasswords.toList()
                    selectedPasswords = emptySet()
                    onBulk(action, passwords, bulkDays)
                }) { Text("Выполнить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingBulk = null }) { Text("Отмена") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserControls(
    search: String,
    onSearch: (String) -> Unit,
    filter: UserFilter,
    onFilter: (UserFilter) -> Unit,
    sort: UserSort,
    onSort: (UserSort) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = onSearch,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Поиск") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UserFilter.entries.forEach { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { onFilter(item) },
                        label = { Text(item.label) },
                    )
                }
                DropdownSelector(
                    selected = sort,
                    options = UserSort.entries.toList(),
                    optionLabel = { it.label },
                    leadingIcon = Icons.Default.FilterList,
                    onSelected = onSort,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BulkActionPanel(
    count: Int,
    days: String,
    onDays: (String) -> Unit,
    onAction: (UserBulkAction) -> Unit,
    onClear: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Выбрано: $count", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClear) { Text("Снять") }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = days,
                    onValueChange = onDays,
                    modifier = Modifier.width(108.dp),
                    label = { Text("Дней") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                FilledTonalButton(onClick = { onAction(UserBulkAction.SetExpiration) }) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Срок")
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BulkButton(Icons.Default.CheckCircle, "Вкл", UserBulkAction.Activate, onAction)
                BulkButton(Icons.Default.PowerSettingsNew, "Выкл", UserBulkAction.Deactivate, onAction)
                BulkButton(Icons.Default.RestartAlt, "Трафик", UserBulkAction.ResetTraffic, onAction)
                BulkButton(Icons.Default.LinkOff, "Отвязать", UserBulkAction.Unbind, onAction)
                BulkButton(Icons.Default.Delete, "Удалить", UserBulkAction.Delete, onAction)
            }
        }
    }
}

@Composable
private fun BulkButton(icon: ImageVector, label: String, action: UserBulkAction, onAction: (UserBulkAction) -> Unit) {
    OutlinedButton(onClick = { onAction(action) }) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserCard(
    user: UserSummary,
    selected: Boolean,
    onSelected: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onUnbind: () -> Unit,
    onResetTraffic: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Checkbox(checked = selected, onCheckedChange = onSelected)
                Column(Modifier.weight(1f)) {
                    Text(user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        user.password,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FilledIconButton(onClick = { clipboard.setText(AnnotatedString(user.password)) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Скопировать пароль")
                }
            }

            FlowStatusRow {
                UserStatusPill(user)
                if (user.bound) StatusPill(Icons.Default.Devices, "Привязан")
                if (user.deviceIp.isNotBlank()) StatusPill(Icons.Default.Dns, user.deviceIp)
                if (user.expired) StatusPill(Icons.Default.Warning, "Истёк")
                if (user.deactivated) StatusPill(Icons.Default.PowerSettingsNew, "Отключён")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                InlineMetric(Icons.Default.ArrowUpward, formatBytes(user.upBytes))
                InlineMetric(Icons.Default.ArrowDownward, formatBytes(user.downBytes))
                Text(
                    "Срок: ${expirationLabel(user.expiresAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (user.ports.isNotBlank()) StatusPill(Icons.Default.Tune, user.ports)
                if (user.vkHash.isNotBlank()) StatusPill(Icons.Default.Key, user.vkHash)
                if (user.lastHandshake > 0) StatusPill(Icons.Default.History, handshakeLabel(user.lastHandshake))
            }

            HorizontalDivider()

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onSetEnabled(user.deactivated) }) {
                    Icon(
                        Icons.Default.PowerSettingsNew,
                        contentDescription = if (user.deactivated) "Включить" else "Отключить",
                    )
                }
                IconButton(onClick = onUnbind, enabled = user.bound) {
                    Icon(Icons.Default.LinkOff, contentDescription = "Отвязать")
                }
                IconButton(onClick = onResetTraffic) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Сбросить трафик")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ProfilesScreen(
    state: AppUiState,
    onExportProfile: (QwdttProfile, String) -> Unit,
    onExportSubscription: () -> Unit,
    onImport: (String) -> Unit,
    onLoadHashes: () -> Unit,
    onAddHashes: (String) -> Unit,
    onDeleteHash: (String) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var showImport by remember { mutableStateOf(false) }
    var qrProfile by remember { mutableStateOf<QwdttProfile?>(null) }
    var hashInput by remember { mutableStateOf("") }
    LaunchedEffect(state.activeServerId) {
        if (state.activeServer != null) onLoadHashes()
    }
    val localProfiles = state.activeServer?.let { server ->
        state.users.map { ProfileTools.userProfile(server, it) }
    }.orEmpty()
    val subscription = state.qwdttSubscription
    val profiles = (subscription?.profiles ?: localProfiles) + state.importedProfiles
    val userByPassword = state.users.associateBy { it.password }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("qWDTT профили", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${profiles.size} профилей · ${state.vkHashes.size} VK-хешей",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { showImport = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Импорт")
                }
            }
        }
        item {
            SubscriptionCard(
                subscription = subscription,
                profileCount = profiles.size,
                endpoint = state.activeServer?.let { "${it.baseUrl.trimEnd('/')}/api/v1/qwdtt/subscription" }.orEmpty(),
                onExport = onExportSubscription,
                onCopyEndpoint = { endpoint ->
                    clipboard.setText(AnnotatedString(endpoint))
                },
            )
        }
        item {
            VkHashPanel(
                hashes = state.vkHashes,
                value = hashInput,
                onValue = { hashInput = it },
                onAdd = {
                    onAddHashes(hashInput)
                    hashInput = ""
                },
                onDelete = onDeleteHash,
            )
        }
        if (profiles.isEmpty()) {
            item { EmptyCard("Профилей пока нет") }
        }
        items(profiles, key = { "${it.peer}|${it.password}|${it.hashes}" }) { profile ->
            QwdttProfileCard(
                profile = profile,
                user = userByPassword[profile.password],
                onExport = { format -> onExportProfile(profile, format) },
                onQr = { qrProfile = profile },
            )
        }
    }

    if (showImport) {
        ImportProfilesDialog(
            clipboardText = clipboard.getText()?.text.orEmpty(),
            onDismiss = { showImport = false },
            onImport = {
                showImport = false
                onImport(it)
            },
        )
    }
    qrProfile?.let { profile ->
        QrDialog(
            profile = profile,
            onDismiss = { qrProfile = null },
        )
    }
}

@Composable
private fun SubscriptionCard(
    subscription: com.lebrit.wdttpanel.model.QwdttSubscription?,
    profileCount: Int,
    endpoint: String,
    onExport: () -> Unit,
    onCopyEndpoint: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(Icons.Default.Article, "JSON-подписка")
            Text(
                subscription?.name ?: "Локальная подписка",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            FlowStatusRow {
                StatusPill(Icons.Default.Groups, "$profileCount профилей")
                if ((subscription?.trafficUsedMb ?: 0.0) > 0) StatusPill(Icons.Default.Speed, "%.1f MB".format(Locale.US, subscription?.trafficUsedMb))
                if (!subscription?.updatedAt.isNullOrBlank()) StatusPill(Icons.Default.History, subscription?.updatedAt.orEmpty())
            }
            if (endpoint.isNotBlank()) {
                Text(endpoint, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onExport) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("JSON")
                }
                OutlinedButton(onClick = { onCopyEndpoint(endpoint) }, enabled = endpoint.isNotBlank()) {
                    Icon(Icons.Default.LinkOff, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("URL")
                }
            }
        }
    }
}

@Composable
private fun VkHashPanel(
    hashes: List<String>,
    value: String,
    onValue: (String) -> Unit,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(Icons.Default.Key, "VK-хеши")
            OutlinedTextField(
                value = value,
                onValueChange = onValue,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Хеши или vk.com/call/join/...") },
                singleLine = false,
                minLines = 2,
            )
            Button(onClick = onAdd, enabled = value.isNotBlank()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Добавить")
            }
            FlowStatusRow {
                hashes.forEach { hash ->
                    AssistChip(
                        onClick = { onDelete(hash) },
                        label = { Text(hash, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QwdttProfileCard(
    profile: QwdttProfile,
    user: UserSummary?,
    onExport: (String) -> Unit,
    onQr: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(profile.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(profile.peer, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
                user?.let { UserStatusPill(it) }
            }
            FlowStatusRow {
                StatusPill(Icons.Default.Tune, "${profile.workers} потоков")
                StatusPill(Icons.Default.Storage, "порт ${profile.port}")
                if (profile.hashes.isNotBlank()) StatusPill(Icons.Default.Key, profile.hashes)
                if (user != null) StatusPill(Icons.Default.Speed, formatBytes(user.totalBytes))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = { onExport("qwdtt") }) { Text("qwdtt://") }
                OutlinedButton(onClick = { onExport("wdtt") }) { Text("wdtt://") }
                IconButton(onClick = { onExport("json") }) { Icon(Icons.Default.Article, contentDescription = "JSON") }
                IconButton(onClick = onQr) { Icon(Icons.Default.Security, contentDescription = "QR") }
            }
        }
    }
}

@Composable
private fun ImportProfilesDialog(
    clipboardText: String,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
) {
    var text by remember { mutableStateOf(clipboardText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Импорт профилей") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 320.dp),
                label = { Text("wdtt://, qwdtt://, JSON, Base64 или QR-текст") },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
        },
        confirmButton = {
            Button(onClick = { onImport(text) }, enabled = text.isNotBlank()) { Text("Импорт") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun QrDialog(profile: QwdttProfile, onDismiss: () -> Unit) {
    val link = remember(profile) { ProfileTools.qwdttLink(profile) }
    val bitmap = remember(link) { ProfileTools.qrBitmap(link) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(profile.name) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR",
                    modifier = Modifier.size(260.dp),
                )
                Text(link, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogsScreen(lines: List<String>, meta: LogsMeta, onLoad: (String, Int) -> Unit) {
    var source by remember(meta.source) { mutableStateOf(meta.source) }
    var limit by remember { mutableStateOf(500) }
    var filter by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current
    val filtered = remember(lines, filter) {
        if (filter.isBlank()) lines else lines.filter { it.contains(filter, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Журналы", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${meta.title}: ${filtered.size}/${lines.size} строк",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onLoad(source, limit) }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Загрузить")
                }
                IconButton(onClick = { clipboard.setText(AnnotatedString(filtered.joinToString("\n"))) }, enabled = filtered.isNotEmpty()) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Скопировать")
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DropdownSelector(
                            selected = source,
                            options = logSources.map { it.value },
                            optionLabel = { value -> logSources.first { it.value == value }.label },
                            leadingIcon = Icons.Default.Article,
                            onSelected = {
                                source = it
                                onLoad(it, limit)
                            },
                        )
                        DropdownSelector(
                            selected = limit,
                            options = listOf(300, 500, 1000, 2000),
                            optionLabel = { "$it строк" },
                            leadingIcon = Icons.Default.FilterList,
                            onSelected = {
                                limit = it
                                onLoad(source, it)
                            },
                        )
                    }
                    OutlinedTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Фильтр") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                    )
                    if (meta.units.isNotEmpty()) {
                        FlowStatusRow {
                            meta.units.forEach { unit ->
                                HealthPill(unit.unit, unit.active)
                            }
                        }
                    }
                }
            }
        }
        if (filtered.isEmpty()) {
            item { EmptyCard("Нет строк журнала") }
        }
        items(filtered) { line ->
            LogLine(line)
        }
    }
}

@Composable
private fun LogLine(line: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = line,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun ServersScreen(
    state: AppUiState,
    onAdd: () -> Unit,
    onEdit: (ServerProfile) -> Unit,
    onDelete: (String) -> Unit,
    onSwitch: (String) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<ServerProfile?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Серверы", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${state.servers.count { it.lastStatus == ConnectionStatus.Online }} онлайн · ${state.servers.size} всего",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить")
                }
            }
        }
        items(state.servers, key = { it.id }) { server ->
            ServerCard(
                server = server,
                active = server.id == state.activeServerId,
                onEdit = { onEdit(server) },
                onDelete = { pendingDelete = server },
                onSwitch = { onSwitch(server.id) },
            )
        }
    }

    pendingDelete?.let { server ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить сервер") },
            text = { Text(server.name) },
            confirmButton = {
                Button(onClick = {
                    pendingDelete = null
                    onDelete(server.id)
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun ServerCard(
    server: ServerProfile,
    active: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSwitch: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    if (server.lastStatus == ConnectionStatus.Online) Icons.Default.Wifi else Icons.Default.Storage,
                    contentDescription = null,
                    tint = if (server.lastStatus == ConnectionStatus.Online) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                )
                Column(Modifier.weight(1f)) {
                    Text(server.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(server.baseUrl, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (active) Icon(Icons.Default.CheckCircle, contentDescription = "Активный")
            }
            FlowStatusRow {
                ConnectionStatusChip(server.lastStatus)
                if (server.version.isNotBlank()) StatusPill(Icons.Default.Security, "v${server.version}")
                if (server.allowInsecureTls) StatusPill(Icons.Default.Warning, "Self-signed TLS")
                if (server.lastCheckedAt > 0) StatusPill(Icons.Default.History, timeLabel(server.lastCheckedAt))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = onSwitch, enabled = !active) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Активировать")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Изменить") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Удалить") }
            }
        }
    }
}

@Composable
private fun ServerDialog(
    server: ServerProfile?,
    onDismiss: () -> Unit,
    onSave: (String?, String, String, String, String, Boolean) -> Unit,
) {
    var name by remember(server?.id) { mutableStateOf(server?.name.orEmpty()) }
    var url by remember(server?.id) { mutableStateOf(server?.baseUrl.orEmpty()) }
    var username by remember(server?.id) { mutableStateOf(server?.username.orEmpty()) }
    var password by remember(server?.id) { mutableStateOf("") }
    var insecure by remember(server?.id) { mutableStateOf(server?.allowInsecureTls ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (server == null) "Новый сервер" else "Сервер") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя") }, singleLine = true)
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL панели") }, singleLine = true)
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Логин") }, singleLine = true)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (server == null) "Пароль" else "Новый пароль") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Self-signed TLS", modifier = Modifier.weight(1f))
                    Switch(checked = insecure, onCheckedChange = { insecure = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(server?.id, name, url, username, password, insecure) }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onCreateManual: (String, String, String, String, String, Boolean, Boolean) -> Unit,
    onCreateAuto: (String) -> Unit,
    onCreateBulk: (Int, String, HashMode, String, String, String, Boolean, Boolean) -> Unit,
) {
    var mode by remember { mutableStateOf(CreateUserMode.Manual) }
    var label by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var vkHash by remember { mutableStateOf("") }
    var ports by remember { mutableStateOf("56000,56001,9000") }
    var days by remember { mutableStateOf("30") }
    var unlimited by remember { mutableStateOf(false) }
    var disabled by remember { mutableStateOf(false) }
    var count by remember { mutableStateOf("2") }
    var hashMode by remember { mutableStateOf(HashMode.Shared) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Пользователи") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CreateUserMode.entries.forEach { item ->
                        FilterChip(
                            selected = mode == item,
                            onClick = { mode = item },
                            label = { Text(item.label) },
                        )
                    }
                }

                when (mode) {
                    CreateUserMode.Manual -> {
                        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Метка") }, singleLine = true)
                        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Пароль") }, singleLine = true)
                        SharedUserFields(
                            vkHash = vkHash,
                            onVkHash = { vkHash = it },
                            ports = ports,
                            onPorts = { ports = it },
                            days = days,
                            onDays = { days = it },
                            unlimited = unlimited,
                            onUnlimited = { unlimited = it },
                            disabled = disabled,
                            onDisabled = { disabled = it },
                        )
                    }
                    CreateUserMode.Auto -> {
                        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Метка") }, singleLine = true)
                    }
                    CreateUserMode.Bulk -> {
                        OutlinedTextField(
                            value = count,
                            onValueChange = { count = it },
                            label = { Text("Количество") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Префикс метки") }, singleLine = true)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HashMode.entries.forEach { item ->
                                FilterChip(
                                    selected = hashMode == item,
                                    onClick = { hashMode = item },
                                    label = { Text(item.label) },
                                )
                            }
                        }
                        SharedUserFields(
                            vkHash = vkHash,
                            onVkHash = { vkHash = it },
                            ports = ports,
                            onPorts = { ports = it },
                            days = days,
                            onDays = { days = it },
                            unlimited = unlimited,
                            onUnlimited = { unlimited = it },
                            disabled = disabled,
                            onDisabled = { disabled = it },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (mode) {
                        CreateUserMode.Manual -> onCreateManual(label, password, vkHash, ports, days, unlimited, disabled)
                        CreateUserMode.Auto -> onCreateAuto(label)
                        CreateUserMode.Bulk -> onCreateBulk(
                            count.toIntOrNull() ?: 1,
                            vkHash,
                            hashMode,
                            label,
                            ports,
                            days,
                            unlimited,
                            disabled,
                        )
                    }
                },
            ) { Text(if (mode == CreateUserMode.Bulk) "Создать пакет" else "Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun SharedUserFields(
    vkHash: String,
    onVkHash: (String) -> Unit,
    ports: String,
    onPorts: (String) -> Unit,
    days: String,
    onDays: (String) -> Unit,
    unlimited: Boolean,
    onUnlimited: (Boolean) -> Unit,
    disabled: Boolean,
    onDisabled: (Boolean) -> Unit,
) {
    OutlinedTextField(value = vkHash, onValueChange = onVkHash, label = { Text("VK-хеш") }, singleLine = true)
    OutlinedTextField(value = ports, onValueChange = onPorts, label = { Text("Порты") }, singleLine = true)
    OutlinedTextField(
        value = days,
        onValueChange = onDays,
        label = { Text("Дней") },
        singleLine = true,
        enabled = !unlimited,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    ToggleRow(label = "Бессрочно", checked = unlimited, onChecked = onUnlimited)
    ToggleRow(label = "Создать отключённым", checked = disabled, onChecked = onDisabled)
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun GeneratedLinksDialog(links: GeneratedLinks, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(links.title) },
        text = {
            OutlinedTextField(
                value = links.links,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 260.dp),
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
        },
        confirmButton = {
            Button(onClick = { clipboard.setText(AnnotatedString(links.links)) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Копировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
    )
}

@Composable
private fun EmptyCard(text: String) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionTitle(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowStatusRow(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}

@Composable
private fun StatusPill(icon: ImageVector, text: String) {
    AssistChip(
        onClick = {},
        label = { Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
    )
}

@Composable
private fun HealthPill(label: String, ok: Boolean) {
    AssistChip(
        onClick = {},
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = {
            Icon(
                if (ok) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (ok) Color(0xFF16A34A) else MaterialTheme.colorScheme.tertiary,
            )
        },
    )
}

@Composable
private fun ConnectionStatusChip(status: ConnectionStatus) {
    val (text, icon, tint) = when (status) {
        ConnectionStatus.Unknown -> Triple("Не проверен", Icons.Default.Warning, MaterialTheme.colorScheme.tertiary)
        ConnectionStatus.Online -> Triple("Онлайн", Icons.Default.CheckCircle, Color(0xFF16A34A))
        ConnectionStatus.Offline -> Triple("Оффлайн", Icons.Default.WifiOff, MaterialTheme.colorScheme.error)
        ConnectionStatus.AuthRequired -> Triple("Пароль", Icons.Default.Key, MaterialTheme.colorScheme.tertiary)
    }
    AssistChip(
        onClick = {},
        label = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = tint) },
    )
}

@Composable
private fun UserStatusPill(user: UserSummary) {
    val text = when {
        user.connected -> "Онлайн"
        user.bound -> "Неактивен"
        else -> "Свободен"
    }
    val icon = if (user.connected) Icons.Default.Wifi else Icons.Default.WifiOff
    AssistChip(
        onClick = {},
        label = { Text(text) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (user.connected) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun InlineMetric(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun <T> DropdownSelector(
    selected: T,
    options: List<T>,
    optionLabel: (T) -> String,
    leadingIcon: ImageVector,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Icon(leadingIcon, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(optionLabel(selected), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = { Text(optionLabel(item)) },
                    onClick = {
                        expanded = false
                        onSelected(item)
                    },
                )
            }
        }
    }
}

private data class Metric(val label: String, val value: String, val icon: ImageVector)

private data class LogSource(val value: String, val label: String)

private enum class UserFilter(val label: String) {
    All("Все"),
    Online("Онлайн"),
    Bound("Привязанные"),
    Disabled("Отключённые"),
    Expired("Истёкшие"),
    Expiring("Истекают"),
    Inactive("Без активности"),
    HeavyTraffic("Много трафика"),
}

private enum class UserSort(val label: String) {
    Name("Имя"),
    Traffic("Трафик"),
    Status("Статус"),
}

private data class TabItem(
    val tab: AppTab,
    val label: String,
    val icon: ImageVector,
)

private val logSources = listOf(
    LogSource("wdtt", "WDTT"),
    LogSource("panel", "Панель"),
    LogSource("nginx", "Nginx"),
    LogSource("installer", "Установщик"),
)

private val CreateUserMode.label: String
    get() = when (this) {
        CreateUserMode.Manual -> "Один"
        CreateUserMode.Auto -> "Авто"
        CreateUserMode.Bulk -> "Пакет"
    }

private val HashMode.label: String
    get() = when (this) {
        HashMode.Shared -> "Общий хеш"
        HashMode.Rotate -> "По очереди"
    }

private fun tabItems(): List<TabItem> = listOf(
    TabItem(AppTab.Dashboard, "Обзор", Icons.Default.Dashboard),
    TabItem(AppTab.Users, "Люди", Icons.Default.Groups),
    TabItem(AppTab.Profiles, "Профили", Icons.Default.Key),
    TabItem(AppTab.Logs, "Логи", Icons.Default.Terminal),
    TabItem(AppTab.Servers, "Серверы", Icons.Default.Storage),
)

private fun UserSummary.matches(query: String): Boolean {
    val normalized = query.trim()
    if (normalized.isBlank()) return true
    return listOf(displayName, password, deviceIp, vkHash, ports)
        .any { it.contains(normalized, ignoreCase = true) }
}

private fun bulkActionTitle(action: UserBulkAction): String =
    when (action) {
        UserBulkAction.Activate -> "Включить пользователей"
        UserBulkAction.Deactivate -> "Отключить пользователей"
        UserBulkAction.SetExpiration -> "Изменить срок"
        UserBulkAction.ResetTraffic -> "Сбросить трафик"
        UserBulkAction.Unbind -> "Отвязать устройства"
        UserBulkAction.Delete -> "Удалить пользователей"
    }

private fun formatBytes(value: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var amount = value.toDouble()
    var index = 0
    while (amount >= 1024 && index < units.lastIndex) {
        amount /= 1024
        index += 1
    }
    return if (index == 0) "${value} ${units[index]}" else "%.1f %s".format(Locale.US, amount, units[index])
}

private fun expirationLabel(timestamp: Long): String {
    if (timestamp <= 0L) return "Бессрочно"
    return dateLabel(timestamp)
}

private fun handshakeLabel(timestamp: Long): String =
    "HS ${dateLabel(timestamp)}"

private fun dateLabel(timestamp: Long): String {
    val millis = if (timestamp < 10_000_000_000L) timestamp * 1000 else timestamp
    return SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(millis))
}

private fun timeLabel(timestamp: Long): String =
    SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun percentFraction(value: String): Float =
    value
        .replace("%", "")
        .replace(",", ".")
        .trim()
        .toFloatOrNull()
        ?.div(100f)
        ?.coerceIn(0f, 1f)
        ?: 0f

private fun percentLabel(value: String): String {
    val clean = value.trim()
    if (clean.isBlank() || clean == "-") return "-"
    return if (clean.endsWith("%")) clean else "$clean%"
}
