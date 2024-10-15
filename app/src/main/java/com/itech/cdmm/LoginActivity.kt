package com.itech.cdmm

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.itech.cdmm.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private var isPassVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, MainHomeDashboard::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        binding.btnSignIn.setOnClickListener {
            login()
        }

        binding.passLayout.setEndIconOnClickListener {
            togglePass()
        }

        binding.btnForgotPass.setOnClickListener {
            handleForgot()
        }
        binding.signupLink.setOnClickListener {
            handleSignUp()
        }
    }

    private fun login() {
        binding.progressbar.visibility = View.VISIBLE
        val email = binding.emailET.text.toString().trim()
        val pass = binding.passET.text.toString().trim()
        val emailPattern = "[a-zA-Z0-9._-]+@student\\.pnm\\.edu\\.ph"

        if (email.isEmpty() || pass.isEmpty()) {
            if(email.isEmpty()) {
                binding.emailET.error = "Email is required!"
                binding.emailET.requestFocus()
                binding.progressbar.visibility = View.GONE
                return
            }
            if(pass.isEmpty()) {
                binding.passET.error = "Password is required!"
                binding.passET.requestFocus()
                binding.progressbar.visibility = View.GONE
                return
            }
            Toast.makeText(this,"All fields are Required!", Toast.LENGTH_SHORT).show()
            binding.progressbar.visibility = View.GONE
            return
        } else if (!email.matches(emailPattern.toRegex())){
            binding.emailET.error = "Wrong Email Address"
            binding.emailET.requestFocus()
            binding.progressbar.visibility = View.GONE
            return
        } else if (pass.length < 6){
            binding.passET.error = "Wrong Password"
            binding.passET.requestFocus()
            binding.progressbar.visibility = View.GONE
            return
        } else {
            auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener {loginTask ->
                if (loginTask.isSuccessful){
                    val user = auth.currentUser
                    if (user != null) {
                        binding.progressbar.visibility = View.GONE
                        val intent = Intent(this, MainHomeDashboard::class.java)
                        startActivity(intent)
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        finish()
                        Toast.makeText(this, "Login Successfully", Toast.LENGTH_SHORT).show()
                    }
                }else{
                    binding.progressbar.visibility = View.GONE
                    Toast.makeText(this, "Something went wrong, try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun togglePass() {
        if (isPassVisible) {
            binding.passET.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.passLayout.endIconDrawable = AppCompatResources.getDrawable(this, R.drawable.visibility_off)
        } else {
            binding.passET.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.passLayout.endIconDrawable = AppCompatResources.getDrawable(this, R.drawable.visibility)
        }
        binding.passET.text?.let {
            binding.passET.setSelection(it.length)
        }
        isPassVisible = !isPassVisible
    }

    private fun handleSignUp() {
        val intent = Intent(this, SignupActivity::class.java)
        startActivity(intent)
    }

    private fun handleForgot() {
        val intent = Intent(this, ForgotPasswordActivity::class.java)
        startActivity(intent)
    }
}