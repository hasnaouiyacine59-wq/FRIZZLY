# Notification Permission Fix

## Problem
Notifications are not showing because Firestore security rules are blocking access to the `notifications` collection.

**Error:**
```
Notification listener error: PERMISSION_DENIED: Missing or insufficient permissions.
```

## Root Cause
The `firestore.rules` file was missing rules for the `notifications` collection, causing all access to be denied by the default rule.

## Solution
Added Firestore security rules for the notifications collection in `firestore.rules`:

```javascript
// Notifications collection - users can read their own notifications
match /notifications/{notificationId} {
  allow read: if isSignedIn() && resource.data.userId == request.auth.uid;
  allow update: if isSignedIn() && resource.data.userId == request.auth.uid; // Allow marking as read
  allow create, delete: if false; // Only backend can create/delete
}
```

## Deploy Instructions

### Option 1: Firebase Console (Manual)
1. Go to https://console.firebase.google.com
2. Select your project
3. Navigate to **Firestore Database** → **Rules**
4. Copy the contents of `firestore.rules` file
5. Paste into the rules editor
6. Click **Publish**

### Option 2: Firebase CLI (if installed)
```bash
npm install -g firebase-tools
firebase login
firebase deploy --only firestore:rules
```

## What This Fixes
✅ Users can now read their own notifications from Firestore
✅ Users can update notifications (mark as read)
✅ Real-time notification listener will work without permission errors
✅ Notification badge count will update in real-time
✅ Backend can still create/delete notifications (via admin SDK)

## Security
- Users can only access notifications where `userId` matches their auth UID
- Users cannot create or delete notifications (only backend can)
- Users can only update their own notifications (for marking as read)
