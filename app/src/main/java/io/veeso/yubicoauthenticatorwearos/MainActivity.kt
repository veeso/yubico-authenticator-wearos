package io.veeso.yubicoauthenticatorwearos

import android.app.Activity
import android.content.*
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import com.yubico.yubikit.android.transport.nfc.NfcConfiguration
import com.yubico.yubikit.android.transport.nfc.NfcNotAvailable
import com.yubico.yubikit.android.transport.nfc.NfcYubiKeyDevice
import com.yubico.yubikit.android.YubiKitManager
import com.yubico.yubikit.core.Logger
import io.veeso.yubicoauthenticatorwearos.databinding.ActivityMainBinding
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var yubikit: YubiKitManager

    private var hasNfc: Boolean = false
    private val nfcConfiguration = NfcConfiguration()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        allowScreenshots(false)
        yubikit = YubiKitManager(this)
        setupYubikitLogger()

        setContentView(binding.root)
    }

    override fun onPause() {
        stopNfcDiscovery()

        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        // Handle existing tag when launched from NDEF
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            intent.removeExtra(NfcAdapter.EXTRA_TAG)

            val executor = Executors.newSingleThreadExecutor()
            val device = NfcYubiKeyDevice(tag, nfcConfiguration.timeout, executor)
            /*
            lifecycleScope.launch {
                try {
                    // @TODO: process device
                    // contextManager?.processYubiKey(device)
                    device.remove {
                        executor.shutdown()
                        startNfcDiscovery()
                    }
                } catch (e: Throwable) {
                    Log.e(TAG, "Error processing YubiKey in AppContextManager", e.toString())
                }
            }
            */

        } else {
            startNfcDiscovery()
        }
    }

    private fun startNfcDiscovery() =
        try {
            Log.d(TAG, "Starting nfc discovery")
            yubikit.startNfcDiscovery(nfcConfiguration, this, ::processYubiKey)
            hasNfc = true
        } catch (e: NfcNotAvailable) {
            hasNfc = false
        }

    private fun stopNfcDiscovery() {
        if (hasNfc) {
            yubikit.stopNfcDiscovery(this)
            Log.d(TAG, "Stopped nfc discovery")
        }
    }

    private fun setupYubikitLogger() {
        Logger.setLogger(object : Logger() {
            private val TAG = "yubikit"

            override fun logDebug(message: String) {
                // redirect yubikit debug logs to traffic
                Log.d(TAG, message)
            }

            override fun logError(message: String, throwable: Throwable) {
                Log.e(TAG, message, throwable)
            }
        })
    }

    private fun allowScreenshots(value: Boolean): Boolean {
        // Note that FLAG_SECURE is the inverse of allowScreenshots
        if (value) {
            Log.d(TAG, "Clearing FLAG_SECURE (allow screenshots)")
            window.clearFlags(FLAG_SECURE)
        } else {
            Log.d(TAG, "Setting FLAG_SECURE (disallow screenshots)")
            window.setFlags(FLAG_SECURE, FLAG_SECURE)
        }

        return FLAG_SECURE != (window.attributes.flags and FLAG_SECURE)
    }

    companion object {
        const val TAG = "MainActivity"
        const val FLAG_SECURE = WindowManager.LayoutParams.FLAG_SECURE
    }
}