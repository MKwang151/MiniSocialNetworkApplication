package com.example.minisocialnetworkapplication.core.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.minisocialnetworkapplication.core.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes app lifecycle using ProcessLifecycleOwner.
 * This is more reliable than Activity lifecycle for tracking online/offline status
 * because it observes the entire app process, not just individual activities.
 * 
 * - onStart: App comes to foreground -> set user online
 * - onStop: App goes to background -> set user offline with lastActive timestamp
 */
@Singleton
class AppLifecycleObserver @Inject constructor(
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // App comes to foreground
        updatePresence(isOnline = true)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // App goes to background
        updatePresence(isOnline = false)
    }

    private fun updatePresence(isOnline: Boolean) {
        // Only update if user is logged in
        if (firebaseAuth.currentUser != null) {
            scope.launch {
                try {
                    userRepository.updatePresence(isOnline)
                    Timber.d("AppLifecycleObserver: Presence updated to $isOnline")
                } catch (e: Exception) {
                    Timber.e(e, "AppLifecycleObserver: Failed to update presence")
                }
            }
        } else {
            Timber.d("AppLifecycleObserver: Skipping presence update - user not logged in")
        }
    }
}
