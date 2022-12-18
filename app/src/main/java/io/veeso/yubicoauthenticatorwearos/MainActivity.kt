package io.veeso.yubicoauthenticatorwearos

import android.app.Activity
import android.os.Bundle
import io.veeso.yubicoauthenticatorwearos.databinding.ActivityMainBinding

class MainActivity : Activity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}