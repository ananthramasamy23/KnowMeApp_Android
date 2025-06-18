package com.anandharaj.knowmeapp.product.viewmodels

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.anandharaj.knowmeapp.R
import com.anandharaj.knowmeapp.product.data.Product
import com.anandharaj.knowmeapp.product.data.ProductDetail
import com.anandharaj.knowmeapp.product.di.ResourceProvider
import com.anandharaj.knowmeapp.product.repository.ProductRepository
import com.anandharaj.knowmeapp.product.ui.ProductScreenEvent
import com.anandharaj.knowmeapp.product.ui.ProductScreenState
import com.anandharaj.knowmeapp.product.utils.NetworkConnectivityChecker
import com.anandharaj.knowmeapp.product.viewmodels.com.anandharaj.knowmeapp.CoroutinesTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response
import okio.IOException
import org.junit.Rule
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ProductViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @Mock
    private lateinit var mockRepository: ProductRepository

    @Mock
    private lateinit var mockNetworkChecker: NetworkConnectivityChecker

    @Mock
    private lateinit var mockApplication: Application

    @Mock
    private lateinit var mockContext: Application
    private lateinit var viewModel: ProductViewModel
    private lateinit var mockResourceProvider: ResourceProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockResourceProvider = mock()
        viewModel = ProductViewModel(
            repository = mockRepository,
            networkConnectivityChecker = mockNetworkChecker,
            application = mockApplication,
            context = mockContext,
            resourceProvider = mockResourceProvider
        )
        `when`(mockResourceProvider.getString(R.string.error_no_internet_connection))
            .thenReturn("No internet connection")
        `when`(mockResourceProvider.getString(R.string.error_network_prefix))
            .thenReturn("Network error: ")
        `when`(mockResourceProvider.getString(R.string.error_fetching_product))
            .thenReturn("Error fetching detail: ")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadProducts success updates uiState with products and loading false`() =
        runTest(testDispatcher) {
            val mockProducts = listOf(
                Product(1, "Product 1", "Desc 1", "image1.url"),
                Product(2, "Product 2", "Desc 2", "image2.url")
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
    fun `loadProducts no internet updates uiState with error and loading false`() =
        runTest(testDispatcher) {
            whenever(mockNetworkChecker.isNetworkAvailable()).thenReturn(false)
            viewModel.uiState.test {
                viewModel.onEvent(ProductScreenEvent.LoadProductList)
                assertEquals(ProductScreenState(), awaitItem())
                val errorState = awaitItem()
                assertFalse(errorState.isLoadingList)
                assertEquals(
                    mockResourceProvider.getString(R.string.error_no_internet_connection),
                    errorState.error
                )
                assertNull(errorState.products)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `loadProducts repository throws IOException updates uiState with network error`() =
        runTest(testDispatcher) {
            val errorMessage = mockResourceProvider.getString(R.string.error_network_prefix)
            whenever(mockNetworkChecker.isNetworkAvailable()).thenReturn(true)
            whenever(mockRepository.fetchProducts()).doSuspendableAnswer {
                throw IOException(
                    errorMessage
                )
            }


            viewModel.uiState.test {
                viewModel.onEvent(ProductScreenEvent.LoadProductList)
                assertEquals(ProductScreenState(), awaitItem())
                assertTrue(awaitItem().isLoadingList)
                val errorState = awaitItem()
                assertFalse(errorState.isLoadingList)
                assertEquals(
                    mockResourceProvider.getString(R.string.error_network_prefix, errorMessage),
                    errorState.error
                )
                assertNull(errorState.products)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `loadProducts repository throws HttpException updates uiState with server error`() =
        runTest(testDispatcher) {
            val errorCode = 404
            val mockHttpException =
                HttpException(Response.error<Any>(errorCode, "".toResponseBody(null)))
            whenever(mockNetworkChecker.isNetworkAvailable()).thenReturn(true)
            whenever(mockRepository.fetchProducts()).doSuspendableAnswer { throw mockHttpException }

            viewModel.uiState.test {

                viewModel.onEvent(ProductScreenEvent.LoadProductList)
                assertEquals(ProductScreenState(), awaitItem())
                assertTrue(awaitItem().isLoadingList)
                val errorState = awaitItem()
                assertFalse(errorState.isLoadingList)
                assertEquals(
                    mockResourceProvider.getString(
                        R.string.error_server_prefix,
                        errorCode
                    ), errorState.error
                )
                assertNull(errorState.products)
                cancelAndIgnoreRemainingEvents()
            }
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

    @Test
    fun `loadProductDetail no internet updates uiState with error`() = runTest(testDispatcher) {
        val productId = 1
        whenever(mockNetworkChecker.isNetworkAvailable()).thenReturn(false)
        viewModel.uiState.test {
            viewModel.onEvent(ProductScreenEvent.LoadProductDetail(productId))
            assertEquals(ProductScreenState(), awaitItem())
            val errorState = awaitItem()
            assertFalse(errorState.isLoadingDetail)
            assertEquals(
                mockResourceProvider.getString(R.string.error_no_internet_connection),
                errorState.error
            )
            assertNull(errorState.selectedProduct)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadProductDetail repository throws Exception updates uiState with error`() =
        runTest(testDispatcher) {
            val productId = 1
            val errorMessage = mockResourceProvider.getString(R.string.error_fetching_product)
            whenever(mockNetworkChecker.isNetworkAvailable()).thenReturn(true)
            whenever(mockRepository.fetchProductDetail(productId)).doSuspendableAnswer {
                throw RuntimeException(
                    errorMessage
                )
            }

            viewModel.uiState.test {
                viewModel.onEvent(ProductScreenEvent.LoadProductDetail(productId))
                assertEquals(ProductScreenState(), awaitItem())
                val loadingState = awaitItem()
                assertTrue(loadingState.isLoadingDetail)
                assertNull(loadingState.selectedProduct)
                val errorState = awaitItem()
                assertFalse(errorState.isLoadingDetail)
                assertEquals(
                    mockResourceProvider.getString(
                        R.string.error_fetching_product,
                        errorMessage
                    ), errorState.error
                )
                assertNull(errorState.selectedProduct)
                cancelAndIgnoreRemainingEvents()
            }
        }
}