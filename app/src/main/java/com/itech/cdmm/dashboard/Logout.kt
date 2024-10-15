package com.itech.cdmm.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.itech.cdmm.LoginActivity

class Logout : Fragment() {
    private val firebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth.signOut()
        Toast.makeText(activity, "Logged out successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(activity, LoginActivity::class.java))
        activity?.finish()
    }
}