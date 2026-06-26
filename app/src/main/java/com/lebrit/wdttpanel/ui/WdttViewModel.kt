package com.lebrit.wdttpanel.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lebrit.wdttpanel.data.ApiException
import com.lebrit.wdttpanel.data.PanelApiClient
import com.lebrit.wdttpanel.data.SecureServerStore
import com.lebrit.wdttpanel.model.AppTab
import com.lebrit.wdttpanel.model.AppUiState
import com.lebrit.wdttpanel.model.ConnectionStatus
import com.lebrit.wdttpanel.model.GeneratedLinks
import com.lebrit.wdttpanel.model.HashMode
import com.lebrit.wdttpanel.model.LogsMeta
import com.lebrit.wdttpanel.model.OverviewSummary
import com.lebrit.wdttpanel.model.ServerProfile
import com.lebrit.wdttpanel.model.ServiceUnitStatus
import com.lebrit.wdttpanel.model.StoredState
import com.lebrit.wdttpanel.model.UserBulkAction
import com.lebrit.wdttpanel.model.UserSummary
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class WdttViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SecureServerStore(application)
    private val api = PanelApiClient()
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val stored = store.load()
            _state.update {
                it.copy(
                    servers = stored.servers,
                    activeServerId = stored.activeServerId ?: stored.servers.firstOrNull()?.id,
                    selectedTab = if (stored.servers.isEmpty()) AppTab.Servers else AppTab.Dashboard,
                )
            }
            refresh()
        }
    }

    fun selectTab(tab: AppTab) {
        _state.update { it.copy(selectedTab = tab, message = null, error = null) }
    }

    fun clearMessages() {
        _state.update { it.copy(message = null, error = null) }
    }

    fun clearGeneratedLinks() {
        _state.update { it.copy(generatedLinks = null) }
    }

    fun refresh() {
        val profile = _state.value.activeServer ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result = runCatching {
                val authed = ensureAuthenticated(profile)
                val overview = parseOverview(api.overview(authed))
                val users = parseUsers(api.users(authed))
                val logsRoot = api.logs(authed, source = _state.value.logsMeta.source)
                val logs = parseLogs(logsRoot)
                val logsMeta = parseLogsMeta(logsRoot)
                val checked = authed.copy(lastStatus = ConnectionStatus.Online, lastCheckedAt = now())
                replaceAndPersist(checked)
                _state.update {
                    it.copy(
                        loading = false,
                        overview = overview,
                        users = users,
                        logs = logs,
                        logsMeta = logsMeta,
                        error = null,
                    )
                }
            }
            val error = result.exceptionOrNull()
            if (error != null) {
                markServer(profile.id, ConnectionStatus.Offline)
                _state.update {
                    it.copy(
                        loading = false,
                        error = error.message ?: "Не удалось обновить сервер",
                    )
                }
            }
        }
    }

    fun loadLogs(source: String, limit: Int) {
        val profile = _state.value.activeServer ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val authed = ensureAuthenticated(profile)
                val root = api.logs(authed, source = source, limit = limit)
                _state.update {
                    it.copy(
                        loading = false,
                        logs = parseLogs(root),
                        logsMeta = parseLogsMeta(root),
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = error.message ?: "Не удалось загрузить журнал",
                    )
                }
            }
        }
    }

    fun saveServer(
        editingId: String?,
        name: String,
        rawUrl: String,
        username: String,
        password: String,
        allowInsecureTls: Boolean,
    ) {
        viewModelScope.launch {
            val current = _state.value.servers.firstOrNull { it.id == editingId }
            val savedPassword = password.ifBlank { current?.password.orEmpty() }
            if (name.isBlank() || rawUrl.isBlank() || savedPassword.isBlank()) {
                _state.update { it.copy(error = "Заполните имя, URL и пароль") }
                return@launch
            }
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val baseUrl = api.normalizeBaseUrl(rawUrl)
                val candidate = ServerProfile(
                    id = current?.id ?: UUID.randomUUID().toString(),
                    name = name.trim(),
                    baseUrl = baseUrl,
                    username = username.trim(),
                    password = savedPassword,
                    allowInsecureTls = allowInsecureTls,
                )
                val info = api.info(candidate)
                val login = api.login(candidate, savedPassword)
                val version = login.version.ifBlank { info.text("panel_version") }
                val checked = candidate.copy(
                    token = login.token,
                    version = version,
                    lastStatus = ConnectionStatus.Online,
                    lastCheckedAt = now(),
                )
                val nextServers = _state.value.servers
                    .filterNot { it.id == checked.id }
                    .plus(checked)
                    .sortedBy { it.name.lowercase() }
                persist(nextServers, checked.id)
                _state.update {
                    it.copy(
                        loading = false,
                        servers = nextServers,
                        activeServerId = checked.id,
                        selectedTab = AppTab.Dashboard,
                        message = "Сервер подключён",
                    )
                }
                refresh()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = error.message ?: "Не удалось подключить сервер",
                    )
                }
            }
        }
    }

    fun switchServer(id: String) {
        viewModelScope.launch {
            persist(_state.value.servers, id)
            _state.update {
                it.copy(
                    activeServerId = id,
                    selectedTab = AppTab.Dashboard,
                    overview = null,
                    users = emptyList(),
                    logs = emptyList(),
                )
            }
            refresh()
        }
    }

    fun deleteServer(id: String) {
        viewModelScope.launch {
            val next = _state.value.servers.filterNot { it.id == id }
            val active = when {
                _state.value.activeServerId != id -> _state.value.activeServerId
                else -> next.firstOrNull()?.id
            }
            persist(next, active)
            _state.update {
                it.copy(
                    servers = next,
                    activeServerId = active,
                    selectedTab = if (next.isEmpty()) AppTab.Servers else it.selectedTab,
                    overview = if (active == null) null else it.overview,
                    users = if (active == null) emptyList() else it.users,
                    logs = if (active == null) emptyList() else it.logs,
                    message = "Сервер удалён",
                )
            }
            if (active != null) refresh()
        }
    }

    fun createUser(
        label: String,
        password: String,
        vkHash: String,
        ports: String,
        days: String,
        unlimited: Boolean,
        disabled: Boolean,
    ) {
        val payload = buildJsonObject {
            put("label", label)
            if (password.isNotBlank()) {
                put("password", password)
            }
            put("vk_hash", vkHash)
            put("ports", ports.ifBlank { "56000,56001,9000" })
            put("is_deactivated", disabled)
            if (unlimited) {
                put("unlimited", true)
            } else {
                put("days", days.toIntOrNull() ?: 30)
            }
        }
        postAction("users/create", payload, "Пользователь создан") { result, profile ->
            val user = result as? JsonObject ?: return@postAction
            _state.update {
                it.copy(generatedLinks = GeneratedLinks("Ссылка пользователя", quickLinks(listOf(user), profile)))
            }
        }
    }

    fun createAutoUser(label: String) {
        val payload = buildJsonObject { put("label", label) }
        postAction("users/create-auto", payload, "Пользователь создан автоматически") { result, profile ->
            val user = result as? JsonObject ?: return@postAction
            _state.update {
                it.copy(generatedLinks = GeneratedLinks("Ссылка пользователя", quickLinks(listOf(user), profile)))
            }
        }
    }

    fun createUsersBulk(
        count: Int,
        vkHash: String,
        hashMode: HashMode,
        labelPrefix: String,
        ports: String,
        days: String,
        unlimited: Boolean,
        disabled: Boolean,
    ) {
        val payload = buildJsonObject {
            put("count", count.coerceIn(1, 10))
            put("vk_hash", vkHash)
            put("hash_mode", if (hashMode == HashMode.Rotate) "rotate" else "shared")
            put("label_prefix", labelPrefix)
            put("ports", ports.ifBlank { "56000,56001,9000" })
            put("is_deactivated", disabled)
            if (unlimited) {
                put("unlimited", true)
            } else {
                put("days", days.toIntOrNull() ?: 30)
            }
        }
        postAction("users/create-bulk", payload, "Пользователи созданы") { result, profile ->
            val root = result as? JsonObject ?: return@postAction
            val users = root.array("users").mapNotNull { it as? JsonObject }
            if (users.isNotEmpty()) {
                _state.update {
                    it.copy(generatedLinks = GeneratedLinks("Ссылки пользователей", quickLinks(users, profile)))
                }
            }
        }
    }

    fun deleteUser(user: UserSummary) {
        postUserAction("users/delete", passwordPayload(user.password), "Пользователь удалён")
    }

    fun unbindUser(user: UserSummary) {
        postUserAction("users/unbind", passwordPayload(user.password), "Устройство отвязано")
    }

    fun resetTraffic(user: UserSummary) {
        postUserAction("users/reset-traffic", passwordPayload(user.password), "Трафик сброшен")
    }

    fun setUserEnabled(user: UserSummary, enabled: Boolean) {
        val payload = buildJsonObject {
            put("current_password", user.password)
            put("password", user.password)
            put("is_deactivated", !enabled)
        }
        postUserAction("users/update", payload, if (enabled) "Пользователь активирован" else "Пользователь отключён")
    }

    fun bulkUserAction(action: UserBulkAction, passwords: List<String>, days: String) {
        if (passwords.isEmpty()) {
            _state.update { it.copy(error = "Выберите пользователей") }
            return
        }
        val actionValue = when (action) {
            UserBulkAction.Activate -> "activate"
            UserBulkAction.Deactivate -> "deactivate"
            UserBulkAction.SetExpiration -> "set_expiration"
            UserBulkAction.ResetTraffic -> "reset_traffic"
            UserBulkAction.Unbind -> "unbind"
            UserBulkAction.Delete -> "delete"
        }
        val payload = buildJsonObject {
            put("action", actionValue)
            put("passwords", JsonArray(passwords.distinct().map { JsonPrimitive(it) }))
            if (action == UserBulkAction.SetExpiration) {
                put("days", days.toIntOrNull() ?: 30)
            }
        }
        postUserAction("users/bulk-action", payload, "Массовое действие выполнено")
    }

    fun serviceAction(action: String) {
        val payload = buildJsonObject { put("service_action", action) }
        postUserAction("service", payload, "Команда отправлена")
    }

    private fun postUserAction(route: String, payload: JsonObject, success: String) {
        postAction(route, payload, success)
    }

    private fun postAction(
        route: String,
        payload: JsonObject,
        success: String,
        onResult: (JsonElement, ServerProfile) -> Unit = { _, _ -> },
    ) {
        val profile = _state.value.activeServer ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val authed = ensureAuthenticated(profile)
                val result = api.post(authed, route, payload)
                onResult(result, authed)
                _state.update { it.copy(message = success) }
                refresh()
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = error.message ?: "Команда не выполнена",
                    )
                }
            }
        }
    }

    private suspend fun ensureAuthenticated(profile: ServerProfile): ServerProfile {
        if (profile.token.isNotBlank()) {
            runCatching { api.session(profile) }
                .onSuccess { return profile }
        }
        if (profile.password.isBlank()) {
            markServer(profile.id, ConnectionStatus.AuthRequired)
            throw ApiException("Нужно заново ввести пароль")
        }
        val login = api.login(profile, profile.password)
        val updated = profile.copy(
            token = login.token,
            version = login.version.ifBlank { profile.version },
            lastStatus = ConnectionStatus.Online,
            lastCheckedAt = now(),
        )
        replaceAndPersist(updated)
        return updated
    }

    private suspend fun replaceAndPersist(profile: ServerProfile) {
        val next = _state.value.servers.map { if (it.id == profile.id) profile else it }
        persist(next, _state.value.activeServerId ?: profile.id)
        _state.update { it.copy(servers = next) }
    }

    private suspend fun markServer(id: String, status: ConnectionStatus) {
        val next = _state.value.servers.map {
            if (it.id == id) it.copy(lastStatus = status, lastCheckedAt = now()) else it
        }
        persist(next, _state.value.activeServerId)
        _state.update { it.copy(servers = next) }
    }

    private suspend fun persist(servers: List<ServerProfile>, activeId: String?) {
        store.save(StoredState(activeServerId = activeId, servers = servers))
    }

    private fun passwordPayload(password: String): JsonObject =
        buildJsonObject { put("password", password) }

    private fun parseOverview(root: JsonObject): OverviewSummary {
        val stats = root.obj("stats")
        val system = root.obj("system")
        return OverviewSummary(
            active = stats.int("active"),
            total = stats.int("total"),
            users = root.int("users"),
            devices = root.int("devices"),
            onlineDevices = root.int("online_devices"),
            uploadGb = stats.text("up_gb", "0"),
            downloadGb = stats.text("down_gb", "0"),
            nat = stats.text("nat"),
            uptime = stats.text("uptime"),
            cpuPercent = system.text("cpu_percent", "-"),
            memoryPercent = system.obj("memory").text("percent", "-"),
            diskPercent = root.obj("disk").text("percent", "-"),
            serviceActive = root.obj("service").bool("active"),
            serviceExists = root.obj("service").bool("exists"),
            binaryExists = root.obj("service").bool("binary"),
            ipForward = root.obj("service").text("ip_forward"),
            publicHost = root.text("public_host"),
            tlsMode = root.text("tls_mode"),
            httpsPort = root.int("https_port", 443),
        )
    }

    private fun parseUsers(root: JsonObject): List<UserSummary> =
        root.array("users").mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val device = obj.objOrNull("device")
            UserSummary(
                password = obj.text("password"),
                label = obj.text("label"),
                deviceId = obj.text("device_id"),
                deviceIp = device?.text("ip").orEmpty(),
                expiresAt = obj.long("expires_at"),
                downBytes = obj.long("down_bytes"),
                upBytes = obj.long("up_bytes"),
                vkHash = obj.text("vk_hash"),
                ports = obj.text("ports", "56000,56001,9000"),
                deactivated = obj.bool("is_deactivated"),
                expired = obj.bool("expired"),
                connected = obj.bool("connected"),
                lastHandshake = obj.long("last_handshake"),
            )
        }

    private fun parseLogs(root: JsonObject): List<String> =
        root.array("lines").mapNotNull { it.asTextOrNull() }

    private fun parseLogsMeta(root: JsonObject): LogsMeta =
        LogsMeta(
            source = root.text("source", _state.value.logsMeta.source),
            title = root.text("title", "Журнал WDTT"),
            units = root.array("units").mapNotNull { item ->
                val obj = item as? JsonObject ?: return@mapNotNull null
                ServiceUnitStatus(
                    unit = obj.text("unit"),
                    active = obj.bool("active"),
                )
            },
        )

    private fun quickLinks(users: List<JsonObject>, profile: ServerProfile): String =
        users.joinToString("\n") { user ->
            val ports = user.text("ports", "56000,56001,9000")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val host = runCatching { URI(profile.baseUrl).host }
                .getOrNull()
                .orEmpty()
                .ifBlank { profile.baseUrl.removePrefix("https://").removePrefix("http://").trimEnd('/') }
            listOf(
                "wdtt://$host",
                ports.getOrElse(0) { "56000" },
                ports.getOrElse(1) { "56001" },
                ports.getOrElse(2) { "9000" },
                user.text("password"),
                user.text("vk_hash"),
            ).joinToString(":")
        }

    private fun now(): Long = System.currentTimeMillis()
}

private fun JsonObject.obj(key: String): JsonObject = objOrNull(key) ?: JsonObject(emptyMap())

private fun JsonObject.objOrNull(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.array(key: String): JsonArray = this[key] as? JsonArray ?: JsonArray(emptyList())

private fun JsonObject.text(key: String, default: String = ""): String =
    this[key].asTextOrNull() ?: default

private fun JsonObject.int(key: String, default: Int = 0): Int =
    this[key]?.jsonPrimitive?.intOrNull ?: default

private fun JsonObject.long(key: String, default: Long = 0): Long =
    this[key]?.jsonPrimitive?.longOrNull ?: default

private fun JsonObject.bool(key: String, default: Boolean = false): Boolean =
    this[key]?.jsonPrimitive?.booleanOrNull ?: default

private fun JsonElement?.asTextOrNull(): String? =
    when (this) {
        null, JsonNull -> null
        is JsonPrimitive -> content
        else -> null
    }
