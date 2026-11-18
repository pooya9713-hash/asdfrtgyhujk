package com.homecontrol.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homecontrol.app.AppContainer
import com.homecontrol.app.data.HistoryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val c: AppContainer) : ViewModel() {
    val items: StateFlow<List<HistoryEntity>> = c.repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) { viewModelScope.launch { c.repository.deleteHistory(id) } }
    fun clear() { viewModelScope.launch { c.repository.clearHistory() } }
}
