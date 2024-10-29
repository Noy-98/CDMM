package com.itech.cdmm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.itech.cdmm.dashboard.Cart
import com.itech.cdmm.dashboard.Home
import com.itech.cdmm.dashboard.Logout
import com.itech.cdmm.dashboard.Notification
import com.itech.cdmm.dashboard.Profile
import com.itech.cdmm.databinding.ActivityMainHomeDashboardBinding

class MainHomeDashboard : AppCompatActivity() {

    private lateinit var binding: ActivityMainHomeDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainHomeDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        loadFragment(Home())
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNavigationView.selectedItemId = R.id.home

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    loadFragment(Home())
                    true
                }
                R.id.cart -> {
                    loadFragment(Cart())
                    true
                }
                R.id.notification -> {
                    loadFragment(Notification())
                    true
                }
                R.id.profile -> {
                    loadFragment(Profile())
                    true
                }
                R.id.logout -> {
                    loadFragment(Logout())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}