package com.itech.cdmm.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.appcompat.content.res.AppCompatResources
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.itech.cdmm.R
import com.itech.cdmm.databinding.FragmentChangePasswordBinding

class ChangePassword : Fragment() {
    private lateinit var binding: FragmentChangePasswordBinding
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var isPassVisible: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChangePasswordBinding.inflate(inflater, container, false)

        binding.conLayout.setEndIconOnClickListener {
            togglePass2()
        }

        binding.passLayout.setEndIconOnClickListener {
            togglePass()
        }

        binding.btnSave.setOnClickListener {
            validateAndChangePassword()
        }

        binding.arrowLeft.setOnClickListener {
            val profileFragment = Profile()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, profileFragment)
                .addToBackStack(null)
                .commit()
        }

        return binding.root
    }

    private fun togglePass() {
        if (isPassVisible) {
            binding.passET.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.passLayout.endIconDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.visibility_off)
        } else {
            binding.passET.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.passLayout.endIconDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.visibility)
        }
        binding.passET.text?.let {
            binding.passET.setSelection(it.length)
        }
        isPassVisible = !isPassVisible
    }

    private fun togglePass2() {
        if (isPassVisible) {
            binding.cpassET.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.conLayout.endIconDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.visibility_off)
        } else {
            binding.cpassET.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.conLayout.endIconDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.visibility)
        }
        binding.cpassET.text?.let {
            binding.cpassET.setSelection(it.length)
        }
        isPassVisible = !isPassVisible
    }

    private fun validateAndChangePassword() {
        val currentPassword = binding.currentPassET.text.toString().trim()
        val newPassword = binding.passET.text.toString().trim()
        val confirmPassword = binding.cpassET.text.toString().trim()

        // Check if current password is provided
        if (currentPassword.isEmpty()) {
            binding.currentPassET.error = "Current Password is required"
            binding.currentPassET.requestFocus()
            return
        }

        // Validate the new password
        if (newPassword.isEmpty()) {
            binding.passET.error = "New Password is required"
            binding.passET.requestFocus()
            return
        }

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            binding.cpassET.error = "Confirm Password is required"
            binding.cpassET.requestFocus()
            return
        }

        // Check if the password is at least 6 characters
        if (newPassword.length < 6) {
            binding.passET.error = "New Password must be at least 6 characters"
            binding.passET.requestFocus()
            return
        }

        // Check if new password and confirm password match
        if (newPassword != confirmPassword) {
            binding.cpassET.error = "Passwords do not match"
            binding.cpassET.requestFocus()
            return
        }

        // Show progress bar while changing password
        binding.progressbar.visibility = View.VISIBLE

        val user = firebaseAuth.currentUser

        // Re-authenticate the user with the current password before updating the password
        if (user != null && user.email != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
            user.reauthenticate(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Re-authentication successful, update the password
                    user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                        binding.progressbar.visibility = View.GONE
                        if (updateTask.isSuccessful) {
                            Toast.makeText(requireContext(), "Password changed successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Failed to change password: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    binding.progressbar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Current password is incorrect", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            binding.progressbar.visibility = View.GONE
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }
}
