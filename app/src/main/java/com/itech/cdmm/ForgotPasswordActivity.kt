package com.itech.cdmm

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.itech.cdmm.databinding.ActivityForgotPasswordBinding

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseApp.initializeApp(this)
        firebaseAuth = FirebaseAuth.getInstance()
        database = Firebase.database

        binding.btnReset.setOnClickListener {
            handleReset()
        }

        binding.signinLink.setOnClickListener {
            handleSigninLinkClick()
        }
    }

    private fun handleReset() {
        binding.progressbar.visibility = View.VISIBLE
        val email = binding.emailET.text.toString()
        val emailPattern = "[a-zA-Z0-9._-]+@student\\.pnm\\.edu\\.ph"

        if(email.isEmpty()) {
            binding.emailET.error = "Email is required!"
            binding.emailET.requestFocus()
            binding.progressbar.visibility = View.GONE
            return
        } else if(!email.matches(emailPattern.toRegex())) {
            binding.emailET.error = "Invalid email address!"
            binding.emailET.requestFocus()
            binding.progressbar.visibility = View.GONE
            return
        } else {
            checkIfEmailExists(email)
        }
    }

    private fun checkIfEmailExists(email: String) {
        val usersRef = database.getReference("StudentsTbl")

        usersRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    sendPasswordResetEmail(email)
                } else {
                    Toast.makeText(this@ForgotPasswordActivity, "Email is not registered in the system", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ForgotPasswordActivity, "Failed to check email: ${error.message}", Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "Reset password successfully! Please check your email.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, it.toString(), Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleSigninLinkClick() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}