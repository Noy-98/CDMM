package com.itech.cdmm.dashboard

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.itech.cdmm.CartDBStructure
import com.itech.cdmm.CartsAdapter
import com.itech.cdmm.CheckOut
import com.itech.cdmm.databinding.FragmentCartBinding
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID


class Cart : Fragment() {
    private lateinit var binding: FragmentCartBinding
    private lateinit var cartsAdapter: CartsAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var cartList: MutableList<CartDBStructure>
    private lateinit var storageReference: StorageReference
    private lateinit var noPostText: TextView
    private lateinit var mainTotalTextView: TextView

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCartBinding.inflate(inflater, container, false)

        cartList = mutableListOf()
        cartsAdapter = CartsAdapter(requireContext(), cartList)
        binding.cartList.adapter = cartsAdapter
        binding.cartList.layoutManager = LinearLayoutManager(requireContext())

        storageReference = FirebaseStorage.getInstance().reference
        noPostText = binding.noPostText
        mainTotalTextView = binding.mainTotal

        loadCartsData()

        binding.checkout.setOnClickListener {
            auth.currentUser?.uid?.let { userId ->
                generateBarcode(userId)
            }
        }

        return binding.root
    }

    private fun generateBarcode(userId: String) {
        val multiFormatWriter = MultiFormatWriter()
        try {
            val bitMatrix: BitMatrix = multiFormatWriter.encode(userId, BarcodeFormat.CODE_128, 400, 150)
            val barcodeBitmap = Bitmap.createBitmap(400, 150, Bitmap.Config.RGB_565)
            for (x in 0 until 400) {
                for (y in 0 until 150) {
                    barcodeBitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            // Show the barcode dialog
            showBarcodeDialog(barcodeBitmap, userId)
            // Save barcode image to Firebase storage
            saveBarcodeToStorage(barcodeBitmap, userId)

        } catch (e: WriterException) {
            e.printStackTrace()
        }
    }

    private fun saveBarcodeToStorage(barcodeBitmap: Bitmap, userId: String) {
        val storagePath = "barcodes/$userId.png"
        val barcodeRef = storageReference.child(storagePath)

        val baos = ByteArrayOutputStream()
        barcodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val data = baos.toByteArray()

        barcodeRef.putBytes(data).addOnSuccessListener {
            barcodeRef.downloadUrl.addOnSuccessListener { uri ->
                // Save checkout information to Firebase Realtime Database
                saveCheckoutInfoToDatabase(uri.toString(), userId)
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to upload barcode", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCheckoutInfoToDatabase(barcodeUrl: String, userId: String) {
        val checkoutReference = FirebaseDatabase.getInstance().getReference("StudentCheckoutTbl").child(userId)
        val studentReference = FirebaseDatabase.getInstance().getReference("StudentsTbl").child(userId)
        val cartReference = FirebaseDatabase.getInstance().getReference("StudentCartTbl").child(userId)

        // Get student info from StudentTbl and save it under the userId in StudentCheckoutTbl
        studentReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(studentSnapshot: DataSnapshot) {
                val studentData = mapOf(
                    "name" to (studentSnapshot.child("name").getValue(String::class.java) ?: "Unknown"),
                    "course" to (studentSnapshot.child("course").getValue(String::class.java) ?: "Unknown"),
                    "section" to (studentSnapshot.child("section").getValue(String::class.java) ?: "Unknown")
                )

                // Save the student details and main total directly under the userId
                checkoutReference.updateChildren(studentData).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        checkoutReference.child("main_total").setValue(mainTotalTextView.text.toString())

                        // After saving student data, save each cart item under a unique cartId
                        cartReference.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(cartSnapshot: DataSnapshot) {
                                for (cartItemSnapshot in cartSnapshot.children) {
                                    val cartId = cartItemSnapshot.key ?: UUID.randomUUID().toString()
                                    val cartData = cartItemSnapshot.getValue(CartDBStructure::class.java)

                                    if (cartData != null) {
                                        val checkoutItemData = mapOf(
                                            "product_name" to cartData.product_name,
                                            "product_size" to cartData.product_size,
                                            "product_quantity" to cartData.product_quantity,
                                            "sub_total" to cartData.product_total_price,
                                            "product_image" to cartData.product_image,
                                            "barcode_image" to barcodeUrl
                                        )
                                        // Save each cart item under its own cartId within userId
                                        checkoutReference.child(cartId).setValue(checkoutItemData)
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("Firebase", "Failed to save cart data: ${error.message}")
                            }
                        })
                    } else {
                        Log.e("Firebase", "Failed to save student data: ${task.exception?.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Failed to fetch student data: ${error.message}")
            }
        })
    }

    private fun showBarcodeDialog(barcodeBitmap: Bitmap, userId: String) {
        val alertDialog = AlertDialog.Builder(requireContext())
        val dialogLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        // ImageView for displaying the barcode
        val barcodeImageView = ImageView(requireContext()).apply {
            setImageBitmap(barcodeBitmap)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            ).apply { setMargins(0, 0, 0, 20) }
        }

        // TextView to display the user ID or other output data
        val outputTextView = TextView(requireContext()).apply {
            text = "User ID: $userId"
            textSize = 18f
            setPadding(0, 0, 0, 20)
        }

        // Add barcode ImageView and output TextView to dialog layout
        dialogLayout.addView(barcodeImageView)
        dialogLayout.addView(outputTextView)

        // Set up dialog with buttons to download and close
        alertDialog.setView(dialogLayout)
            .setPositiveButton("Checkout") { _, _ ->
                downloadBarcodeImage(barcodeBitmap, userId)
                // Navigate to CheckOut.kt
                val intent = Intent(requireContext(), CheckOut::class.java).apply {
                    putExtra("USER_ID", userId) // Pass user ID if needed in CheckOut activity
                }
                startActivity(intent)
            }
            .setNegativeButton("Close") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun downloadBarcodeImage(barcodeBitmap: Bitmap, userId: String) {
        val filename = "Barcode_$userId.png"
        var outputStream: OutputStream? = null

        try {
            // Check if the device has Android Q or later
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Save the file to MediaStore's Downloads
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = requireContext().contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    outputStream = requireContext().contentResolver.openOutputStream(uri)
                }
            } else {
                // For older versions, save to the Downloads directory
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val imageFile = File(downloadsDir, filename)
                outputStream = FileOutputStream(imageFile)
            }

            // Compress and save the barcode image
            if (outputStream != null) {
                barcodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            outputStream?.flush()

            Toast.makeText(requireContext(), "Barcode saved to Downloads", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to save barcode", Toast.LENGTH_SHORT).show()
        } finally {
            outputStream?.close()
        }
    }

    private fun loadCartsData() {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference("StudentCartTbl").child(userId)
            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    cartList.clear()
                    var totalSum = 0.0  // Variable to store the total sum

                    if (snapshot.exists()) {
                        for (c_idSnapshot in snapshot.children) {
                            val cart = c_idSnapshot.getValue(CartDBStructure::class.java)
                            if (cart != null) {
                                cartList.add(cart)
                                totalSum += cart.product_total_price?.toDouble() ?: 0.0  // Convert to Double and add to totalSum
                            }
                        }
                        cartsAdapter.notifyDataSetChanged()
                    }
                    noPostText.visibility = if (cartList.isEmpty()) View.VISIBLE else View.GONE
                    mainTotalTextView.text = String.format("%.2f", totalSum)  // Update main_total TextView with total
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load carts", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}