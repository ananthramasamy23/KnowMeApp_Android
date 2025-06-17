package com.anandharaj.knowmeapp.product.viewmodels

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.anandharaj.knowmeapp.product.data.Product
import com.anandharaj.knowmeapp.product.data.ProductDetail
import com.anandharaj.knowmeapp.product.repository.ProductRepository
import com.anandharaj.knowmeapp.product.ui.ProductScreenEvent
import com.anandharaj.knowmeapp.product.ui.ProductScreenState
import com.anandharaj.knowmeapp.product.utils.NetworkConnectivityChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.junit.Rule

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ProductViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockRepository: ProductRepository

    @Mock
    private lateinit var mockNetworkChecker: NetworkConnectivityChecker

    @Mock
    private lateinit var mockApplication: Application

    private lateinit var viewModel: ProductViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ProductViewModel(
            repository = mockRepository,
            networkConnectivityChecker = mockNetworkChecker,
            application = mockApplication
        )

    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadProducts success updates uiState with products and loading false`() =
        runTest(testDispatcher) {
            val mockProducts = listOf(
                Product(1, "image1.url","Product 1", "Desc 1" ),
                Product(2, "image1.url","Product 2", "Desc 2" ),
            )
            whenever(mockNetworkChecker.isNetworkAvailable()).thenReturn(true)
            whenever(mockRepository.fetchProducts()).thenReturn(mockProducts)

            viewModel.uiState.test {
                viewModel.onEvent(ProductScreenEvent.LoadProductList)
                assertEquals(ProductScreenState(), awaitItem())
                val loadingState = awaitItem()
                assertTrue(loadingState.isLoadingList)
                assertNull(loadingState.error)
                val successState = awaitItem()
                assertFalse(successState.isLoadingList)
                assertEquals(mockProducts, successState.products)
                assertNull(successState.error)
                cancelAndIgnoreRemainingEvents()
            }
            verify(mockRepository).fetchProducts()
        }

    @Test
    fun `loadProductDetail success updates uiState with selectedProduct`() =
        runTest(testDispatcher) {
            val productId = 1
            val mockProductDetail = ProductDetail(
                productId,
                "Detail Product",
                "Detail Desc",
                "detail.url",
                "Detail Summary",
                "$50.0,"
            )
            whenever(mockNetworkChecker.isNetworkAvailable()).thenReturn(true)
            whenever(mockRepository.fetchProductDetail(productId)).thenReturn(mockProductDetail)

            viewModel.uiState.test {

                viewModel.onEvent(ProductScreenEvent.LoadProductDetail(productId))
                assertEquals(ProductScreenState(), awaitItem())
                val loadingState = awaitItem()
                assertTrue(loadingState.isLoadingDetail)
                assertNull(loadingState.error)
                assertNull(loadingState.selectedProduct)
                val successState = awaitItem()
                assertFalse(successState.isLoadingDetail)
                assertEquals(mockProductDetail, successState.selectedProduct)
                assertNull(successState.error)
                cancelAndIgnoreRemainingEvents()
            }
            verify(mockRepository).fetchProductDetail(productId)
        }

}