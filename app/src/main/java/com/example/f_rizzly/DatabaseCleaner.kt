package com.example.f_rizzly

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object DatabaseCleaner {
    
    suspend fun clearAllUserData(context: Context): Boolean {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Toast.makeText(context, "No user logged in", Toast.LENGTH_SHORT).show()
                return false
            }
            
            val firestore = FirebaseFirestore.getInstance()
            
            // Clear orders
            val ordersSnapshot = firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            ordersSnapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }
            
            // Clear user data
            firestore.collection("users")
                .document(userId)
                .delete()
                .await()
            
            // Clear local storage
            DataStore.saveOrders(context, userId, emptyList())
            DataStore.saveCartItems(context, userId, emptyList())
            
            Toast.makeText(context, "All data cleared successfully", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
    
    suspend fun clearAllOrders(context: Context): Boolean {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Toast.makeText(context, "No user logged in", Toast.LENGTH_SHORT).show()
                return false
            }
            
            val firestore = FirebaseFirestore.getInstance()
            
            // Clear all orders for this user
            val ordersSnapshot = firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            ordersSnapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }
            
            // Clear local storage
            DataStore.saveOrders(context, userId, emptyList())
            
            Toast.makeText(context, "All orders cleared", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
