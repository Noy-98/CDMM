package com.itech.cdmm

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.itech.cdmm.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var isPassVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseApp.initializeApp(this)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        binding.btnRegister.setOnClickListener {
            submit()
        }

        binding.conLayout.setEndIconOnClickListener {
            togglePass2()
        }

        binding.passLayout.setEndIconOnClickListener {
            togglePass()
        }

        binding.signupLink.setOnClickListener {
            handleLoginLinkClick()
        }
    }

    private fun submit() {
        binding.progressbar.visibility = View.VISIBLE
        val name = binding.nameET.text.toString().trim()
        val email = binding.emailET.text.toString().trim()
        val pass = binding.passET.text.toString().trim()
        val cpass = binding.cpassET.text.toString().trim()
        val section = binding.sectionET.text.toString().trim()
        val course = binding.courseET.text.toString().trim()
        val emailPattern = "[a-zA-Z0-9._-]+@student\\.pnm\\.edu\\.ph"

        if(name.isEmpty() || section.isEmpty() || course.isEmpty() || email.isEmpty() || pass.isEmpty() || cpass.isEmpty()) {
            if(name.isEmpty()) {
                binding.nameET.error = "Name is required!"
                binding.nameET.requestFocus()
                binding.progressbar.visibility = View.GONE
                return
            }
            if(section.isEmpty()) {
                binding.sectionET.error = "Section is required!"
                binding.sectionET.requestFocus()
                binding.progressbar.visibility = View.GONE
                return
            }
            if(course.isEmpty()) {
                binding.courseET.error = "Program/Course is required!"
                binding.courseET.requestFocus()
                binding.progressbar.visibility = View.GONE
                return
            }
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
            if(cpass.isEmpty()) {
                binding.cpassET.error = "Confirm Password is required!"
                binding.cpassET.requestFocus()
                binding.progressbar.visibility = View.GONE
                return
            }
            Toast.makeText(this,"All fields are Required!", Toast.LENGTH_SHORT).show()
            binding.progressbar.visibility = View.GONE
            return
        } else if (!email.matches(emailPattern.toRegex())){
            binding.emailET.error = "Enter valid email address"
            binding.emailET.requestFocus()
            binding.progressbar.visibility = View.GONE
            return
        } else if (pass.length < 6){
            binding.passET.error = "Enter your password more than 6 characters"
            binding.passET.requestFocus()
            binding.progressbar.visibility = View.GONE
            return
        } else if (pass != cpass){
            binding.cpassET.error = "Password not match"
            binding.cpassET.requestFocus()
            binding.progressbar.visibility = View.GONE
            return
        } else {
            createAccount(name, email, pass, section, course)
        }
    }

    private fun createAccount(name: String, email: String, pass: String, section: String, course: String) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid
                    val timestamp = Timestamp.now()

                    val userData = hashMapOf(
                        "name" to name,
                        "email" to email,
                        "section" to section,
                        "course" to course,
                        "createdAt" to timestamp,
                    )

                    if (uid != null) {
                        database.reference.child("StudentsTbl").child(uid).setValue(userData)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this, "Student registered successfully", Toast.LENGTH_SHORT).show()
                                    binding.progressbar.visibility = View.GONE
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                } else {
                                    binding.progressbar.visibility = View.GONE
                                    Toast.makeText(this, "Failed to register student: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }

                } else {
                    binding.progressbar.visibility = View.GONE
                    Toast.makeText(this, "${task.exception?.message}", Toast.LENGTH_SHORT).show()
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

    private fun togglePass2() {
        if (isPassVisible) {
            binding.cpassET.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.conLayout.endIconDrawable = AppCompatResources.getDrawable(this, R.drawable.visibility_off)
        } else {
            binding.cpassET.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.conLayout.endIconDrawable = AppCompatResources.getDrawable(this, R.drawable.visibility)
        }
        binding.cpassET.text?.let {
            binding.cpassET.setSelection(it.length)
        }
        isPassVisible = !isPassVisible
    }

    private fun handleLoginLinkClick() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}