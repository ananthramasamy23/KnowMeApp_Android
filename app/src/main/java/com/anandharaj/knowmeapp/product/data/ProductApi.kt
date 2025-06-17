package com.anandharaj.knowmeapp.product.data

import retrofit2.http.GET
import retrofit2.http.Path

interface ProductApi {

    @GET("products.json")
     suspend fun getProducts(): List<Product>

    @GET("product-details/{productId}.json")
    suspend fun getProductDetail(@Path("productId") id: Int): ProductDetail

}