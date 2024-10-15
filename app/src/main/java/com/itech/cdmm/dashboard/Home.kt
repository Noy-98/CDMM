package com.itech.cdmm.dashboard

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itech.cdmm.ProductsAdapter
import com.itech.cdmm.ProductsDBStructure
import com.itech.cdmm.databinding.FragmentHomeBinding

class Home : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var productsAdapter: ProductsAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var productList: MutableList<ProductsDBStructure>
    private lateinit var storageReference: StorageReference
    private lateinit var searchBox: EditText
    private lateinit var noPostText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Initialize product list and adapter
        productList = mutableListOf()
        productsAdapter = ProductsAdapter(requireContext(), productList)
        binding.productList.adapter = productsAdapter
        binding.productList.layoutManager = LinearLayoutManager(requireContext())

        // Initialize Firebase database reference
        storageReference = FirebaseStorage.getInstance().reference
        searchBox = binding.searchBox
        noPostText = binding.noPostText

        loadProductData()  // Load product data

        // Add search functionality
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                filterProducts(charSequence.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        return binding.root
    }

    private fun loadProductData() {
        databaseReference = FirebaseDatabase.getInstance().getReference("ProductsTbl")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productList.clear()
                if (snapshot.exists()) {
                    for (p_idSnapshot in snapshot.children) {
                        val product = p_idSnapshot.getValue(ProductsDBStructure::class.java)
                        if (product != null) {
                            productList.add(product)
                        }
                    }
                    productsAdapter.notifyDataSetChanged()
                }
                noPostText.visibility = if (productList.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load products", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterProducts(query: String) {
        val filteredList = ArrayList<ProductsDBStructure>()
        for (product in productList) {
            val productName = product.product_name?.toString()?.toLowerCase() ?: ""
            val productPrice = product.product_price?.toString()?.toLowerCase() ?: ""

            if (productName.contains(query.toLowerCase()) || productPrice.contains(query.toLowerCase())) {
                filteredList.add(product)
            }
        }
        productsAdapter.filterList(filteredList)
    }
}
