package com.itech.cdmm

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(private val context: Context, private var notificationList: MutableList<NotificationDBStructure>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title_: TextView = itemView.findViewById(R.id.title)
        val subject_: TextView = itemView.findViewById(R.id.subject)
        val from_: TextView = itemView.findViewById(R.id.from)
        val to_: TextView = itemView.findViewById(R.id.to)
        val message_: TextView = itemView.findViewById(R.id.message)
        val deleteBttn: AppCompatButton = itemView.findViewById(R.id.deleteBttn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.notification_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notificationList[position]

        holder.title_.text = notification.title
        holder.subject_.text = notification.subject
        holder.from_.text = notification.from
        holder.to_.text = notification.to
        holder.message_.text = notification.message

        // Handle click on view details button
        holder.deleteBttn.setOnClickListener {
            deleteNotificationFromFirebase(notification, position)
        }
    }

    private fun deleteNotificationFromFirebase(
        notification: NotificationDBStructure,
        position: Int
    ) {
        NotificationDelete().deleteNotification(notification.notification_id) { isSuccess ->
            if (isSuccess) {
                // Check if position is valid before removing
                if (position >= 0 && position < notificationList.size) {
                    notificationList.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, notificationList.size)
                } else {
                    Log.e("NotificationAdapter", "Invalid position: $position, size: ${notificationList.size}")
                }
            } else {
                // Handle failure, e.g., show a message
                Log.e("NotificationAdapter", "Failed to delete notification: ${notification.notification_id}")
            }
        }
    }

    override fun getItemCount(): Int {
        return notificationList.size
    }
}