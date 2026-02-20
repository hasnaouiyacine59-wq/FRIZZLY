package com.example.f_rizzly

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object DataStore {
    private const val PREFS_NAME = "frizzly_prefs"
    private const val ORDER_COUNTER_KEY = "order_counter"
    
    private val gson = Gson()
    
    private fun getPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            android.util.Log.e("DataStore", "Failed to create encrypted prefs, falling back to regular: ${e.message}")
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
    
    // Get next order number (sequential)
    fun getNextOrderNumber(context: Context): Long {
        val prefs = getPrefs(context)
        val current = prefs.getLong(ORDER_COUNTER_KEY, 0L)
        val next = current + 1
        prefs.edit().putLong(ORDER_COUNTER_KEY, next).apply()
        return next
    }
    
    // Cart Items - user-specific
    fun saveCartItems(context: Context, userId: String?, items: List<CartItem>) {
        val key = "cart_items_${userId ?: "anonymous"}"
        val json = gson.toJson(items)
        getPrefs(context).edit().putString(key, json).apply()
    }
    
    fun loadCartItems(context: Context, userId: String?): List<CartItem> {
        val key = "cart_items_${userId ?: "anonymous"}"
        val json = getPrefs(context).getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<CartItem>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Orders - user-specific
    fun saveOrders(context: Context, userId: String?, orders: List<Order>) {
        val key = "orders_${userId ?: "anonymous"}"
        val json = gson.toJson(orders)
        getPrefs(context).edit().putString(key, json).apply()
    }
    
    fun loadOrders(context: Context, userId: String?): List<Order> {
        val key = "orders_${userId ?: "anonymous"}"
        val json = getPrefs(context).getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<Order>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Notifications
    fun saveNotifications(context: Context, userId: String?, notifications: List<AppNotification>) {
        val key = "notifications_${userId ?: "anonymous"}"
        val json = gson.toJson(notifications)
        getPrefs(context).edit().putString(key, json).apply()
    }
    
    fun loadNotifications(context: Context, userId: String?): List<AppNotification> {
        val key = "notifications_${userId ?: "anonymous"}"
        val json = getPrefs(context).getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<AppNotification>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val type: String, // "order", "promo", "general"
    val timestamp: Long,
    val orderId: String? = null,
    val isRead: Boolean = false
)
