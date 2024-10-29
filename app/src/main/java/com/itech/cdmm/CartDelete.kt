package com.itech.cdmm

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class CartDelete {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("StudentCartTbl")

    // Function to delete a product from Firebase
    fun deleteCart(userId: String, cart_id: String, callback: (Boolean) -> Unit) {
        val productRef = database.child(userId).child(cart_id)
        productRef.removeValue()
            .addOnSuccessListener {
                callback(true) // Success callback
            }
            .addOnFailureListener {
                callback(false) // Failure callback
            }
    }

}