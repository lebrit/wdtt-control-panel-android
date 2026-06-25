package com.lebrit.wdttpanel.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.lebrit.wdttpanel.model.StoredState
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SecureServerStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("wdtt_servers_secure", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): StoredState = withContext(Dispatchers.IO) {
        val iv = prefs.getString(KEY_IV, null)
        val payload = prefs.getString(KEY_PAYLOAD, null)
        if (iv.isNullOrBlank() || payload.isNullOrBlank()) {
            return@withContext StoredState()
        }
        runCatching {
            val plain = decrypt(iv, payload)
            json.decodeFromString<StoredState>(plain)
        }.getOrDefault(StoredState())
    }

    suspend fun save(state: StoredState) = withContext(Dispatchers.IO) {
        val encrypted = encrypt(json.encodeToString(state))
        prefs.edit()
            .putString(KEY_IV, encrypted.iv)
            .putString(KEY_PAYLOAD, encrypted.payload)
            .apply()
    }

    private fun encrypt(value: String): EncryptedPayload {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(
            iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP),
            payload = Base64.encodeToString(encrypted, Base64.NO_WRAP),
        )
    }

    private fun decrypt(iv: String, payload: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
        val plain = cipher.doFinal(Base64.decode(payload, Base64.NO_WRAP))
        return plain.toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private data class EncryptedPayload(val iv: String, val payload: String)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "wdtt_panel_servers_v1"
        const val KEY_IV = "state_iv"
        const val KEY_PAYLOAD = "state_payload"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
