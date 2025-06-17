package com.anandharaj.knowmeapp.product.repository

import com.anandharaj.knowmeapp.product.data.ProductApi
import javax.inject.Inject

class ProductRepository @Inject constructor(
    private val api: ProductApi
) {
     suspend fun fetchProducts() = api.getProducts()
     suspend fun fetchProductDetail(id: Int) = api.getProductDetail(id)
}