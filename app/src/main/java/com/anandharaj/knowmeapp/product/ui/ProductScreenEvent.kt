package com.anandharaj.knowmeapp.product.ui

import android.location.Location
import com.anandharaj.knowmeapp.product.data.Product

sealed interface ProductScreenEvent {
    object LoadProductList : ProductScreenEvent
    data class LoadProductDetail(val productId: Int) :
        ProductScreenEvent

    object ClearError : ProductScreenEvent
    object RequestLocationAndShare :
        ProductScreenEvent

    data class LocationPermissionsResult(val granted: Boolean) :
        ProductScreenEvent

    data class LocationFetched(val location: Location?, val address: String?) :
        ProductScreenEvent

    object ShareProductWithLocation :
        ProductScreenEvent
    data class AddToWishlist(val product: Product) : ProductScreenEvent
    object ClearLastWishlistActionMessage : ProductScreenEvent
    data class RemoveFromWishlist(val product: Product) : ProductScreenEvent
    data class SearchProducts(val query: String) : ProductScreenEvent

}