package com.dhikr.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dhikr.app.MainViewModel
import com.dhikr.app.Screen

@Composable
fun DhikrApp(vm: MainViewModel = viewModel()) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {   // roll the daily message over after midnight
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) vm.refreshDay() }
        owner.lifecycle.addObserver(o); onDispose { owner.lifecycle.removeObserver(o) }
    }
    DhikrTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val settings by vm.settings.collectAsStateWithLifecycle()
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                if (!settings.onboarded) Onboarding(vm::finishOnboarding)
                else Box(Modifier.fillMaxSize()) {
                    when (val s = vm.screen) {
                        Screen.Home -> HomeScreen(vm)
                        Screen.Achievements -> AchievementsScreen(vm)
                        Screen.Settings -> SettingsScreen(vm)
                        is Screen.Focus -> FocusScreen(vm, s.id)
                    }
                    vm.celebrating?.let { CelebrationOverlay(it, vm::dismissCelebration) }
                }
            }
        }
    }
}
