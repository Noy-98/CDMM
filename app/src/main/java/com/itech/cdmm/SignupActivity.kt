package com.itech.cdmm

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.itech.cdmm.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.signupLink.setOnClickListener {
            handleLoginLinkClick()
        }
    }

    private fun handleLoginLinkClick() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}