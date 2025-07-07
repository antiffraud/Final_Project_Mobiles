package com.example.projectmobiles.presentation.cart

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.projectmobiles.databinding.FragmentShopBinding
import com.example.projectmobiles.presentation.detail.CartManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.midtrans.sdk.uikit.api.model.*
import com.midtrans.sdk.uikit.external.UiKitApi
import com.midtrans.sdk.uikit.internal.util.UiKitConstants
import java.util.*
import kotlin.math.roundToLong

class shop : Fragment(), CartAdapter.CartItemListener {
    private var _binding: FragmentShopBinding? = null
    private val binding get() = _binding!!
    private lateinit var cartAdapter: CartAdapter
    private val cartManager = CartManager.getInstance()

    private val midtransLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result?.resultCode == Activity.RESULT_OK) {
                val tr = result.data
                    ?.getParcelableExtra<TransactionResult>(UiKitConstants.KEY_TRANSACTION_RESULT)
                if (tr == null) {
                    Toast.makeText(requireContext(), "No transaction data", Toast.LENGTH_LONG).show()
                    return@registerForActivityResult
                }
                when (tr.status) {
                    UiKitConstants.STATUS_SUCCESS -> {
                        Toast.makeText(
                            requireContext(),
                            "Pembayaran Berhasil. ID: ${tr.transactionId}",
                            Toast.LENGTH_LONG
                        ).show()
                        cartManager.clearCart()
                        updateCartUI()
                    }
                    UiKitConstants.STATUS_PENDING -> {
                        Toast.makeText(
                            requireContext(),
                            "Pembayaran Tertunda. ID: ${tr.transactionId}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    UiKitConstants.STATUS_FAILED -> {
                        Toast.makeText(
                            requireContext(),
                            "Pembayaran Gagal. ID: ${tr.transactionId}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    UiKitConstants.STATUS_CANCELED -> {
                        Toast.makeText(
                            requireContext(),
                            "Pembayaran Dibatalkan",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {
                        Toast.makeText(
                            requireContext(),
                            "Status Pembayaran: ${tr.status}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cartManager.initialize(requireContext())
        setupMidtrans()
        setupRecyclerView()
        setupCheckoutButton()
        updateCartUI()
    }

    private fun setupMidtrans() {
        UiKitApi.Builder()
            .withContext(requireContext().applicationContext)
            .withMerchantUrl("https://63fc-103-133-160-153.ngrok-free.app/Midtrans/charge.php/")
            .withMerchantClientKey("SB-Mid-client-s20DG06SUaz6sj9n")
            .enableLog(true)
            .withColorTheme(CustomColorTheme("#FFE51255", "#B61548", "#FFE51255"))
            .build()

        val uIKitCustomSetting = UiKitApi.getDefaultInstance().uiKitSetting
        uIKitCustomSetting.saveCardChecked = true
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(cartManager.getCartItems().toMutableList(), this)
        binding.recyclerViewCart.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }
    }

    private fun setupCheckoutButton() {
        binding.checkoutButton.setOnClickListener {
            Log.d("ButtonDebug", "Checkout button ditekan")
            if (cartManager.getCartItems().isNotEmpty()) {
                startMidtransPayment()
            } else {
                Toast.makeText(requireContext(), "Keranjang Anda kosong", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startMidtransPayment() {
        try {
            Log.d("MidtransDebug", "Memulai proses pembayaran Midtrans")

            // 1) Variabel awal untuk order
            val cartItems = cartManager.getCartItems()
            val subtotal  = cartManager.calculateSubtotal()
            val tax       = subtotal * 0.1   // 10% tax
            val total     = subtotal + tax

            val taxIdr      = tax.roundToLong()
            val totalIdr    = total.roundToLong()

            Log.d("MidtransDebug", "Subtotal=$subtotal, Tax=$tax, Total=$total, Items=${cartItems.size}")

            // 2) Buat detail item Midtrans
            val itemDetailsList = cartItems.map { item ->
                val rawPrice = item.movie.price ?: 0.0
                val priceIdr = rawPrice.roundToLong()
                ItemDetails(
                    id       = item.movie.id.toString(),
                    price    = priceIdr.toDouble(),
                    quantity = item.quantity,
                    name     = item.movie.title
                )
            }
            val taxItem = ItemDetails(
                id       = "TAX",
                price    = taxIdr.toDouble(),
                quantity = 1,
                name     = "Pajak (10%)"
            )
            val allItems = itemDetailsList + taxItem

            // 3) Kirim data customer ke FirebaseAuth
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                Toast.makeText(requireContext(), "Anda harus login dulu", Toast.LENGTH_SHORT).show()
                return
            }
            val firstName = user.displayName ?: "Pelanggan Movie"
            val email     = user.email ?: ""
            val phone     = user.phoneNumber ?: ""

            val customerDetails = CustomerDetails(
                firstName          = firstName,
                customerIdentifier = user.uid,
                email              = email,
                phone              = phone
            )
            Log.d("MidtransDebug", "Customer details: $customerDetails")

            // 4) Buat detail transaksi
            val orderId = "ORDER-" + UUID.randomUUID().toString().take(8)
            val transactionDetails = SnapTransactionDetail(orderId, totalIdr.toDouble())
            Log.d("MidtransDebug", "OrderID=$orderId, Amount=$total")

            val data = mapOf(
                "userId"    to user.uid,
                "movieId"   to cartItems.first().movie.id.toString(),
                "status"    to "pending",
                "createdAt" to FieldValue.serverTimestamp()
            )
            Firebase.firestore
                .collection("payments")
                .document(orderId)
                .set(data)
                .addOnSuccessListener {
                    Log.d("Shop", "Payment record created: $orderId")

                    Firebase.firestore
                        .collection("payments")
                        .document(orderId)
                        .addSnapshotListener { snap, err ->
                            if (err != null || snap == null || !snap.exists()) return@addSnapshotListener
                            val status = snap.getString("status")
                            when (status) {
                                "settlement" -> {
                                    // 1) Hapus item dari keranjang
                                    cartManager.clearCart()
                                    updateCartUI()
                                    Toast.makeText(requireContext(), "Pembayaran sukses!", Toast.LENGTH_SHORT).show()
                                }
                                "cancelled" -> {
                                    Toast.makeText(requireContext(), "Pembayaran $status", Toast.LENGTH_SHORT).show()
                                }
                                // "pending" → biarin aja
                            }
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("Shop", "Failed to write payment", e)
                }

            // (5) Now launch Midtrans UI flow
            val uiKit = UiKitApi.getDefaultInstance() ?: run {
                Toast.makeText(requireContext(), "Midtrans SDK belum inisialisasi", Toast.LENGTH_LONG).show()
                return
            }
            uiKit.startPaymentUiFlow(
                activity           = requireActivity(),
                launcher           = midtransLauncher,
                transactionDetails = transactionDetails,
                customerDetails    = customerDetails,
                itemDetails        = allItems
            )
            Log.d("MidtransDebug", "startPaymentUiFlow dipanggil")
        } catch (e: Exception) {
            Log.e("MidtransDebug", "Error umum: ${e.message}", e)
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateCartUI() {
        val cartItems = cartManager.getCartItems()
        cartAdapter.updateCartItems(cartItems)

        if (cartItems.isEmpty()) {
            binding.recyclerViewCart.visibility = View.GONE
            binding.emptyCartText.visibility = View.VISIBLE
            binding.checkoutButton.isEnabled = false
        } else {
            binding.recyclerViewCart.visibility = View.VISIBLE
            binding.emptyCartText.visibility = View.GONE
            binding.checkoutButton.isEnabled = true
        }

        // Update harga
        val subtotal = cartManager.calculateSubtotal()
        val tax = subtotal * 0.1
        val total = subtotal + tax

        binding.subtotalValue.text = "Rp ${formatPrice(subtotal)}"
        binding.taxValue.text = "Rp ${formatPrice(tax)}"
        binding.totalValue.text = "Rp ${formatPrice(total)}"
    }

    private fun formatPrice(price: Double): String {
        return String.format("%,.0f", price)
    }

    override fun onQuantityChanged(position: Int, newQuantity: Int) {
        cartManager.updateQuantity(position, newQuantity)
        updateCartUI()
    }

    override fun onRemoveItem(position: Int) {
        cartManager.removeItem(position)
        updateCartUI()
    }

    override fun onResume() {
        super.onResume()
        updateCartUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}