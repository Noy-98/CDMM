package com.itech.cdmm

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.itech.cdmm.databinding.ActivityForgotPasswordBinding

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signinLink.setOnClickListener {
            handleSigninLinkClick()
        }
    }

    private fun handleSigninLinkClick() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}