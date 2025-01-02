package com.itech.cdmm

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class CartsAdapter(private val context: Context, private var cartList: MutableList<CartDBStructure>) :
    RecyclerView.Adapter<CartsAdapter.ViewHolder>() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("StudentCartTbl")
    private val sizes = listOf("small", "medium", "large", "extra large", "2 extra large")

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val product_image: ImageView = itemView.findViewById(R.id.productImage)
        val product_name: TextView = itemView.findViewById(R.id.productName)
        val decrease: ImageView = itemView.findViewById(R.id.decrease)
        val productQuantity: TextView = itemView.findViewById(R.id.productQuantity)
        val increase: ImageView = itemView.findViewById(R.id.increase)
        val product_price: TextView = itemView.findViewById(R.id.productPrice)
        val decrease2: ImageView = itemView.findViewById(R.id.decrease2)
        val productSize: TextView = itemView.findViewById(R.id.productSize)
        val increase2: ImageView = itemView.findViewById(R.id.increase2)
        val product_stock: TextView = itemView.findViewById(R.id.productStock)
        val deleteBttn: AppCompatButton = itemView.findViewById(R.id.deleteBttn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.carts_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cart = cartList[position]

        // Load image from URL using Glide
        Glide.with(context)

            .load(cart.product_image)
            .placeholder(R.drawable.cart_icon) // Placeholder image while loading
            .error(R.drawable.cart_icon) // Error image if the loading fails
            .into(holder.product_image)

        holder.product_name.text = cart.product_name
        holder.productQuantity.text = cart.product_quantity
        holder.product_price.text = cart.product_total_price
        holder.productSize.text = cart.product_size
        holder.product_stock.text = cart.product_stock

        holder.decrease2.setOnClickListener {
            updateProductSize(cart, holder, decrease = true)
        }

        holder.increase2.setOnClickListener {
            updateProductSize(cart, holder, decrease = false)
        }

        holder.decrease.setOnClickListener {
            val currentQuantity = cart.product_quantity.toInt()
            if (currentQuantity > 1) {
                val newQuantity = currentQuantity - 1
                updateProductQuantityAndPrice(cart, newQuantity)
            }
        }

        holder.increase.setOnClickListener {
            val newQuantity = cart.product_quantity.toInt() + 1
            updateProductQuantityAndPrice(cart, newQuantity)
        }

        holder.deleteBttn.setOnClickListener {
            deleteProductFromFirebase(cart, position)
        }
    }

    private fun updateProductSize(cart: CartDBStructure, holder: ViewHolder, decrease: Boolean) {
        val currentSize = cart.product_size
        val currentIndex = sizes.indexOf(currentSize)
        val newIndex = if (decrease) {
            (currentIndex - 1 + sizes.size) % sizes.size
        } else {
            (currentIndex + 1) % sizes.size
        }
        val newSize = sizes[newIndex]

        // Update the product size in Firebase and locally
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val productRef = database.child(userId).child(cart.cart_id)

            // Calculate the new total price based on the new size and current quantity
            val pricePerUnit = cart.product_price.toDouble()
            val extraCharge = if (newSize == "extra large" || newSize == "2 extra large") 50 else 0
            val newTotalPrice = (pricePerUnit * cart.product_quantity.toInt()) + extraCharge

            val updates = mapOf(
                "product_size" to newSize,
                "product_total_price" to newTotalPrice.toString()
            )

            productRef.updateChildren(updates).addOnSuccessListener {
                // Update the product locally in the list
                cartList.find { it.cart_id == cart.cart_id }?.let {
                    it.product_size = newSize
                    it.product_total_price = newTotalPrice.toString()
                }
                holder.productSize.text = newSize // Update UI with new size
                holder.product_price.text = newTotalPrice.toString() // Update UI with new price
                notifyDataSetChanged() // Notify adapter of the update
            }.addOnFailureListener {
                // Optionally handle failure
            }
        }
    }

    private fun deleteProductFromFirebase(cart: CartDBStructure, position: Int) {
        val userId = auth.currentUser?.uid
        if (userId != null && position >= 0 && position < cartList.size) {
            CartDelete().deleteCart(userId, cart.cart_id) { isSuccess ->
                if (isSuccess) {
                    // Remove the product safely from the list
                    if (position < cartList.size) {
                        cartList.removeAt(position)
                        notifyItemRemoved(position)
                        // Only update the range if there are still items left
                        if (cartList.isNotEmpty()) {
                            notifyItemRangeChanged(position, cartList.size)
                        }
                    }
                } else {
                    // Handle deletion failure (optional)
                }
            }
        }
    }

    private fun updateProductQuantityAndPrice(cart: CartDBStructure, newQuantity: Int) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val productRef = database.child(userId).child(cart.cart_id)

            // Base price calculation with an extra charge if size is "extra large" or "2 extra large"
            val pricePerUnit = cart.product_price.toDouble()
            val extraCharge = if (cart.product_size == "extra large" || cart.product_size == "2 extra large") 50 else 0
            val newTotalPrice = (pricePerUnit * newQuantity) + extraCharge

            val updates = mapOf(
                "product_quantity" to newQuantity.toString(),
                "product_total_price" to newTotalPrice.toString()
            )

            productRef.updateChildren(updates)
                .addOnSuccessListener {
                    // Update the product locally in the list
                    cartList.find { it.cart_id == cart.cart_id }?.let {
                        it.product_quantity = newQuantity.toString()
                        it.product_total_price = newTotalPrice.toString() // Update total price
                    }
                    notifyDataSetChanged() // Refresh the adapter to show updates
                }
                .addOnFailureListener {
                    // Optionally handle failure
                }
        }
    }

    override fun getItemCount(): Int {
        return cartList.size
    }
}