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
    val lastLatencyMs: Long = 0L,
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
    val onlineDevices: Int = 0,
    val uploadGb: String = "0",
    val downloadGb: String = "0",
    val nat: String = "",
    val uptime: String = "",
    val cpuPercent: String = "-",
    val memoryPercent: String = "-",
    val diskPercent: String = "-",
    val serviceActive: Boolean = false,
    val serviceExists: Boolean = false,
    val binaryExists: Boolean = false,
    val ipForward: String = "",
    val publicHost: String = "",
    val tlsMode: String = "",
    val httpsPort: Int = 443,
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
    val connected: Boolean = false,
    val lastHandshake: Long = 0L,
    val lastUploadAt: Long = 0L,
    val lastDownloadAt: Long = 0L,
) {
    val displayName: String
        get() = label.ifBlank { password }

    val bound: Boolean
        get() = deviceId.isNotBlank()

    val totalBytes: Long
        get() = downBytes + upBytes

    val lastActivityAt: Long
        get() = maxOf(lastHandshake, lastUploadAt, lastDownloadAt)
}

data class QwdttProfile(
    val name: String,
    val peer: String,
    val hashes: String,
    val workers: Int,
    val port: Int,
    val password: String,
    val source: String = "local",
)

data class QwdttSubscription(
    val name: String = "WDTT",
    val description: String = "",
    val updatedAt: String = "",
    val trafficUsedMb: Double = 0.0,
    val profiles: List<QwdttProfile> = emptyList(),
)

data class VkHashLibrary(
    val hashes: List<String> = emptyList(),
)

data class TelegramSettings(
    val enabled: Boolean = false,
    val adminId: String = "",
    val botTokenSet: Boolean = false,
    val botTokenHint: String = "",
    val serviceActive: Boolean = false,
)

data class ServiceUnitStatus(
    val unit: String,
    val active: Boolean,
)

data class LogsMeta(
    val source: String = "wdtt",
    val title: String = "Журнал WDTT",
    val units: List<ServiceUnitStatus> = emptyList(),
)

data class GeneratedLinks(
    val title: String,
    val links: String,
)

enum class UserBulkAction {
    Activate,
    Deactivate,
    SetExpiration,
    ResetTraffic,
    Unbind,
    Delete,
}

enum class HashMode {
    Shared,
    Rotate,
}

enum class CreateUserMode {
    Manual,
    Auto,
    Bulk,
}

enum class AppTab {
    Dashboard,
    Users,
    Profiles,
    Settings,
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
    val logsMeta: LogsMeta = LogsMeta(),
    val vkHashes: List<String> = emptyList(),
    val telegram: TelegramSettings = TelegramSettings(),
    val qwdttSubscription: QwdttSubscription? = null,
    val importedProfiles: List<QwdttProfile> = emptyList(),
    val generatedLinks: GeneratedLinks? = null,
    val message: String? = null,
    val error: String? = null,
) {
    val activeServer: ServerProfile?
        get() = servers.firstOrNull { it.id == activeServerId }
}
