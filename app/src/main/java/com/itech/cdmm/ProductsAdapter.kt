package com.itech.cdmm

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class ProductsAdapter(private val context: Context, private var productList: List<ProductsDBStructure>) :
    RecyclerView.Adapter<ProductsAdapter.ViewHolder>() {

    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val product_image: ImageView = itemView.findViewById(R.id.productImage)
        val product_name: TextView = itemView.findViewById(R.id.productName)
        val product_price: TextView = itemView.findViewById(R.id.productPrice)
        val addtoCartBttn: AppCompatButton = itemView.findViewById(R.id.addtoCartBttn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.products_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val products = productList[position]

        // Load image from URL using Glide
        Glide.with(context)

            .load(products.product_image)
            .placeholder(R.drawable.cart_icon) // Placeholder image while loading
            .error(R.drawable.cart_icon) // Error image if the loading fails
            .into(holder.product_image)

        holder.product_name.text = products.product_name
        holder.product_price.text = products.product_price

        // Handle click on view details button
        holder.addtoCartBttn.setOnClickListener {
            addToCart(products)
        }
    }

    private fun addToCart(products: ProductsDBStructure) {
        // Generate a unique cart ID
        val cartId = databaseReference.child("StudentCartTbl").child(currentUserId).push().key ?: return

        // Create CartDBStructure instance with product data
        val cartItem = CartDBStructure(
            cart_id = cartId,
            product_name = products.product_name ?: "",
            product_price = products.product_price ?: "",
            product_size = products.product_size ?: "",
            product_description = products.product_description ?: "",
            product_quantity = "1",
            product_total_price = products.product_price ?: "",
            product_image = products.product_image
        )

        // Add to Firebase under StudentCartTbl -> currentUserId -> cartId
        databaseReference.child("StudentCartTbl").child(currentUserId).child(cartId).setValue(cartItem)
            .addOnSuccessListener {
                Toast.makeText(context, "${products.product_name} added to cart", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to add to cart", Toast.LENGTH_SHORT).show()
            }
    }

    override fun getItemCount(): Int {
        return productList.size
    }

    fun filterList(filteredList: List<ProductsDBStructure>) {
        productList = filteredList.toMutableList()
        notifyDataSetChanged()
    }
}