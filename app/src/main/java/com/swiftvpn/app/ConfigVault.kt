package com.swiftvpn.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the imported WireGuard configuration encrypted with an Android Keystore key.
 * The private key is never written to the repository or shared preferences.
 */
object ConfigVault {
    private const val KEY_ALIAS = "swiftvpn_wireguard_config_key"
    private const val FILE_NAME = "wireguard_config.enc"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    fun save(context: Context, config: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(config.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        File(context.filesDir, FILE_NAME).outputStream().use { output ->
            output.write(iv.size)
            output.write(iv)
            output.write(encrypted)
        }
    }

    fun hasConfig(context: Context): Boolean =
        File(context.filesDir, FILE_NAME).exists()

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}
