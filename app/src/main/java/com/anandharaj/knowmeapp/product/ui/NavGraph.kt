package com.anandharaj.knowmeapp.product.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.anandharaj.knowmeapp.product.utils.NavConstants
import com.anandharaj.knowmeapp.product.viewmodels.ProductViewModel

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavConstants.PRODUCTS_GRAPH_ROUTE
    ) {
        productsNavGraph(navController = navController)
    }
}

@SuppressLint("UnrememberedGetBackStackEntry")
fun NavGraphBuilder.productsNavGraph(navController: NavController) {
    navigation(
        startDestination = NavConstants.PRODUCT_LIST_ROUTE,
        route = NavConstants.PRODUCTS_GRAPH_ROUTE
    ) {
        composable(NavConstants.PRODUCT_LIST_ROUTE) { _->
            val productGraphEntry = navController.getBackStackEntry(NavConstants.PRODUCTS_GRAPH_ROUTE)
            val viewModel: ProductViewModel = hiltViewModel(viewModelStoreOwner = productGraphEntry)
            ProductListScreen(navController = navController, viewModel = viewModel)
        }
        composable(NavConstants.PRODUCT_DETAIL_ROUTE_PATTERN) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString(NavConstants.PRODUCT_ID_ARG)?.toIntOrNull()
            val productGraphEntry = navController.getBackStackEntry(NavConstants.PRODUCTS_GRAPH_ROUTE)
            val viewModel: ProductViewModel = hiltViewModel(viewModelStoreOwner = productGraphEntry)

            if (productId != null) {
                ProductDetailScreen(
                    navController = navController,
                    productId = productId,
                    viewModel = viewModel
                )
            }
        }
        composable(NavConstants.WISHLIST_SCREEN_ROUTE) { _->
            val productGraphEntry = navController.getBackStackEntry(NavConstants.PRODUCTS_GRAPH_ROUTE)
            val viewModel: ProductViewModel = hiltViewModel(viewModelStoreOwner = productGraphEntry)
            WishlistScreen(navController = navController, viewModel = viewModel)
        }
    }
}
