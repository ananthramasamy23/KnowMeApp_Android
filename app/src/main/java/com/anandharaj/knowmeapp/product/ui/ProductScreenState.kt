package com.anandharaj.knowmeapp.product.ui

import com.anandharaj.knowmeapp.product.data.Product
import com.anandharaj.knowmeapp.product.data.ProductDetail

data class ProductScreenState(
    val isLoadingList: Boolean = false,
    val isLoadingDetail: Boolean = false,
    val products: List<Product>? = null,
    val selectedProduct: ProductDetail? = null,
    val error: String? = null,
    val isFetchingLocation: Boolean = false,
    val currentAddress: String? = "Address not available.",
    val locationPermissionGranted: Boolean? = null,
    val wishlistItems: List<Product> = emptyList(),
    val lastWishlistActionMessage: String? = null,
    val searchQuery: String = ""

)
