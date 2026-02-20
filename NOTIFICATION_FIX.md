# Notification Real-Time Update Fix

## Problem
The notification system in the home screen was not updating in real-time and was causing crashes with the error:
```
androidx.compose.runtime.LeftCompositionCancellationException: The coroutine scope left the composition
```

## Root Cause
The Firestore `addSnapshotListener` was never being removed when the composition left, causing:
1. **Memory leaks** - Listeners kept running even after the screen was destroyed
2. **Coroutine scope errors** - State updates attempted after the composition scope was cancelled
3. **No real-time updates** - The listener would crash before properly updating the UI

## Solution
Added proper listener cleanup using `awaitCancellation()` and `finally` block:

```kotlin
LaunchedEffect(currentUserId) {
    val userId = currentUserId
    if (userId != null) {
        // ... setup code ...
        
        val listenerRegistration = FirebaseFirestore.getInstance()
            .collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                // ... handle updates ...
            }
        
        // Cleanup: Remove listener when LaunchedEffect is cancelled
        try {
            awaitCancellation()
        } finally {
            listenerRegistration.remove()
            android.util.Log.d("MainScreen", "🔔 Notification listener removed")
        }
    }
}
```

## Changes Made
1. **Added `awaitCancellation()` import** - `kotlinx.coroutines.awaitCancellation`
2. **Stored listener registration** - Captured the `ListenerRegistration` object
3. **Added cleanup block** - Used `try/finally` to ensure listener is removed when composition leaves

## How It Works
- `awaitCancellation()` suspends indefinitely until the LaunchedEffect is cancelled
- When the composition leaves (user navigates away, logs out, etc.), the coroutine is cancelled
- The `finally` block executes and removes the Firestore listener
- This prevents memory leaks and ensures clean state management

## Result
✅ Notifications now update in real-time without crashes
✅ Proper cleanup prevents memory leaks
✅ No more "coroutine scope left the composition" errors
✅ Notification badge count updates instantly when new notifications arrive
