package com.itech.cdmm.dashboard

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itech.cdmm.R
import com.itech.cdmm.databinding.FragmentProfileBinding
import com.squareup.picasso.Picasso
import java.util.UUID

class Profile : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val storageReference: StorageReference = storage.reference
    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        displayUser()

        binding.profilePic.setOnClickListener {
            openGallery()
        }

        binding.editBttn.setOnClickListener {
            val editProfile = EditProfile()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, editProfile)
                .addToBackStack(null)
                .commit()
        }

        binding.changePaswordBttn.setOnClickListener {
            val changePassword = ChangePassword()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, changePassword)
                .addToBackStack(null)
                .commit()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            Log.d("Profile", "Image URI: $imageUri")
            if (imageUri != null) {
                binding.progressbar.visibility = View.VISIBLE
                deleteOldImage()
            }
        }
    }

    private fun deleteOldImage() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val usersReference = FirebaseDatabase.getInstance().getReference("StudentsTbl/$uid")

            usersReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return // Check if view is still available
                    val profileImageUrl = snapshot.child("profile_picture").getValue(String::class.java)
                    if (profileImageUrl != null) {
                        val oldAvatarRef = storage.getReferenceFromUrl(profileImageUrl)
                        oldAvatarRef.delete().addOnSuccessListener {
                            if (_binding == null) return@addOnSuccessListener
                            Toast.makeText(requireContext(), "Old photo deleted", Toast.LENGTH_SHORT).show()
                            storeImage()
                        }.addOnFailureListener { error ->
                            if (_binding == null) return@addOnFailureListener
                            Toast.makeText(requireContext(), "Failed to delete old photo", Toast.LENGTH_SHORT).show()
                            storeImage()
                        }
                    } else {
                        storeImage()  // No previous image, just upload new one
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (_binding == null) return
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun storeImage() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val fileReference = storageReference.child("profile_picture/$userId/${UUID.randomUUID()}.jpg")

        Log.d("Profile", "Storing image at path: ${fileReference.path}")

        // Upload the new image to Firebase Storage
        fileReference.putFile(imageUri!!)
            .addOnSuccessListener {
                // Get the download URL of the uploaded image
                fileReference.downloadUrl.addOnSuccessListener { uri ->
                    if (_binding == null) return@addOnSuccessListener
                    val downloadUrl = uri.toString()
                    Log.d("Profile", "Image uploaded successfully: $downloadUrl")

                    // Save the new image URL to Firebase Realtime Database
                    val userReference = FirebaseDatabase.getInstance().getReference("StudentsTbl/$userId")
                    userReference.child("profile_picture").setValue(downloadUrl)
                        .addOnSuccessListener {
                            if (_binding == null) return@addOnSuccessListener
                            binding.progressbar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Profile image updated successfully", Toast.LENGTH_SHORT).show()

                            // Load the new image into the ImageView
                            Picasso.get().load(downloadUrl).into(binding.profilePic)
                        }
                        .addOnFailureListener { error ->
                            if (_binding == null) return@addOnFailureListener
                            binding.progressbar.visibility = View.GONE
                            Toast.makeText(requireContext(), "Failed to update profile image: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { error ->
                if (_binding == null) return@addOnFailureListener
                binding.progressbar.visibility = View.GONE
                Toast.makeText(requireContext(), "Image upload failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayUser() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val usersReference = FirebaseDatabase.getInstance().getReference("StudentsTbl/$uid")

            usersReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return // Check if view is still available
                    if (snapshot.exists()) {
                        val name = snapshot.child("name").getValue(String::class.java)
                        val course = snapshot.child("course").getValue(String::class.java)
                        val section = snapshot.child("section").getValue(String::class.java)
                        val email = snapshot.child("email").getValue(String::class.java)
                        val profileImageUrl = snapshot.child("profile_picture").getValue(String::class.java)

                        binding.name.text = name ?: "N/A"
                        binding.course.text = course ?: "N/A"
                        binding.section.text = section ?: "N/A"
                        binding.email.text = email ?: "N/A"

                        if (!profileImageUrl.isNullOrEmpty()) {
                            Picasso.get().load(profileImageUrl).into(binding.profilePic)
                        } else {
                            binding.profilePic.setImageResource(R.drawable.camera_icon)
                        }
                    } else {
                        // Handle no data case
                        binding.name.text = "N/A"
                        binding.course.text = "N/A"
                        binding.section.text = "N/A"
                        binding.email.text = "N/A"
                        binding.profilePic.setImageResource(R.drawable.camera_icon)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (_binding == null) return
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
