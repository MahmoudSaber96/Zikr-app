package com.dhikr.app

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.dhikr.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

sealed interface Screen {
    data object Home : Screen
    data object Achievements : Screen
    data object Settings : Screen
    data class Focus(val id: Long) : Screen
}

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    private val player = ClickPlayer()
    private val tapLock = Mutex()   // keeps rapid taps strictly sequential

    val counters = repo.counters.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = repo.prefs.settings
    val stats = repo.prefs.stats

    var screen by mutableStateOf<Screen>(Screen.Home)
    var celebrating by mutableStateOf<String?>(null)
        private set

    var today by mutableLongStateOf(LocalDate.now().toEpochDay())
        private set
    val dailyMessage get() = repo.messageFor(today)
    /** Call on every ON_RESUME so the message rolls over after midnight. */
    fun refreshDay() { today = LocalDate.now().toEpochDay() }
    fun dismissBanner() = repo.prefs.dismissBanner(today)

    fun tap(id: Long) = viewModelScope.launch {
        tapLock.withLock {
            val hit = repo.tap(id) ?: return@withLock
            val s = settings.value
            if (hit) {
                if (s.completeSound) player.complete()
                celebrating = counters.value.firstOrNull { it.id == id }?.name ?: ""
            } else if (s.tick) player.tick()
        }
    }
    fun dismissCelebration() { celebrating = null }
    fun add(name: String, target: Int) = viewModelScope.launch { repo.add(name, target) }
    fun reset(c: Counter) = viewModelScope.launch { repo.reset(c) }
    fun delete(c: Counter) = viewModelScope.launch { repo.delete(c) }
    fun setTick(v: Boolean) = repo.prefs.setTick(v)
    fun setComplete(v: Boolean) = repo.prefs.setComplete(v)
    fun finishOnboarding() = repo.prefs.setOnboarded()
    override fun onCleared() { player.release() }
}
