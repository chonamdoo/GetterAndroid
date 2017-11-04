package com.oligark.getter.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import com.oligark.getter.service.model.ProductCategory
import com.oligark.getter.service.repository.ProductCategoryRepository
import com.oligark.getter.service.repository.source.DataSource
import com.oligark.getter.service.repository.source.local.StoresLocalDataSource
import com.oligark.getter.service.repository.source.remote.ProductCategoryRemoteDataSource
import com.oligark.getter.service.repository.source.remote.StoresRemoteDataSource
import com.oligark.getter.viewmodel.resources.DataResource

/**
 * Created by pmvb on 17-11-03.
 */
class FiltersViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        val TAG = FiltersViewModel::class.java.simpleName
    }

    // For now, only use remote source
    private val dataSource = ProductCategoryRemoteDataSource()
    val productCategoryRepository = ProductCategoryRepository.getInstance(
            dataSource,
            dataSource
    )

    val productCategories = MutableLiveData<DataResource<ProductCategory>>()
    val selectedProductCategories = MutableLiveData<HashMap<Int, ProductCategory>>()

    fun init() {
        productCategories.value = DataResource(listOf(), DataResource.LoadState.LOADING)
        productCategoryRepository.getItems(object : DataSource.LoadItemsCallback<ProductCategory> {
            override fun onItemsLoaded(items: List<ProductCategory>) {
                productCategories.value = DataResource(items, DataResource.LoadState.SUCCESS)
            }
            override fun onDataNotAvailable() {
                productCategories.value = DataResource(listOf(), DataResource.LoadState.ERROR)
            }
        })
    }

    fun selectCategory(category: ProductCategory) {
        val selectedCategories = selectedProductCategories.value
        selectedCategories?.put(category.id, category)
        selectedProductCategories.value = selectedCategories
    }
}