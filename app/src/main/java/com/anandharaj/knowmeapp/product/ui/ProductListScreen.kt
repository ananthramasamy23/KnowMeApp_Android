package com.anandharaj.knowmeapp.product.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.anandharaj.knowmeapp.R
import com.anandharaj.knowmeapp.product.data.Product
import com.anandharaj.knowmeapp.product.utils.NavConstants
import com.anandharaj.knowmeapp.product.utils.ProductListScreenTestTags
import com.anandharaj.knowmeapp.product.utils.ProductListScreenTestTags.productItemImageTag
import com.anandharaj.knowmeapp.product.utils.ProductListScreenTestTags.productItemTitleTag
import com.anandharaj.knowmeapp.product.viewmodels.ProductViewModel
import com.anandharaj.knowmeapp.ui.theme.KnowMeAppTheme
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ProductListScreen(
    navController: NavController,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.onEvent(ProductScreenEvent.LoadProductList)
    }

    KnowMeAppTheme {
        Scaffold(
            modifier = Modifier.testTag(ProductListScreenTestTags.SCREEN_ROOT),
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(id = R.string.product_list_title),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(ProductListScreenTestTags.TOP_APP_BAR_TITLE),
                            )
                        },
                        actions = {
                            BadgedBox(modifier = Modifier.padding(6.dp), badge = {
                                if (uiState.wishlistItems.isNotEmpty()) {
                                    Badge {
                                        Text(uiState.wishlistItems.size.toString())
                                    }
                                }
                            }) {
                                IconButton(onClick = {
                                    navController.navigate(NavConstants.WISHLIST_SCREEN_ROUTE)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.FavoriteBorder,
                                        contentDescription = stringResource(id = R.string.wishlist_icon_description)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.size(8.dp))
                        }
                    )
                    // Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { query ->
                            viewModel.onEvent(ProductScreenEvent.SearchProducts(query))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("search_text_field"),
                        placeholder = { Text(stringResource(id = R.string.search_products_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_icon_description)) },
                        singleLine = true,

                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.White)
            ) {
                when {
                    uiState.isLoadingList && uiState.products.isNullOrEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.Gray,
                                modifier = Modifier.testTag(
                                    ProductListScreenTestTags.LOADING_INDICATOR
                                )
                            )
                        }
                    }

                    uiState.error != null && uiState.products.isNullOrEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                imageVector = Icons.Filled.Face,
                                contentDescription = stringResource(id = R.string.loading_products_error_description),
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .size(100.dp)
                                    .testTag(ProductListScreenTestTags.ERROR_ICON)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = uiState.error
                                    ?: stringResource(id = R.string.error_unknown_occurred),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .testTag(ProductListScreenTestTags.ERROR_MESSAGE_TEXT)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    viewModel.onEvent(ProductScreenEvent.LoadProductList)
                                },
                                modifier = Modifier.testTag(ProductListScreenTestTags.RETRY_BUTTON),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = stringResource(id = R.string.retry_icon_description)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(id = R.string.retry_button_text),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    !uiState.products.isNullOrEmpty() || uiState.searchQuery.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(ProductListScreenTestTags.PRODUCT_LIST),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            items(
                                items = uiState.products.orEmpty(),
                                key = { product -> product.id }) { product ->
                                ProductListItem(
                                    product = product,
                                    onItemClick = {
                                        navController.navigate("product_detail/${product.id}")
                                    },
                                    modifier = Modifier
                                        .testTag(
                                            "${
                                                ProductListScreenTestTags.PRODUCT_LIST_ITEM_PREFIX
                                            }${
                                                product.id
                                            }"
                                        )
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }

                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.no_products_found),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .testTag(ProductListScreenTestTags.NO_PRODUCTS_TEXT)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ProductListItem(
    product: Product,
    onItemClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    KnowMeAppTheme {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onItemClick(product) }
                .padding(vertical = 8.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlideImage(
                model = product.imageUrl,
                contentDescription =
                    stringResource(R.string.product_image_description, product.title),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
                    .testTag(productItemImageTag("${product.id}"))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    modifier = Modifier
                        .testTag(productItemTitleTag("${product.id}"))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    product.summary,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
        }
    }
}