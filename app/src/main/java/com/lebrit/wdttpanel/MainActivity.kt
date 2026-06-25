package com.lebrit.wdttpanel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.lebrit.wdttpanel.model.AppTab
import com.lebrit.wdttpanel.model.AppUiState
import com.lebrit.wdttpanel.model.ConnectionStatus
import com.lebrit.wdttpanel.model.OverviewSummary
import com.lebrit.wdttpanel.model.ServerProfile
import com.lebrit.wdttpanel.model.UserSummary
import com.lebrit.wdttpanel.ui.WdttViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WdttTheme {
                val viewModel: WdttViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                WdttApp(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun WdttTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF0369A1),
            secondary = androidx.compose.ui.graphics.Color(0xFF0F766E),
            tertiary = androidx.compose.ui.graphics.Color(0xFF7C3AED),
            surface = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE2E8F0),
            background = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
        ),
        content = content,
    )
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
                        Text(
                            text = state.activeServer?.name ?: "WDTT Panel",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        state.activeServer?.baseUrl?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    ServerSwitcher(state = state, onSwitch = viewModel::switchServer)
                    IconButton(onClick = viewModel::refresh, enabled = state.activeServer != null && !state.loading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Обновить")
                    }
                    IconButton(onClick = {
                        editingServer = null
                        showServerDialog = true
                    }) {
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
                        label = { Text(item.label) },
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
                state.servers.isEmpty() -> EmptyServers(onAdd = {
                    editingServer = null
                    showServerDialog = true
                })
                state.selectedTab == AppTab.Dashboard -> DashboardScreen(
                    overview = state.overview,
                    onService = viewModel::serviceAction,
                )
                state.selectedTab == AppTab.Users -> UsersScreen(
                    users = state.users,
                    onCreate = { showCreateUser = true },
                    onDelete = viewModel::deleteUser,
                    onUnbind = viewModel::unbindUser,
                    onResetTraffic = viewModel::resetTraffic,
                    onSetEnabled = viewModel::setUserEnabled,
                )
                state.selectedTab == AppTab.Logs -> LogsScreen(lines = state.logs)
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    CircularProgressIndicator()
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
            onCreate = { label, hash, ports, days, unlimited ->
                showCreateUser = false
                viewModel.createUser(label, hash, ports, days, unlimited)
            },
        )
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
                    text = { Text(server.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
        Icon(Icons.Default.Storage, contentDescription = null)
        Spacer(Modifier.height(12.dp))
        Text("Серверы не добавлены", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Добавить сервер")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardScreen(overview: OverviewSummary?, onService: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Обзор", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
        if (overview == null) {
            item { EmptyCard("Нет данных") }
        } else {
            item {
                MetricGrid(
                    listOf(
                        "Активные" to "${overview.active}/${overview.total}",
                        "Пользователи" to overview.users.toString(),
                        "Устройства" to overview.devices.toString(),
                        "Вверх" to "${overview.uploadGb} GB",
                        "Вниз" to "${overview.downloadGb} GB",
                        "CPU" to "${overview.cpuPercent}%",
                        "RAM" to "${overview.memoryPercent}%",
                        "Диск" to "${overview.diskPercent}%",
                    ),
                )
            }
            item {
                Card(shape = RoundedCornerShape(8.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(active = overview.serviceActive)
                            Spacer(Modifier.weight(1f))
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = { onService("start") }) { Text("Запустить") }
                            FilledTonalButton(onClick = { onService("restart") }) { Text("Рестарт") }
                            OutlinedButton(onClick = { onService("stop") }) { Text("Стоп") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    MetricCard(label = item.first, value = item.second, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
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
) {
    var pendingDelete by remember { mutableStateOf<UserSummary?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Пользователи", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Button(onClick = onCreate) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Создать")
                }
            }
        }
        if (users.isEmpty()) {
            item { EmptyCard("Пользователей нет") }
        }
        items(users, key = { it.password }) { user ->
            UserCard(
                user = user,
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserCard(
    user: UserSummary,
    onDelete: () -> Unit,
    onUnbind: () -> Unit,
    onResetTraffic: () -> Unit,
    onSetEnabled: (Boolean) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(user.password, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
                IconButton(onClick = { clipboard.setText(AnnotatedString(user.password)) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Скопировать пароль")
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(if (user.connected) "Подключён" else "Не привязан") })
                if (user.deactivated) AssistChip(onClick = {}, label = { Text("Отключён") }, leadingIcon = { Icon(Icons.Default.Warning, null) })
                if (user.expired) AssistChip(onClick = {}, label = { Text("Истёк") }, leadingIcon = { Icon(Icons.Default.Warning, null) })
                if (user.deviceIp.isNotBlank()) AssistChip(onClick = {}, label = { Text(user.deviceIp) })
            }
            Text("Трафик: ${formatBytes(user.upBytes)} вверх / ${formatBytes(user.downBytes)} вниз")
            if (user.vkHash.isNotBlank()) Text("VK: ${user.vkHash}", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Порты: ${user.ports}")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { onSetEnabled(user.deactivated) }) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = if (user.deactivated) "Включить" else "Отключить")
                }
                IconButton(onClick = onUnbind, enabled = user.connected) {
                    Icon(Icons.Default.LinkOff, contentDescription = "Отвязать")
                }
                IconButton(onClick = onResetTraffic) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Сбросить трафик")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                }
            }
        }
    }
}

@Composable
private fun LogsScreen(lines: List<String>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("Логи", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        }
        if (lines.isEmpty()) {
            item { EmptyCard("Лог пуст") }
        }
        items(lines) { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Серверы", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
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
                onDelete = { onDelete(server.id) },
                onSwitch = { onSwitch(server.id) },
            )
        }
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
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(server.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(server.baseUrl, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (active) Icon(Icons.Default.CheckCircle, contentDescription = "Активный")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusLabel(server.lastStatus)
                if (server.version.isNotBlank()) Text("v${server.version}", style = MaterialTheme.typography.labelMedium)
                if (server.lastCheckedAt > 0) Text(timeLabel(server.lastCheckedAt), style = MaterialTheme.typography.labelSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalButton(onClick = onSwitch, enabled = !active) { Text("Переключить") }
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
                    label = { Text("Пароль") },
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

@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, Boolean) -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var vkHash by remember { mutableStateOf("") }
    var ports by remember { mutableStateOf("56000,56001,9000") }
    var days by remember { mutableStateOf("30") }
    var unlimited by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый пользователь") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Метка") }, singleLine = true)
                OutlinedTextField(value = vkHash, onValueChange = { vkHash = it }, label = { Text("VK-хеш") }, singleLine = true)
                OutlinedTextField(value = ports, onValueChange = { ports = it }, label = { Text("Порты") }, singleLine = true)
                OutlinedTextField(
                    value = days,
                    onValueChange = { days = it },
                    label = { Text("Дней") },
                    singleLine = true,
                    enabled = !unlimited,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Бессрочно", modifier = Modifier.weight(1f))
                    Switch(checked = unlimited, onCheckedChange = { unlimited = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(label, vkHash, ports, days, unlimited) }) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

@Composable
private fun EmptyCard(text: String) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Text(text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatusChip(active: Boolean) {
    AssistChip(
        onClick = {},
        label = { Text(if (active) "Сервис активен" else "Сервис остановлен") },
        leadingIcon = {
            Icon(if (active) Icons.Default.CheckCircle else Icons.Default.Warning, contentDescription = null)
        },
    )
}

@Composable
private fun StatusLabel(status: ConnectionStatus) {
    val text = when (status) {
        ConnectionStatus.Unknown -> "Не проверен"
        ConnectionStatus.Online -> "Онлайн"
        ConnectionStatus.Offline -> "Оффлайн"
        ConnectionStatus.AuthRequired -> "Нужен пароль"
    }
    AssistChip(onClick = {}, label = { Text(text) })
}

private data class TabItem(
    val tab: AppTab,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private fun tabItems(): List<TabItem> = listOf(
    TabItem(AppTab.Dashboard, "Обзор", Icons.Default.Home),
    TabItem(AppTab.Users, "Пользователи", Icons.Default.Groups),
    TabItem(AppTab.Logs, "Логи", Icons.Default.Article),
    TabItem(AppTab.Servers, "Серверы", Icons.Default.Storage),
)

private fun formatBytes(value: Long): String {
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var amount = value.toDouble()
    var index = 0
    while (amount >= 1024 && index < units.lastIndex) {
        amount /= 1024
        index += 1
    }
    return if (index == 0) "${value} ${units[index]}" else "%.1f %s".format(amount, units[index])
}

private fun timeLabel(timestamp: Long): String =
    SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(timestamp))
