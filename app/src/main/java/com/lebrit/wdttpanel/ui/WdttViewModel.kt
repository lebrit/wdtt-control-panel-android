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
import com.lebrit.wdttpanel.model.OverviewSummary
import com.lebrit.wdttpanel.model.ServerProfile
import com.lebrit.wdttpanel.model.StoredState
import com.lebrit.wdttpanel.model.UserSummary
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
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

    fun refresh() {
        val profile = _state.value.activeServer ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val result = runCatching {
                val authed = ensureAuthenticated(profile)
                val overview = parseOverview(api.overview(authed))
                val users = parseUsers(api.users(authed))
                val logs = parseLogs(api.logs(authed))
                val checked = authed.copy(lastStatus = ConnectionStatus.Online, lastCheckedAt = now())
                replaceAndPersist(checked)
                _state.update {
                    it.copy(
                        loading = false,
                        overview = overview,
                        users = users,
                        logs = logs,
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

    fun createUser(label: String, vkHash: String, ports: String, days: String, unlimited: Boolean) {
        val payload = buildJsonObject {
            put("label", label)
            put("vk_hash", vkHash)
            put("ports", ports.ifBlank { "56000,56001,9000" })
            if (unlimited) {
                put("unlimited", true)
            } else {
                put("days", days.toIntOrNull() ?: 30)
            }
        }
        postUserAction("users/create", payload, "Пользователь создан")
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

    fun serviceAction(action: String) {
        val payload = buildJsonObject { put("service_action", action) }
        postUserAction("service", payload, "Команда отправлена")
    }

    private fun postUserAction(route: String, payload: JsonObject, success: String) {
        val profile = _state.value.activeServer ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val authed = ensureAuthenticated(profile)
                api.post(authed, route, payload)
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
            uploadGb = stats.text("up_gb", "0"),
            downloadGb = stats.text("down_gb", "0"),
            cpuPercent = system.text("cpu_percent", "-"),
            memoryPercent = system.obj("memory").text("percent", "-"),
            diskPercent = root.obj("disk").text("percent", "-"),
            serviceActive = root.obj("service").bool("active"),
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
                deviceIp = device.text("ip"),
                expiresAt = obj.long("expires_at"),
                downBytes = obj.long("down_bytes"),
                upBytes = obj.long("up_bytes"),
                vkHash = obj.text("vk_hash"),
                ports = obj.text("ports", "56000,56001,9000"),
                deactivated = obj.bool("is_deactivated"),
                expired = obj.bool("expired"),
            )
        }

    private fun parseLogs(root: JsonObject): List<String> =
        root.array("lines").mapNotNull { it.jsonPrimitive.contentOrNull }

    private fun now(): Long = System.currentTimeMillis()
}

private fun JsonObject.obj(key: String): JsonObject = objOrNull(key) ?: JsonObject(emptyMap())

private fun JsonObject.objOrNull(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.array(key: String): JsonArray = this[key] as? JsonArray ?: JsonArray(emptyList())

private fun JsonObject.text(key: String, default: String = ""): String =
    this[key]?.jsonPrimitive?.contentOrNull ?: default

private fun JsonObject.int(key: String, default: Int = 0): Int =
    this[key]?.jsonPrimitive?.intOrNull ?: default

private fun JsonObject.long(key: String, default: Long = 0): Long =
    this[key]?.jsonPrimitive?.longOrNull ?: default

private fun JsonObject.bool(key: String, default: Boolean = false): Boolean =
    this[key]?.jsonPrimitive?.booleanOrNull ?: default
