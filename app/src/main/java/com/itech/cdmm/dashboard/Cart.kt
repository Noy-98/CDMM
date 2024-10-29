package com.itech.cdmm.dashboard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.itech.cdmm.CartDBStructure
import com.itech.cdmm.CartDelete
import com.itech.cdmm.CartsAdapter
import com.itech.cdmm.ProductsDBStructure
import com.itech.cdmm.databinding.FragmentCartBinding

class Cart : Fragment() {
    private lateinit var binding: FragmentCartBinding
    private lateinit var cartsAdapter: CartsAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var cartList: MutableList<CartDBStructure>
    private lateinit var storageReference: StorageReference
    private lateinit var noPostText: TextView

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

        loadCartsData()

        return binding.root
    }

    private fun loadCartsData() {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            databaseReference = FirebaseDatabase.getInstance().getReference("StudentCartTbl").child(userId)
            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    cartList.clear()
                    if (snapshot.exists()) {
                        for (c_idSnapshot in snapshot.children) {
                            val cart = c_idSnapshot.getValue(CartDBStructure::class.java)
                            if (cart != null) {
                                cartList.add(cart)
                            }
                        }
                        cartsAdapter.notifyDataSetChanged()
                    }
                    noPostText.visibility = if (cartList.isEmpty()) View.VISIBLE else View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load carts", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}