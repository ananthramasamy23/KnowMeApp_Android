@file:OptIn(ExperimentalPermissionsApi::class)

package com.anandharaj.knowmeapp.product.ui

import android.Manifest
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.anandharaj.knowmeapp.product.data.ProductDetail
import com.anandharaj.knowmeapp.product.viewmodels.ProductViewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.anandharaj.knowmeapp.product.utils.LocationUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.anandharaj.knowmeapp.R
import com.anandharaj.knowmeapp.product.data.Product
import com.anandharaj.knowmeapp.product.utils.ProductListScreenTestTags
import com.anandharaj.knowmeapp.ui.theme.KnowMeAppTheme
import com.google.accompanist.permissions.MultiplePermissionsState


@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalGlideComposeApi::class,
)
@Composable
fun ProductDetailScreen(
    navController: NavController, productId: Int, viewModel: ProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        LocationUtils.initializeLocationClient(context)
        onDispose {}
    }

    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        )
    ) { permissionsResult ->
        val allGranted = permissionsResult.values.all { it }
        viewModel.onEvent(ProductScreenEvent.LocationPermissionsResult(granted = allGranted))

        if (allGranted && uiState.isFetchingLocation) {
            viewModel.fetchLocationAndAddress(context, uiState.selectedProduct, viewModel)
        }
    }

    LaunchedEffect(productId) {
        viewModel.onEvent(ProductScreenEvent.LoadProductDetail(productId))
    }
    KnowMeAppTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    modifier = Modifier.fillMaxWidth(),
                    title = {
                        Text(
                            text = stringResource(id = R.string.product_detail_title),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(ProductListScreenTestTags.TOP_APP_BAR_TITLE),
                        )
                    },
                    actions = {
                        Spacer(modifier = Modifier.width(48.dp))
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.back_button_description)
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = Color.White)
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                when {
                    uiState.isLoadingDetail && uiState.selectedProduct == null -> {
                        LoadingStateView()
                    }

                    uiState.error != null && uiState.selectedProduct == null -> {
                        ErrorStateView(modifier = Modifier, uiState.error, onRetry = {
                            viewModel.onEvent(ProductScreenEvent.LoadProductDetail(productId))
                        })
                    }

                    uiState.selectedProduct != null -> {
                        uiState.selectedProduct?.let { productDetails ->
                            ProductDetailContentWrapper(
                                productDetails = productDetails,
                                uiState = uiState,
                                viewModel = viewModel,
                                context = context,
                                locationPermissionsState = locationPermissionsState
                            )
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.shopping_backgroud),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

}

@Composable
fun ProductDetailContentWrapper(
    productDetails: ProductDetail,
    uiState: ProductScreenState,
    viewModel: ProductViewModel,
    context: Context,
    locationPermissionsState: MultiplePermissionsState
) {
    ProductDetailsContent(
        product = productDetails,
        uiState = uiState,
        onShareClick = {
            viewModel.onEvent(ProductScreenEvent.RequestLocationAndShare)
            if (locationPermissionsState.allPermissionsGranted) {
                viewModel.fetchLocationAndAddress(
                    context,
                    productDetails,
                    viewModel
                )
            } else {
                locationPermissionsState.launchMultiplePermissionRequest()
            }
        },
        onAddToWishlistClick = { productToAddToWishlist ->
            val productForWishlist = Product(
                id = productToAddToWishlist.id,
                title = productToAddToWishlist.title,
                summary = productToAddToWishlist.description,
                imageUrl = productToAddToWishlist.imageUrl
            )
            viewModel.onEvent(ProductScreenEvent.AddToWishlist(productForWishlist))
        },
        isProductInWishlist = uiState.wishlistItems.any { it.id == productDetails.id }
    )
}

@Composable
fun LoadingStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ColumnScope.WishListItem(
    uiState: ProductScreenState,
    productDetail: ProductDetail,
    isProductInWishlist: Boolean,
    onAddToWishlistClick: (ProductDetail) -> Unit,
    onShareClick: () -> Unit,
) {
    val isEnabled = uiState.selectedProduct != null && !uiState.isFetchingLocation

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onAddToWishlistClick(productDetail) },
            enabled = isEnabled
        ) {
            Icon(
                imageVector = if (isProductInWishlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isProductInWishlist) stringResource(id = R.string.remove_from_wishlist_description) else stringResource(
                    id = R.string.add_to_wishlist_description
                ), // Use string resources
                tint = if (isProductInWishlist) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        IconButton(
            onClick = onShareClick,
            enabled = isEnabled
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = stringResource(id = R.string.share_product_description),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ProductDetailsContent(
    product: ProductDetail,
    uiState: ProductScreenState,
    onAddToWishlistClick: (ProductDetail) -> Unit,
    isProductInWishlist: Boolean,
    onShareClick: () -> Unit,
) {

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column {
                GlideImage(
                    model = product.imageUrl,
                    contentDescription =
                        stringResource(R.string.product_image_description, product.title),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(bottomStart = 16.dp)
                        )
                        .padding(8.dp)
                        .align(Alignment.End)
                ) {
                    Text(
                        text = product.price,
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(product.title, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        product.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    when {
                        uiState.isFetchingLocation -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(id = R.string.getting_location_message),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        uiState.error != null && !uiState.isLoadingDetail &&
                                (uiState.error != stringResource(R.string.error_no_internet_connection)
                                        || uiState.currentAddress
                                        == stringResource(R.string.address_not_available)) -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(
                                    id = R.string.info_prefix,
                                    uiState.error
                                ),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    WishListItem(
                        uiState,
                        product,
                        isProductInWishlist,
                        onAddToWishlistClick,
                        onShareClick
                    )
                }
            }
        }
    }
}


@Composable
fun ErrorStateView(
    modifier: Modifier = Modifier,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = stringResource(id = R.string.error_icon_description),
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = errorMessage.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry, colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = stringResource(id = R.string.retry_icon_description)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.retry_action))
        }
    }
}