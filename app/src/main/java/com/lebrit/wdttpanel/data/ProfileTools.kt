package com.lebrit.wdttpanel.data

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.lebrit.wdttpanel.model.QwdttProfile
import com.lebrit.wdttpanel.model.QwdttSubscription
import com.lebrit.wdttpanel.model.ServerProfile
import com.lebrit.wdttpanel.model.UserSummary
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object ProfileTools {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    fun userProfile(server: ServerProfile, user: UserSummary): QwdttProfile {
        val ports = user.ports.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val host = runCatching { URI(server.baseUrl).host }.getOrNull().orEmpty()
            .ifBlank { server.baseUrl.removePrefix("https://").removePrefix("http://").trimEnd('/') }
        return QwdttProfile(
            name = user.displayName,
            peer = "$host:${ports.getOrElse(0) { "56000" }}",
            hashes = user.vkHash,
            workers = 16,
            port = ports.getOrElse(2) { "9000" }.toIntOrNull() ?: 9000,
            password = user.password,
            source = server.name,
        )
    }

    fun wdttLink(profile: QwdttProfile): String {
        val peer = profile.peer.substringBeforeLast(":")
        val dtlsPort = profile.peer.substringAfterLast(":", "56000")
        return listOf("wdtt://$peer", dtlsPort, "56001", profile.port.toString(), profile.password, profile.hashes)
            .joinToString(":")
    }

    fun qwdttLink(profile: QwdttProfile): String {
        val params = mapOf(
            "name" to profile.name,
            "peer" to profile.peer,
            "hashes" to profile.hashes,
            "workers" to profile.workers.toString(),
            "port" to profile.port.toString(),
            "pass" to profile.password,
        ).entries.joinToString("&") { (key, value) ->
            "$key=${URLEncoder.encode(value, Charsets.UTF_8.name())}"
        }
        return "qwdtt://config?$params"
    }

    fun profileJson(profile: QwdttProfile): String =
        json.encodeToString(JsonObject.serializer(), profileObject(profile))

    fun subscriptionJson(subscription: QwdttSubscription): String =
        json.encodeToString(JsonObject.serializer(), buildJsonObject {
            put("subscriptionName", subscription.name)
            put("description", subscription.description)
            put("trafficUsedMb", subscription.trafficUsedMb)
            put("updatedAt", subscription.updatedAt)
            put("version", 1)
            put("profiles", buildJsonArray {
                subscription.profiles.forEach { add(profileObject(it)) }
            })
        })

    fun qwdttFile(profile: QwdttProfile): String = profileJson(profile)

    fun parseMany(raw: String): List<QwdttProfile> {
        val text = decodeBase64OrSelf(raw.trim())
        if (text.isBlank()) return emptyList()
        if (text.startsWith("wdtt://", ignoreCase = true)) return listOf(parseWdtt(text))
        if (text.startsWith("qwdtt://", ignoreCase = true)) return listOf(parseQwdtt(text))
        val element = json.parseToJsonElement(text)
        if (element is JsonArray) return element.mapNotNull { parseJsonProfileOrNull(it) }
        val root = element.jsonObject
        val profiles = root["profiles"] ?: root["servers"]
        if (profiles is JsonArray) return profiles.mapNotNull { parseJsonProfileOrNull(it) }
        return listOfNotNull(parseJsonProfileOrNull(root))
    }

    fun qrBitmap(value: String, size: Int = 720): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    private fun parseWdtt(value: String): QwdttProfile {
        val parts = value.removePrefix("wdtt://").split(":")
        val host = parts.getOrElse(0) { "" }
        val dtls = parts.getOrElse(1) { "56000" }
        val port = parts.getOrElse(3) { "9000" }.toIntOrNull() ?: 9000
        val password = parts.getOrElse(4) { "" }
        val hash = parts.drop(5).joinToString(":")
        return QwdttProfile(
            name = host.ifBlank { "WDTT" },
            peer = "$host:$dtls",
            hashes = hash,
            workers = 16,
            port = port,
            password = password,
            source = "import",
        )
    }

    private fun parseQwdtt(value: String): QwdttProfile {
        val query = value.substringAfter("?", "")
        val params = query.split("&")
            .mapNotNull {
                val key = it.substringBefore("=", "")
                if (key.isBlank()) return@mapNotNull null
                key to URLDecoder.decode(it.substringAfter("=", ""), Charsets.UTF_8.name())
            }
            .toMap()
        val peer = params["peer"] ?: params["server"] ?: ""
        return QwdttProfile(
            name = params["name"].orEmpty().ifBlank { peer.ifBlank { "qWDTT" } },
            peer = peer.ifBlank { "${params["host"].orEmpty()}:${params["dtls_port"] ?: params["server_port"] ?: "56000"}" },
            hashes = params["hashes"] ?: params["vkHashes"] ?: "",
            workers = params["workers"]?.toIntOrNull() ?: params["workersPerHash"]?.toIntOrNull() ?: 16,
            port = params["port"]?.toIntOrNull() ?: params["listenPort"]?.toIntOrNull() ?: 9000,
            password = params["pass"] ?: params["password"] ?: "",
            source = "import",
        )
    }

    private fun parseJsonProfileOrNull(element: JsonElement): QwdttProfile? {
        val root = element as? JsonObject ?: return null
        val peer = root.text("peer").ifBlank {
            val host = root.text("host")
            val port = root.text("dtls_port").ifBlank { root.text("server_port").ifBlank { "56000" } }
            if (host.isBlank()) "" else "$host:$port"
        }
        return QwdttProfile(
            name = root.text("name").ifBlank { peer.ifBlank { "qWDTT" } },
            peer = peer,
            hashes = root.text("hashes").ifBlank { root.text("vkHashes") },
            workers = root.int("workers", root.int("workersPerHash", 16)),
            port = root.int("port", root.int("listenPort", 9000)),
            password = root.text("password").ifBlank { root.text("pass") },
            source = "import",
        )
    }

    private fun profileObject(profile: QwdttProfile): JsonObject =
        buildJsonObject {
            put("name", profile.name)
            put("peer", profile.peer)
            put("hashes", profile.hashes)
            put("workers", profile.workers)
            put("port", profile.port)
            put("password", profile.password)
        }

    private fun decodeBase64OrSelf(value: String): String {
        return runCatching {
            String(Base64.getDecoder().decode(value), Charsets.UTF_8)
        }.getOrElse { value }
    }

    private fun JsonObject.text(key: String): String =
        (this[key] as? JsonPrimitive)?.content.orEmpty()

    private fun JsonObject.int(key: String, default: Int): Int =
        this[key]?.jsonPrimitive?.intOrNull ?: this[key]?.jsonPrimitive?.doubleOrNull?.toInt() ?: default
}
