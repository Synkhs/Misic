package com.example.core.accounts

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

data class StoredOAuthToken(
    val accessToken: String,
    val refreshToken: String = "",
    val expiresAtMs: Long = 0L,
    val scope: String = "",
    val accountLabel: String = "",
    val accountHandle: String = "",
)

/**
 * Hardware-backed encrypted token vault powered by Google Tink (AES256_GCM) + Android Keystore.
 * Zero plaintext secrets written to disk on real devices. Includes an isolated in-memory store
 * only when running under Robolectric unit tests without AndroidKeyStore support.
 */
class SecureTokenStore(context: Context) {

    private val appCtx = context.applicationContext
    private val prefs: SharedPreferences =
        appCtx.getSharedPreferences(TOKEN_PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var cachedAead: Aead? = null

    private fun getOrInitAead(): Aead? {
        cachedAead?.let { return it }
        synchronized(this) {
            cachedAead?.let { return it }
            val aead = runCatching {
                AeadConfig.register()
                AndroidKeysetManager.Builder()
                    .withSharedPref(appCtx, KEYSET_NAME, KEYSET_PREF_NAME)
                    .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                    .withMasterKeyUri(MASTER_KEY_URI)
                    .build()
                    .keysetHandle
                    .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
            }.getOrNull()
            cachedAead = aead
            return aead
        }
    }

    fun saveToken(sourceId: String, token: StoredOAuthToken) {
        val json = JSONObject().apply {
            put("accessToken", token.accessToken)
            put("refreshToken", token.refreshToken)
            put("expiresAtMs", token.expiresAtMs)
            put("scope", token.scope)
            put("accountLabel", token.accountLabel)
            put("accountHandle", token.accountHandle)
        }.toString()

        val aead = getOrInitAead()
        if (aead != null) {
            val cipher = aead.encrypt(
                json.toByteArray(StandardCharsets.UTF_8),
                sourceId.toByteArray(StandardCharsets.UTF_8),
            )
            prefs.edit()
                .putString(keyFor(sourceId), Base64.encodeToString(cipher, Base64.NO_WRAP))
                .apply()
        } else if (isTestRuntime()) {
            testMemoryFallback[sourceId] = token
        }
    }

    fun readToken(sourceId: String): StoredOAuthToken? {
        val aead = getOrInitAead()
        if (aead != null) {
            val encoded = prefs.getString(keyFor(sourceId), null) ?: return null
            return runCatching {
                val cipher = Base64.decode(encoded, Base64.NO_WRAP)
                val plain = aead.decrypt(cipher, sourceId.toByteArray(StandardCharsets.UTF_8))
                val obj = JSONObject(String(plain, StandardCharsets.UTF_8))
                StoredOAuthToken(
                    accessToken = obj.optString("accessToken", ""),
                    refreshToken = obj.optString("refreshToken", ""),
                    expiresAtMs = obj.optLong("expiresAtMs", 0L),
                    scope = obj.optString("scope", ""),
                    accountLabel = obj.optString("accountLabel", ""),
                    accountHandle = obj.optString("accountHandle", ""),
                ).takeIf { it.accessToken.isNotBlank() }
            }.getOrElse {
                deleteToken(sourceId)
                null
            }
        }
        return if (isTestRuntime()) testMemoryFallback[sourceId] else null
    }

    fun deleteToken(sourceId: String) {
        prefs.edit().remove(keyFor(sourceId)).apply()
        testMemoryFallback.remove(sourceId)
    }

    fun clearAllTokens() {
        prefs.edit().clear().apply()
        testMemoryFallback.clear()
    }

    private fun keyFor(sourceId: String): String = "enc_token_$sourceId"

    private fun isTestRuntime(): Boolean =
        Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

    companion object {
        private const val KEYSET_NAME = "magicmusic_token_keyset"
        private const val KEYSET_PREF_NAME = "magicmusic_tink_keyset_prefs"
        private const val TOKEN_PREFS_NAME = "magicmusic_encrypted_vault"
        private const val MASTER_KEY_URI = "android-keystore://magicmusic_master_key"
        private val testMemoryFallback = ConcurrentHashMap<String, StoredOAuthToken>()
    }
}
