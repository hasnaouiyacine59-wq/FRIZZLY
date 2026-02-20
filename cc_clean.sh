#!/bin/bash

echo "🧹 FRIZZLY Complete Clean Script"
echo "================================="

# Clean Firestore database
echo ""
echo "📦 Cleaning Firestore database..."
cd /home/oo33/AndroidStudioProjects/FRIZZLY/admin-dashboard
python3 -W ignore << 'EOF'
import firebase_admin
from firebase_admin import credentials, firestore

if not firebase_admin._apps:
    cred = credentials.Certificate('serviceAccountKey.json')
    firebase_admin.initialize_app(cred)

db = firestore.client()

# Delete all orders
orders = list(db.collection('orders').stream())
for order in orders:
    order.reference.delete()

# Delete all users
users = list(db.collection('users').stream())
for user in users:
    user.reference.delete()

# Reset order counter to 0
counter_ref = db.collection('system').document('counters')
counter_ref.set({'orderCounter': 0})

print(f"✅ Deleted {len(orders)} orders")
print(f"✅ Deleted {len(users)} users")
print("✅ Order counter reset to 0")
EOF

# Clear app data and cache
echo ""
echo "📱 Clearing app data and cache..."
adb shell pm clear com.example.effrizly 2>/dev/null
if [ $? -eq 0 ]; then
    echo "✅ App data cleared"
else
    echo "❌ Failed to clear app data (app may not be installed)"
fi

echo ""
echo "✅ Complete clean finished!"
echo "================================="
