package com.anandharaj.knowmeapp.product.viewmodels.com.anandharaj.knowmeapp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class CoroutinesTestRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(), // Unconfined is good for simple tests
) : TestWatcher() {
    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}