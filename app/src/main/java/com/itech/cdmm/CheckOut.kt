package com.itech.cdmm


import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.itech.cdmm.databinding.ActivityCheckOutBinding
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.paypal.android.corepayments.CoreConfig
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutListener
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutResult
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.corepayments.Environment


class CheckOut : AppCompatActivity() {

    private lateinit var binding: ActivityCheckOutBinding
    private val database = FirebaseDatabase.getInstance()
    private var userId: String? = null
    private var mainTotal: String = "0.00" // Default value

    private val clientID = "AW3O1qtRwBAy8C-Q6gAlM8CSLj8TJpUqXGtlNLqNvLYcsexdLFW0VliBS4Q8hBayHZj2SwAjVQwJQ0I2"
    private val secretID = "EP785DEpWeEXgUZZsPChtSTyTp1Q-tNhg2hitJHCcdkfmlx_pEvMk-IQO2mnurOmHAXzFc-NvQuM3ufj"
    private val returnUrl = "com.itech.cdmm://cdmm"
    var accessToken = ""
    private lateinit var uniqueId: String
    private var orderid = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userId = intent.getStringExtra("USER_ID") ?: ""

        AndroidNetworking.initialize(applicationContext)

        binding = ActivityCheckOutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.checkout.visibility = View.GONE

        fetchAccessToken()
        fetchMainTotal()

        binding.checkout.setOnClickListener {
            startOrder()
        }
    }

    private fun fetchMainTotal() {
        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "User ID not provided", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = database.getReference("StudentCheckoutTbl").child(userId!!)
        userRef.child("main_total").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mainTotal = snapshot.getValue(String::class.java) ?: "0.00"
                Log.d(TAG, "Fetched main_total: $mainTotal")
                Toast.makeText(this@CheckOut, "Total: $mainTotal", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")
                Toast.makeText(this@CheckOut, "Error fetching total", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handlerOrderID(orderID: String) {
        val config = CoreConfig(clientID, environment = Environment.SANDBOX)
        val payPalWebCheckoutClient = PayPalWebCheckoutClient(this@CheckOut, config, returnUrl)
        payPalWebCheckoutClient.listener = object : PayPalWebCheckoutListener {
            override fun onPayPalWebSuccess(result: PayPalWebCheckoutResult) {
                Log.d(TAG, "onPayPalWebSuccess: $result")
            }

            override fun onPayPalWebFailure(error: PayPalSDKError) {
                Log.d(TAG, "onPayPalWebFailure: $error")
            }

            override fun onPayPalWebCanceled() {
                Log.d(TAG, "onPayPalWebCanceled: ")
            }
        }

        orderid = orderID
        val payPalWebCheckoutRequest =
            PayPalWebCheckoutRequest(orderID, fundingSource = PayPalWebCheckoutFundingSource.PAYPAL)
        payPalWebCheckoutClient.start(payPalWebCheckoutRequest)

    }

    private fun startOrder() {
        uniqueId = UUID.randomUUID().toString()

        val conversionRatePHPtoUSD = 0.0172916667 // Conversion rate: 1 PHP to USD
        val amountInPHP = mainTotal.toDoubleOrNull() ?: 0.0
        val amountInUSD = amountInPHP * conversionRatePHPtoUSD

        if (amountInUSD <= 0.0) {
            Toast.makeText(this, "Invalid amount to start order", Toast.LENGTH_SHORT).show()
            return
        }


        val orderRequestJson = JSONObject().apply {
            put("intent", "CAPTURE")
            put("purchase_units", JSONArray().apply {
                put(JSONObject().apply {
                    put("reference_id", uniqueId)
                    put("amount", JSONObject().apply {
                        put("currency_code", "USD")
                            put("value", String.format("%.2f", amountInUSD))
                    })
                })
            })
            put("payment_source", JSONObject().apply {
                put("paypal", JSONObject().apply {
                    put("experience_context", JSONObject().apply {
                        put("payment_method_preference", "IMMEDIATE_PAYMENT_REQUIRED")
                        put("brand_name", "SH Developer")
                        put("locale", "en-US")
                        put("landing_page", "LOGIN")
                        put("shipping_preference", "NO_SHIPPING")
                        put("user_action", "PAY_NOW")
                        put("return_url", returnUrl)
                        put("cancel_url", "https://example.com/cancelUrl")
                    })
                })
            })
        }

        AndroidNetworking.post("https://api-m.sandbox.paypal.com/v2/checkout/orders")
            .addHeaders("Authorization", "Bearer $accessToken")
            .addHeaders("Content-Type", "application/json")
            .addHeaders("PayPal-Request-Id", uniqueId)
            .addJSONObjectBody(orderRequestJson)
            .setPriority(Priority.HIGH)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    Log.d(TAG, "Order Response : " + response.toString())
                    handlerOrderID(response.getString("id"))
                }

                override fun onError(error: ANError) {
                    Log.d(
                        TAG,
                        "Order Error : ${error.message} || ${error.errorBody} || ${error.response}"
                    )
                }
            })
    }

    private fun fetchAccessToken() {
        val authString = "$clientID:$secretID"
        val encodedAuthString = Base64.encodeToString(authString.toByteArray(), Base64.NO_WRAP)

        AndroidNetworking.post("https://api-m.sandbox.paypal.com/v1/oauth2/token")
            .addHeaders("Authorization", "Basic $encodedAuthString")
            .addHeaders("Content-Type", "application/x-www-form-urlencoded")
            .addBodyParameter("grant_type", "client_credentials")
            .setPriority(Priority.HIGH)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    accessToken = response.getString("access_token")
                    Log.d(TAG, accessToken)
                    Toast.makeText(this@CheckOut, "Access Token Fetched!", Toast.LENGTH_SHORT).show()

                    binding.checkout.visibility = View.VISIBLE
                }

                override fun onError(error: ANError) {
                    Log.d(TAG, error.errorBody)
                    Toast.makeText(this@CheckOut, "Error Occurred!", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onNewIntent(intent: Intent) {
        if (intent != null) {
            super.onNewIntent(intent)
        }
        Log.d(TAG, "onNewIntent: $intent")
        if (intent?.data!!.getQueryParameter("opType") == "payment") {
            captureOrder(orderid)
        } else if (intent?.data!!.getQueryParameter("opType") == "cancel") {
            Toast.makeText(this, "Payment Cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun captureOrder(orderID: String) {
        AndroidNetworking.post("https://api-m.sandbox.paypal.com/v2/checkout/orders/$orderID/capture")
            .addHeaders("Authorization", "Bearer $accessToken")
            .addHeaders("Content-Type", "application/json")
            .addJSONObjectBody(JSONObject()) // Empty body
            .setPriority(Priority.HIGH)
            .build()
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {
                    Log.d(TAG, "Capture Response : " + response.toString())
                    Toast.makeText(this@CheckOut, "Payment Successful", Toast.LENGTH_SHORT).show()
                }

                override fun onError(error: ANError) {
                    // Handle the error
                    Log.e(TAG, "Capture Error : " + error.errorDetail)
                }
            })
    }

    companion object {
        const val TAG = "MyTag"
    }
}