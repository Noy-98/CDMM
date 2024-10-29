package com.itech.cdmm

import com.google.firebase.database.FirebaseDatabase

class NotificationDelete {

    // Delete a notification by its unique notification_id
    fun deleteNotification(notificationId: String, callback: (Boolean) -> Unit) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("NotificationTbl")

        // Target only the specific notification by notification_id
        databaseRef.child(notificationId).removeValue()
            .addOnSuccessListener {
                callback(true) // Deletion was successful
            }
            .addOnFailureListener {
                callback(false) // Deletion failed
            }
    }
}
