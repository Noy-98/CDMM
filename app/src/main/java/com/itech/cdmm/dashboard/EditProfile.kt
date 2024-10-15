package com.itech.cdmm.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.itech.cdmm.databinding.FragmentEditProfileBinding

class EditProfile : Fragment() {
    private lateinit var binding: FragmentEditProfileBinding
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false)

        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            val userRef = database.getReference("StudentsTbl/$userId")
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.nameET.setText(snapshot.child("name").getValue(String::class.java) ?: "")
                    binding.courseET.setText(snapshot.child("course").getValue(String::class.java) ?: "")
                    binding.sectionET.setText(snapshot.child("section").getValue(String::class.java) ?: "")
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
        }

        // Handle save button click
        binding.btnSave.setOnClickListener {
            binding.progressbar.visibility = View.VISIBLE
            val name = binding.nameET.text.toString().trim()
            val course = binding.courseET.text.toString().trim()
            val section = binding.sectionET.text.toString().trim()

            if (validation(name, course, section)) {
                if (userId != null) {
                    val updateMap = mapOf(
                        "name" to name,
                        "course" to course,
                        "section" to section,
                    )
                    // Update user data in Firebase Realtime Database
                    database.getReference("StudentsTbl/$userId").updateChildren(updateMap)
                        .addOnSuccessListener {
                            binding.progressbar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                        }
                        .addOnFailureListener {
                            binding.progressbar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
                        }
                }
            } else {
                binding.progressbar.visibility = View.GONE
            }
        }

        binding.arrowLeft.setOnClickListener {
            val profileFragment = Profile()
            parentFragmentManager.beginTransaction()
                .replace(com.itech.cdmm.R.id.fragment_container, profileFragment)
                .addToBackStack(null)
                .commit()
        }

        return binding.root
    }

    private fun validation(name: String, course: String, section: String): Boolean {
        if (name.isEmpty()) {
            binding.nameET.error = "Name is required"
            binding.nameET.requestFocus()
            return false
        }

        if (course.isEmpty()) {
            binding.courseET.error = "Program/Course is required"
            binding.courseET.requestFocus()
            return false
        }

        if (section.isEmpty()) {
            binding.sectionET.error = "Section is required"
            binding.sectionET.requestFocus()
            return false
        }

        return true
    }
}
