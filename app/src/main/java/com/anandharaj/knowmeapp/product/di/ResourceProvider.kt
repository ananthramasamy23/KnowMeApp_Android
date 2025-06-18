package com.anandharaj.knowmeapp.product.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

interface ResourceProvider {
    fun getString(resId: Int): String
    fun getString(resId: Int, vararg formatArgs: Any): String
}

class AppResourceProvider @Inject constructor(@ApplicationContext private val context: Context) : ResourceProvider {
    override fun getString(resId: Int): String {
        return context.getString(resId)
    }
    override fun getString(resId: Int, vararg formatArgs: Any): String {
        return context.getString(resId, *formatArgs)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ResourceModule {
    @Binds
    abstract fun bindResourceProvider(impl: AppResourceProvider): ResourceProvider
}