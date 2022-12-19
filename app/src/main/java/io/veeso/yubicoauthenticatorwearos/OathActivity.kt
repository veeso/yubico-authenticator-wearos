package io.veeso.yubicoauthenticatorwearos

import android.app.Activity
import android.content.*
import android.nfc.Tag
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import com.yubico.yubikit.android.transport.nfc.NfcConfiguration
import com.yubico.yubikit.core.smartcard.SmartCardConnection
import com.yubico.yubikit.oath.*
import com.yubico.yubikit.android.transport.nfc.NfcYubiKeyDevice
import io.veeso.yubicoauthenticatorwearos.oath.KeyManager
import io.veeso.yubicoauthenticatorwearos.oath.calculateSteamCode
import io.veeso.yubicoauthenticatorwearos.oath.isSteamCredential
import io.veeso.yubicoauthenticatorwearos.oath.keystore.ClearingMemProvider
import io.veeso.yubicoauthenticatorwearos.oath.keystore.KeyStoreProvider
import io.veeso.yubicoauthenticatorwearos.view.CredentialsAdapter
import io.veeso.yubicoauthenticatorwearos.view.Totp
import java.util.concurrent.Executors

class OathActivity : Activity() {

    private val intentTag: String = "TAG"
    private lateinit var device: NfcYubiKeyDevice
    private lateinit var oath: OathSession
    private lateinit var credentialsView: RecyclerView
    private var lastDeviceId: String? = null
    private val memoryKeyProvider = ClearingMemProvider()
    private val keyManager by lazy {
        KeyManager(KeyStoreProvider(), memoryKeyProvider)
    }
    private val nfcConfiguration = NfcConfiguration()

    private var codes: ArrayList<Totp> = ArrayList()

    companion object {
        const val TAG = "OathActivity"
        const val NFC_DATA_CLEANUP_DELAY = 30L * 1000 // 30s
        val OTP_AID = byteArrayOf(0xa0.toByte(), 0x00, 0x00, 0x05, 0x27, 0x20, 0x01, 0x01)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.oath_activity)

        val tag = intent.getParcelableExtra<Tag>(intentTag)
        if (tag != null) {
            setupSession(tag)
        } else {
            throw Exception("Missing 'TAG' in intent parameters")
        }

        credentialsView = findViewById(R.id.tokens)
        credentialsView.adapter = CredentialsAdapter(codes)

    }

    override fun onResume() {
        super.onResume()

        // check whether is still connected
        if (lastDeviceId == null || lastDeviceId != oath.deviceId) {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun setupSession(tag: Tag) {
        val executor = Executors.newSingleThreadExecutor()
        device = NfcYubiKeyDevice(tag, nfcConfiguration.timeout, executor)
        device.requestConnection(SmartCardConnection::class.java) {
            val connection: SmartCardConnection = it.value
            oath = OathSession(connection)
            tryToUnlockOathSession()
            // set last device id if null
            if (lastDeviceId == null) {
                lastDeviceId = oath.deviceId
            }
            // if hasn't changed, recalculate oath codes, otherwise reset app
            if (oath.deviceId == lastDeviceId) {
                if (!oath.isLocked) {
                    codes = ArrayList()
                    // update credentials
                    for ((credentials, code) in calculateOathCodes(oath).entries) {
                        codes.add(Totp(credentials, code))
                    }
                }
            } else {
                // go back to main activity
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }

    private fun tryToUnlockOathSession(): Boolean {
        if (!oath.isLocked) {
            return true
        }

        val deviceId = oath.deviceId
        val accessKey = keyManager.getKey(deviceId) ?: return false

        val unlockSucceed = oath.unlock(accessKey)

        if (unlockSucceed) {
            return true
        }

        keyManager.removeKey(deviceId) // remove invalid access keys from [KeyManager]
        return false // the unlock did not work, session is locked
    }

    private fun calculateOathCodes(session: OathSession): Map<Credential, Code> {
        // NFC, need to pad timer to avoid immediate expiration
        val timestamp = System.currentTimeMillis() + 10000
        return session.calculateCodes(timestamp).map { (credential, code) ->
            Pair(
                credential,
                if (credential.isSteamCredential() && (!credential.isTouchRequired)) {
                    session.calculateSteamCode(credential, timestamp)
                } else if (credential.isTouchRequired) {
                    session.calculateCode(credential, timestamp)
                } else {
                    code
                }
            )
        }.toMap()
    }

}
