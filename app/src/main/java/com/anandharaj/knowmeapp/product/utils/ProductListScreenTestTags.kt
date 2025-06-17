package com.anandharaj.knowmeapp.product.utils

object ProductListScreenTestTags {
    const val SCREEN_ROOT = "productListScreenRoot"
    const val TOP_APP_BAR_TITLE = "topAppBarTitle_productList"
    const val LOADING_INDICATOR = "loadingIndicator_productList"
    const val ERROR_ICON = "errorIcon_productList"
    const val ERROR_MESSAGE_TEXT = "errorMessageText_productList"
    const val RETRY_BUTTON = "retryButton_productList"
    const val PRODUCT_LIST = "productList_lazyColumn"
    const val PRODUCT_LIST_ITEM_PREFIX = "productListItem_" // e.g., productListItem_123
    const val NO_PRODUCTS_TEXT = "noProductsText_productList"
    fun productItemImageTag(productId: String) = "productImage_$productId"
    fun productItemTitleTag(productId: String) = "productTitle_$productId"
}