package com.lebrit.wdttpanel.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerProfile(
    val id: String,
    val name: String,
    val baseUrl: String,
    val username: String = "",
    val password: String = "",
    val token: String = "",
    val allowInsecureTls: Boolean = false,
    val version: String = "",
    val lastStatus: ConnectionStatus = ConnectionStatus.Unknown,
    val lastCheckedAt: Long = 0L,
)

@Serializable
data class StoredState(
    val activeServerId: String? = null,
    val servers: List<ServerProfile> = emptyList(),
)

@Serializable
enum class ConnectionStatus {
    Unknown,
    Online,
    Offline,
    AuthRequired,
}

data class OverviewSummary(
    val active: Int = 0,
    val total: Int = 0,
    val users: Int = 0,
    val devices: Int = 0,
    val uploadGb: String = "0",
    val downloadGb: String = "0",
    val cpuPercent: String = "-",
    val memoryPercent: String = "-",
    val diskPercent: String = "-",
    val serviceActive: Boolean = false,
)

data class UserSummary(
    val password: String,
    val label: String,
    val deviceId: String,
    val deviceIp: String,
    val expiresAt: Long,
    val downBytes: Long,
    val upBytes: Long,
    val vkHash: String,
    val ports: String,
    val deactivated: Boolean,
    val expired: Boolean,
) {
    val displayName: String
        get() = label.ifBlank { password }

    val connected: Boolean
        get() = deviceId.isNotBlank()
}

enum class AppTab {
    Dashboard,
    Users,
    Logs,
    Servers,
}

data class AppUiState(
    val loading: Boolean = false,
    val servers: List<ServerProfile> = emptyList(),
    val activeServerId: String? = null,
    val selectedTab: AppTab = AppTab.Servers,
    val overview: OverviewSummary? = null,
    val users: List<UserSummary> = emptyList(),
    val logs: List<String> = emptyList(),
    val message: String? = null,
    val error: String? = null,
) {
    val activeServer: ServerProfile?
        get() = servers.firstOrNull { it.id == activeServerId }
}
