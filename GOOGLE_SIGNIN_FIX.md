# Google Sign-In Configuration Issue

## Problem
Google Sign-In is failing because the OAuth client is not properly configured in Firebase Console.

## Current Status
- **Package Name**: `com.example.effrizly`
- **Firebase Project**: `frizzly-9a65f`
- **Project Number**: `941997129015`
- **Debug SHA-1**: `60:CC:26:80:F9:0E:D8:86:7B:AC:A6:49:BC:D4:E6:E2:10:A4:BA:74`

## Issue
The `google-services.json` file has an empty `oauth_client` array, which means Google Sign-In is not configured.

## Solution - Configure in Firebase Console

### Step 1: Add SHA-1 to Firebase
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select project: **frizzly-9a65f**
3. Go to **Project Settings** (gear icon)
4. Scroll to **Your apps** section
5. Find the Android app: `com.example.effrizly`
6. Click **Add fingerprint**
7. Add this SHA-1: `60:CC:26:80:F9:0E:D8:86:7B:AC:A6:49:BC:D4:E6:E2:10:A4:BA:74`
8. Click **Save**

### Step 2: Enable Google Sign-In
1. In Firebase Console, go to **Authentication**
2. Click **Sign-in method** tab
3. Click **Google** provider
4. Click **Enable**
5. Select a support email
6. Click **Save**

### Step 3: Download Updated google-services.json
1. Go back to **Project Settings**
2. Scroll to **Your apps**
3. Click **Download google-services.json**
4. Replace the file at: `app/google-services.json`

### Step 4: Get Web Client ID
After downloading the new `google-services.json`, it should contain an `oauth_client` array with a web client ID.

Update `AuthViewModel.kt` line 38 with the correct Web Client ID from the new file.

## Alternative: Use Default Sign-In (No Web Client ID)
If you don't need ID tokens for backend authentication, you can simplify the sign-in:

```kotlin
val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestEmail()
    .build()
```

Remove the `.requestIdToken()` line from AuthViewModel.kt
