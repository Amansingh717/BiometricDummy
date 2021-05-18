package com.example.biometricdummy

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.Charset
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface CryptographyManager {

    /**
     * This method first gets or generates an instance of SecretKey and then initializes the Cipher
     * with the key. The secret key uses [ENCRYPT_MODE][Cipher.ENCRYPT_MODE] is used.
     */
    fun getInitializedCipherForEncryption(keyName: String): Cipher?

    /**
     * This method first gets or generates an instance of SecretKey and then initializes the Cipher
     * with the key. The secret key uses [DECRYPT_MODE][Cipher.DECRYPT_MODE] is used.
     */
    fun getInitializedCipherForDecryption(keyName: String): Cipher?

    /**
     * The Cipher created with [getInitializedCipherForEncryption] is used here
     */
    fun encryptData(plaintext: String, cipher: Cipher): String?

    /**
     * The Cipher created with [getInitializedCipherForDecryption] is used here
     */
    fun decryptData(cipher: Cipher): String?

}

fun cryptographyManager(): CryptographyManager = CryptographyManagerImpl()

private class CryptographyManagerImpl : CryptographyManager {
    companion object {
        private const val KEY_SIZE: Int = 256
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
        private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
        private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val CHARSET = "UTF-8"
    }

    override fun getInitializedCipherForEncryption(keyName: String): Cipher? {
        try {
            val cipher = getCipher()
            val secretKey = getOrCreateSecretKey(keyName)
            if (secretKey != null) {
                try {
                    cipher?.init(
                        Cipher.ENCRYPT_MODE,
                        secretKey
                    )
                } catch (e: KeyPermanentlyInvalidatedException) {
                    try {
                        //User need to re-setup fingerprint auth in the app
                        val keyStore: KeyStore? = KeyStore.getInstance(ANDROID_KEYSTORE)
                        keyStore?.load(null)
                        keyStore?.deleteEntry(keyName)
                        PreferenceHelper.setBase64EncryptionIv("")
                        PreferenceHelper.setBase64SecretText("")
                    } catch (e: Exception) {
                        //this line is reached only if the exception occurs in deleting entry from keyStore
                        return null
                    }
                    //this line is reached only if the exception occurs in cipher initialization, user get null cipher
                    return null
                }
                //this line is reached for successful cipher initialization
                return cipher
            }
            //this line is reached only if secretKey is null
            return null
        } catch (e: Exception) {
            //this line is reached only if any exception is thrown in above process
            return null
        }
    }

    override fun getInitializedCipherForDecryption(keyName: String): Cipher? {
        try {
            //obtaining initializationVector from sharedPreferences and using it for decryption
            val base64EncryptionIv = PreferenceHelper.getBase64EncryptionIv()
            if (base64EncryptionIv.isNotBlank()) {
                val initializationVector: ByteArray? =
                    Base64.decode(base64EncryptionIv, Base64.DEFAULT)
                val cipher = getCipher()
                val secretKey = getOrCreateSecretKey(keyName)
                if (secretKey != null && initializationVector?.isNotEmpty() == true) {
                    try {
                        cipher?.init(
                            Cipher.DECRYPT_MODE,
                            secretKey,
                            GCMParameterSpec(128, initializationVector)
                        )
                    } catch (e: KeyPermanentlyInvalidatedException) {
                        try {
                            //User need to re-setup fingerprint auth in the app
                            val keyStore: KeyStore? = KeyStore.getInstance(ANDROID_KEYSTORE)
                            keyStore?.load(null)
                            keyStore?.deleteEntry(keyName)
                            PreferenceHelper.setBase64EncryptionIv("")
                            PreferenceHelper.setBase64SecretText("")
                        } catch (e: Exception) {
                            //this line is reached only if the exception occurs in deleting entry from keyStore
                            return null
                        }
                        //this line is reached only if the exception occurs in cipher initialization, user get null cipher
                        return null
                    }
                    //this line is reached for successful cipher initialization
                    return cipher
                }
                //this line is reached only if secretKey is null OR iv is null
                return null
            }
            //this line is reached only if base64EncryptionIv is null
            return null
        } catch (e: Exception) {
            //this line is reached only if exception thrown in above process
            return null
        }
    }

    override fun encryptData(plaintext: String, cipher: Cipher): String? {
        try {
            val cipherText = cipher.doFinal(plaintext.toByteArray(Charset.forName(CHARSET)))
            val initializationVector: ByteArray? = cipher.iv

            if (cipherText?.isNotEmpty() == true && initializationVector?.isNotEmpty() == true) {
                //saving initializationVector and cipherText to sharedPreferences
                val base64initializationVector =
                    Base64.encodeToString(initializationVector, Base64.DEFAULT)
                val base64SecretText = Base64.encodeToString(cipherText, Base64.DEFAULT)
                base64initializationVector?.let { PreferenceHelper.setBase64EncryptionIv(it) }
                base64SecretText?.let { PreferenceHelper.setBase64SecretText(it) }
            }
            return String(cipherText, Charset.forName(CHARSET))
        } catch (e: Exception) {
            //this line is reached only if exception is thrown in above process
            return null
        }
    }

    override fun decryptData(cipher: Cipher): String? {
        try {
            //obtaining cipherText from sharedPreferences and using it for decryption
            val base64cipherText = PreferenceHelper.getBase64SecretText()
            if (base64cipherText.isNotBlank()) {
                val cipherText: ByteArray? = Base64.decode(base64cipherText, Base64.DEFAULT)
                val plaintext = cipher.doFinal(cipherText)
                return String(plaintext, Charset.forName(CHARSET))
            }
            //this line is reached if base64cipherText is blank
            return null
        } catch (e: Exception) {
            //this line is reached only if exception is thrown in above process
            return null
        }
    }

    private fun getCipher(): Cipher? {
        return try {
            val transformation = "$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING"
            Cipher.getInstance(transformation)
        } catch (e: Exception) {
            null
        }
    }

    private fun getOrCreateSecretKey(keyName: String): SecretKey? {
        try {
            // If SecretKey was previously created for that keyName, then grab and return it.
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null) // Keystore must be loaded before it can be accessed
            keyStore.getKey(keyName, null)?.let { return it as SecretKey }

            // if you reach here, then a new SecretKey must be generated for that keyName
            val paramsBuilder = KeyGenParameterSpec.Builder(
                keyName,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
            paramsBuilder.apply {
                setBlockModes(ENCRYPTION_BLOCK_MODE)
                setEncryptionPaddings(ENCRYPTION_PADDING)
                setKeySize(KEY_SIZE)
                setUserAuthenticationRequired(true)
            }

            val keyGenParams = paramsBuilder.build()
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            keyGenerator.init(keyGenParams)
            return keyGenerator.generateKey()
        } catch (e: Exception) {
            return null
        }
    }
}