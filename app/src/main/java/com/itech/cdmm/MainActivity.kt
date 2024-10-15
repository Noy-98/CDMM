package com.itech.cdmm

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.itech.cdmm.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fadeInAnimation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val scaleAnimation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.scale)

        binding.mainLogo.startAnimation(fadeInAnimation)
        binding.mainLogo.startAnimation(scaleAnimation)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 3000)
    }
}