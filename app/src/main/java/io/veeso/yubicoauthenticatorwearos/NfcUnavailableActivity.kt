package io.veeso.yubicoauthenticatorwearos

import android.app.Activity
import android.os.Bundle

class NfcUnavailableActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.nfc_unavailable_activity)
    }

}
