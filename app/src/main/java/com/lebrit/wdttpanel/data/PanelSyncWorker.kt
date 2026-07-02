package com.lebrit.wdttpanel.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lebrit.wdttpanel.model.ServerProfile
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PanelSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    private val store = SecureServerStore(context)
    private val api = PanelApiClient()

    override suspend fun doWork(): Result {
        val stored = store.load()
        stored.servers.forEach { server ->
            runCatching {
                val authed = authenticate(server)
                val overview = api.overview(authed)
                val service = overview["service"]?.jsonObject ?: JsonObject(emptyMap())
                if (service["active"]?.jsonPrimitive?.booleanOrNull != true) {
                    NotificationHelper.notify(
                        applicationContext,
                        "WDTT остановлен",
                        "${server.name}: сервис не активен",
                    )
                }
            }.onFailure { error ->
                NotificationHelper.notify(
                    applicationContext,
                    "WDTT сервер недоступен",
                    "${server.name}: ${error.message ?: "ошибка проверки"}",
                )
            }
        }
        return Result.success()
    }

    private suspend fun authenticate(server: ServerProfile): ServerProfile {
        if (server.token.isNotBlank()) {
            runCatching { api.session(server) }.onSuccess { return server }
        }
        if (server.password.isBlank()) return server
        val login = api.login(server, server.password)
        return server.copy(token = login.token, version = login.version.ifBlank { server.version })
    }

    companion object {
        private const val WORK_NAME = "wdtt-panel-background-sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<PanelSyncWorker>(30, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
