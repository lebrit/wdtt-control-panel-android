package com.lebrit.wdttpanel.data

import com.lebrit.wdttpanel.model.ServerProfile
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class PanelApiClient {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val normalClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()
    private val insecureClient by lazy { buildInsecureClient() }

    suspend fun info(profile: ServerProfile): JsonObject =
        request(profile = profile, route = "info", auth = false).jsonObject

    suspend fun login(profile: ServerProfile, password: String): LoginResult {
        val payload = buildJsonObject {
            if (profile.username.isNotBlank()) {
                put("username", profile.username)
            }
            put("password", password)
        }
        val result = request(
            profile = profile,
            route = "auth/login",
            method = "POST",
            payload = payload,
            auth = false,
        ).jsonObject
        val token = result["token"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (token.isBlank()) {
            throw ApiException("Сервер не вернул токен")
        }
        val server = result["server"]?.jsonObject ?: JsonObject(emptyMap())
        val version = server["panel_version"]?.jsonPrimitive?.contentOrNull.orEmpty()
        return LoginResult(token = token, version = version)
    }

    suspend fun session(profile: ServerProfile): JsonObject =
        request(profile = profile, route = "auth/session").jsonObject

    suspend fun overview(profile: ServerProfile): JsonObject =
        request(profile = profile, route = "overview").jsonObject

    suspend fun users(profile: ServerProfile): JsonObject =
        request(profile = profile, route = "users").jsonObject

    suspend fun logs(profile: ServerProfile, source: String = "wdtt", limit: Int = 300): JsonObject =
        request(profile = profile, route = "logs?source=$source&limit=$limit").jsonObject

    suspend fun post(profile: ServerProfile, route: String, payload: JsonObject = JsonObject(emptyMap())): JsonElement =
        request(profile = profile, route = route, method = "POST", payload = payload)

    private suspend fun request(
        profile: ServerProfile,
        route: String,
        method: String = "GET",
        payload: JsonObject? = null,
        auth: Boolean = true,
    ): JsonElement = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url(apiUrl(profile.baseUrl, route))
            .header("Accept", "application/json")
        if (auth) {
            if (profile.token.isBlank()) {
                throw ApiException("Нужно войти на сервер")
            }
            builder.header("Authorization", "Bearer ${profile.token}")
        }
        if (method == "POST") {
            val body = (payload ?: JsonObject(emptyMap())).toString().toRequestBody(jsonMediaType)
            builder.post(body)
        } else {
            builder.get()
        }

        val response = client(profile.allowInsecureTls).newCall(builder.build()).execute()
        response.use {
            val text = it.body.string()
            val root = runCatching { json.parseToJsonElement(text).jsonObject }
                .getOrElse { throw ApiException("Сервер вернул не JSON: HTTP ${response.code}") }
            val ok = root["ok"]?.jsonPrimitive?.booleanOrNull ?: it.isSuccessful
            if (!it.isSuccessful || !ok) {
                val error = root["error"]?.jsonPrimitive?.contentOrNull ?: "HTTP ${it.code}"
                throw ApiException(error)
            }
            root["result"] ?: root
        }
    }

    private fun apiUrl(baseUrl: String, route: String): String {
        val normalized = normalizeBaseUrl(baseUrl).trimEnd('/')
        return "$normalized/api/v1/${route.trimStart('/')}"
    }

    fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim()
        val withScheme = when {
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "https://$trimmed"
        }
        return withScheme.trimEnd('/') + "/"
    }

    private fun client(allowInsecureTls: Boolean): OkHttpClient =
        if (allowInsecureTls) insecureClient else normalClient

    private fun buildInsecureClient(): OkHttpClient {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
    }
}

data class LoginResult(
    val token: String,
    val version: String,
)

class ApiException(message: String) : RuntimeException(message)
