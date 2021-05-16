package com.example.biometricdummy

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.charset.Charset

private const val TAG = "biometricDummy"

class MainActivity : AppCompatActivity() {
    private lateinit var cryptographyManager: CryptographyManager
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var secretKeyName: String
    private var readyToEncrypt: Boolean = false
    private lateinit var cipherText: ByteArray
    private lateinit var initializationVector: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cryptographyManager = cryptographyManager()
        biometricPrompt = createBiometricPrompt()
        promptInfo = createPromptInfo()
        secretKeyName = getString(R.string.secret_key_name)

        buttonEncrypt.setOnClickListener { authenticateToEncrypt() }
        buttonDecrypt.setOnClickListener { authenticateToDecrypt() }
    }


    private fun createBiometricPrompt(): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.d(TAG, "$errorCode :: $errString")
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.d(TAG, "Authentication failed for an unknown reason")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d(TAG, "Authentication was successful")
                processData(result.cryptoObject)
            }
        }

        //The API requires the client/Activity context for displaying the prompt
        return BiometricPrompt(this, executor, callback)
    }

    private fun createPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            // e.g. "Sign in"
            .setTitle(getString(R.string.prompt_info_title))
            // e.g. "Biometric for My App"
            .setSubtitle(getString(R.string.prompt_info_subtitle))
            // e.g. "Confirm biometric to continue"
            .setDescription(getString(R.string.prompt_info_description))
            .setConfirmationRequired(false)
            .setNegativeButtonText(getString(R.string.prompt_info_use_app_password))
            // .setDeviceCredentialAllowed(true) // Allow PIN/pattern/password authentication.
            // Also note that setDeviceCredentialAllowed and setNegativeButtonText are
            // incompatible so that if you uncomment one you must comment out the other
            .build()
    }

    private fun authenticateToEncrypt() {
        readyToEncrypt = true
        if (BiometricManager.from(applicationContext)
                .canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
        ) {
            val cipher = cryptographyManager.getInitializedCipherForEncryption(secretKeyName)
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun authenticateToDecrypt() {
        readyToEncrypt = false
        if (BiometricManager.from(applicationContext).canAuthenticate() == BiometricManager
                .BIOMETRIC_SUCCESS
        ) {
            val cipher = cryptographyManager.getInitializedCipherForDecryption(
                secretKeyName,
                initializationVector
            )
            biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
        }
    }

    private fun processData(cryptoObject: BiometricPrompt.CryptoObject?) {
        val data = if (readyToEncrypt) {
            val text = editText.text.toString()
            val encryptedData = cryptographyManager.encryptData(text, cryptoObject?.cipher!!)
            cipherText = encryptedData.cipherText
            initializationVector = encryptedData.initializationVector

            String(cipherText, Charset.forName("UTF-8"))
        } else {
            cryptographyManager.decryptData(cipherText, cryptoObject?.cipher!!)
        }
        textView.text = data
    }
}