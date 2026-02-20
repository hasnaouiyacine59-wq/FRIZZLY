#!/usr/bin/env python3
"""Test script to add a notification to Firestore"""

import firebase_admin
from firebase_admin import credentials, firestore
import time

# Initialize Firebase Admin
cred = credentials.Certificate('admin-dashboard/serviceAccountKey.json')
firebase_admin.initialize_app(cred)

db = firestore.client()

# User ID from logs
user_id = 'orI6CDcLXHSfrZ5vkuwaSo2PktO2'

# Create test notification
notification_data = {
    'userId': user_id,
    'title': 'Test Notification',
    'body': 'This is a test notification to verify real-time updates work!',
    'type': 'order',
    'orderId': 'ORD1',
    'timestamp': firestore.SERVER_TIMESTAMP,
    'isRead': False
}

doc_ref = db.collection('notifications').add(notification_data)
print(f"✅ Test notification created with ID: {doc_ref[1].id}")
print(f"📱 Check your app - notification should appear instantly!")
