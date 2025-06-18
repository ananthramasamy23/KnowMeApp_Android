package com.anandharaj.knowmeapp.product.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anandharaj.knowmeapp.R
import com.anandharaj.knowmeapp.product.data.Product
import com.anandharaj.knowmeapp.product.data.ProductDetail
import com.anandharaj.knowmeapp.product.di.ResourceProvider
import com.anandharaj.knowmeapp.product.repository.ProductRepository
import com.anandharaj.knowmeapp.product.ui.ProductScreenEvent
import com.anandharaj.knowmeapp.product.ui.ProductScreenState
import com.anandharaj.knowmeapp.product.utils.LocationUtils
import com.anandharaj.knowmeapp.product.utils.NetworkConnectivityChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.IOException
import retrofit2.HttpException
import javax.inject.Inject


@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val networkConnectivityChecker: NetworkConnectivityChecker,
    application: Application,
    @ApplicationContext private val context: Context,
    private val resourceProvider: ResourceProvider,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ProductScreenState())
    val uiState: StateFlow<ProductScreenState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.collect { state ->
                state.wishlistItems.forEach { _ ->
                }
            }
        }
    }

    fun onEvent(event: ProductScreenEvent) {
        when (event) {
            is ProductScreenEvent.LoadProductList -> loadProducts()
            is ProductScreenEvent.LoadProductDetail -> loadProductDetail(event.productId)
            is ProductScreenEvent.ClearError -> _uiState.update { it.copy(error = null) }
            is ProductScreenEvent.RequestLocationAndShare -> {
                _uiState.update { it.copy(isFetchingLocation = true) }
            }

            is ProductScreenEvent.LocationPermissionsResult -> {
                _uiState.update { it.copy(locationPermissionGranted = event.granted) }
                if (!event.granted) {
                    _uiState.update {
                        it.copy(
                            isFetchingLocation = false,
                            currentAddress = resourceProvider.getString(R.string.location_permission_denied)
                        )
                    }
                }
            }

            is ProductScreenEvent.LocationFetched -> {
                _uiState.update {
                    it.copy(
                        isFetchingLocation = false,
                        currentAddress = event.address
                            ?: resourceProvider.getString(R.string.could_not_determine_address),
                    )
                }
            }

            is ProductScreenEvent.ShareProductWithLocation -> {
                shareProductDetails(
                    context,
                    _uiState.value.selectedProduct,
                    _uiState.value.currentAddress
                )
            }
            is ProductScreenEvent.AddToWishlist -> toggleWishlist(event.product)
            ProductScreenEvent.ClearLastWishlistActionMessage -> {
                _uiState.update { it.copy(lastWishlistActionMessage = null) }
            }
            is ProductScreenEvent.RemoveFromWishlist -> toggleWishlist(event.product)


        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            if (!networkConnectivityChecker.isNetworkAvailable()) {
                _uiState.update {
                    it.copy(
                        error = resourceProvider.getString(R.string.error_no_internet_connection),
                        isLoadingList = false
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(isLoadingList = true, error = null) }
            try {
                val products = repository.fetchProducts()
                _uiState.update {
                    it.copy(products = products, isLoadingList = false)
                }
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(
                        error = resourceProvider.getString(
                            R.string.error_network_prefix,
                            e.localizedMessage
                        ),
                        isLoadingList = false
                    )
                }
            } catch (e: HttpException) {
                _uiState.update {
                    it.copy(
                        error = resourceProvider.getString(R.string.error_server_prefix, e.code()),
                        isLoadingList = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = resourceProvider.getString(
                            R.string.error_unexpected_prefix,
                            e.localizedMessage
                        ),
                        isLoadingList = false
                    )
                }
            }
        }
    }

    private fun loadProductDetail(productId: Int) {
        viewModelScope.launch {
            if (!networkConnectivityChecker.isNetworkAvailable()) {
                _uiState.update {
                    it.copy(
                        error = resourceProvider.getString(R.string.error_no_internet_connection),
                        isLoadingDetail = false
                    )
                }
                return@launch
            }
            _uiState.update {
                it.copy(
                    isLoadingDetail = true,
                    error = null,
                    selectedProduct = null
                )
            }
            try {
                val product = repository.fetchProductDetail(productId)
                _uiState.update {
                    it.copy(selectedProduct = product, isLoadingDetail = false)
                }
            } catch (e: IOException) {
                _uiState.update {
                    it.copy(
                        error = resourceProvider.getString(
                            R.string.error_network_prefix,
                            "fetching detail: ${e.localizedMessage}"
                        ),
                        isLoadingDetail = false
                    )
                }
            } catch (e: HttpException) {
                _uiState.update {
                    it.copy(
                        error = resourceProvider.getString(R.string.error_server_prefix, e.code()),
                        isLoadingDetail = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = resourceProvider.getString(
                            R.string.error_unexpected_prefix,
                            "fetching detail: ${e.localizedMessage}"
                        ),
                        isLoadingDetail = false
                    )
                }
            }
        }
    }

    fun fetchLocationAndAddress(
        context: Context,
        product: ProductDetail?,
        viewModel: ProductViewModel
    ) {
        LocationUtils.getCurrentLocation(context) { location ->
            if (location != null) {
                LocationUtils.reverseGeocodeLocation(
                    context,
                    location.latitude,
                    location.longitude
                ) { address ->
                    viewModel.onEvent(ProductScreenEvent.LocationFetched(location, address))
                    product?.let { prod ->
                        shareProductDetails(context, prod, address)
                    }
                }
            } else {
                viewModel.onEvent(
                    ProductScreenEvent.LocationFetched(
                        null,
                        resourceProvider.getString(R.string.could_not_get_current_location)
                    )
                )
                product?.let { prod ->
                    shareProductDetails(
                        context,
                        prod,
                        resourceProvider.getString(R.string.location_unknown)
                    )
                }
            }
        }
    }

    private fun toggleWishlist(product: Product) {
        viewModelScope.launch {
            _uiState.update { currentState ->
                val currentWishlist = currentState.wishlistItems.toMutableList()
                val isCurrentlyInWishlist = currentWishlist.any { it.id == product.id }
                val message: String

                if (isCurrentlyInWishlist) {
                    currentWishlist.removeIf { it.id == product.id }
                    message = resourceProvider.getString(R.string.removed_from_wishlist_message, product.title)
                } else {
                    currentWishlist.add(product)
                    message = resourceProvider.getString(R.string.added_to_wishlist_message, product.title)
                }

                currentState.copy(
                    wishlistItems = currentWishlist,
                    lastWishlistActionMessage = message
                )
            }
        }
    }

    fun shareProductDetails(context: Context, product: ProductDetail?, address: String?) {
        val shareText = resourceProvider.getString(
            R.string.share_product_text,
            product?.title ?: "",
            product?.price ?: "",
            address ?: resourceProvider.getString(R.string.location_unknown)
        )
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(
            sendIntent,
            resourceProvider.getString(R.string.share_product_title, product?.title ?: "")
        )
        context.startActivity(shareIntent)
    }
}