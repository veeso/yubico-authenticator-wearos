package io.veeso.yubicoauthenticatorwearos.view

import com.yubico.yubikit.oath.Code
import com.yubico.yubikit.oath.Credential

class Totp(private val credential: Credential, private val code: Code) {

    fun getCredential(): Credential {
        return credential
    }

    fun getCode(): Code {
        return code
    }

}
