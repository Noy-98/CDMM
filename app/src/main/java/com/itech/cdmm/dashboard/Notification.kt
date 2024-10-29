package com.itech.cdmm.dashboard

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.itech.cdmm.NotificationAdapter
import com.itech.cdmm.NotificationDBStructure
import com.itech.cdmm.R
import com.itech.cdmm.databinding.FragmentNotificationBinding

class Notification : Fragment() {

    private lateinit var binding: FragmentNotificationBinding
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var notificationList: MutableList<NotificationDBStructure>
    private lateinit var noPostText: TextView

    companion object {
        const val CHANNEL_ID = "your_channel_id" // Define your channel ID
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentNotificationBinding.inflate(inflater, container, false)

        // Initialize product list and adapter
        notificationList = mutableListOf()
        notificationAdapter = NotificationAdapter(requireContext(), notificationList)
        binding.notificationList.adapter = notificationAdapter
        binding.notificationList.layoutManager = LinearLayoutManager(requireContext())

        noPostText = binding.noPostText

        // Check for notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }

        createNotificationChannel()
        loadNotificationData()

        return binding.root
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notifications",
                NotificationManager.IMPORTANCE_HIGH // Use high importance
            )
            channel.description = "Channel for app notifications"
            val notificationManager = requireContext().getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun loadNotificationData() {
        databaseReference = FirebaseDatabase.getInstance().getReference("NotificationTbl")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notificationList.clear()
                if (snapshot.exists()) {
                    for (n_idSnapshot in snapshot.children) {
                        val notification = n_idSnapshot.getValue(NotificationDBStructure::class.java)
                        if (notification != null) {
                            notificationList.add(notification)
                            // Show notification when a new notification is loaded
                            showLocalNotification("New Notification From Admin", "You have a new notification!")
                        }
                    }
                    notificationAdapter.notifyDataSetChanged()
                }
                noPostText.visibility = if (notificationList.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load notification", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showLocalNotification(title: String, message: String) {
        Log.d("NotificationDebug", "Showing notification: $title - $message")

        val notificationId = 1 // Unique ID for the notification
        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            Intent(), // Empty Intent
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_notifications_24) // Your app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // This will do nothing when tapped
            .setAutoCancel(true)

        val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}