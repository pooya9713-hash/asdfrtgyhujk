package com.homecontrol.app.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homecontrol.app.AppContainer
import com.homecontrol.app.data.CategoryEntity
import com.homecontrol.app.data.DeviceWithCommands
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DevicesUiState(
    val devices: List<DeviceWithCommands> = emptyList(),
    val categories: List<CategoryEntity> = emptyList()
)

class DevicesViewModel(private val c: AppContainer) : ViewModel() {

    val state: StateFlow<DevicesUiState> = combine(
        c.repository.devices,
        c.repository.categories
    ) { devices, categories -> DevicesUiState(devices, categories) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DevicesUiState())

    fun deleteDevice(id: Long) { viewModelScope.launch { c.repository.deleteDevice(id) } }

    fun move(id: Long, up: Boolean) { viewModelScope.launch { c.repository.moveDevice(id, up) } }

    fun saveCategory(id: Long, name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { c.repository.saveCategory(CategoryEntity(id = id, name = clean)) }
    }

    fun deleteCategory(id: Long) { viewModelScope.launch { c.repository.deleteCategory(id) } }

    fun addSamples() { viewModelScope.launch { c.repository.seedSamples() } }
}
