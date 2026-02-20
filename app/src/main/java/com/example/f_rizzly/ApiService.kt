package com.example.f_rizzly

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiService {
    private const val BASE_URL = "https://yacinedev84.pythonanywhere.com/api"
    
    suspend fun checkHealth(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/health")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                Result.success(response)
            } else {
                Result.failure(Exception("API health check failed: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun submitOrder(order: Order, userId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/order/submit")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            // Build JSON payload
            val orderObj = JSONObject().apply {
                put("orderId", order.orderId)  // Temp ID
                put("totalAmount", order.totalAmount)
                put("status", order.status.name)
                put("timestamp", order.timestamp)
                
                // Add delivery location
                val locationObj = JSONObject().apply {
                    put("latitude", order.deliveryLocation?.first ?: 0.0)
                    put("longitude", order.deliveryLocation?.second ?: 0.0)
                }
                put("deliveryLocation", locationObj)
                
                // Add items
                val itemsArray = JSONArray()
                order.items.forEach { cartItem ->
                    val itemObj = JSONObject().apply {
                        put("productName", cartItem.product.name)
                        put("productPrice", cartItem.product.price)
                        put("quantity", cartItem.quantity)
                    }
                    itemsArray.put(itemObj)
                }
                put("items", itemsArray)
            }
            
            val jsonObject = JSONObject().apply {
                put("userId", userId)
                put("order", orderObj)
            }
            
            // Send request
            connection.outputStream.use { os ->
                os.write(jsonObject.toString().toByteArray())
            }
            
            val responseCode = connection.responseCode
            val response = if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            }
            
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                Result.success(response)
            } else {
                Result.failure(Exception("Order submission failed: $responseCode - $response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getOrders(userId: String): Result<List<Order>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/orders?userId=$userId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val orders = parseOrdersFromJson(response)
                Result.success(orders)
            } else {
                Result.failure(Exception("Failed to fetch orders: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseOrdersFromJson(json: String): List<Order> {
        val orders = mutableListOf<Order>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                
                // Parse items
                val items = mutableListOf<CartItem>()
                val itemsArray = obj.getJSONArray("items")
                for (j in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.getJSONObject(j)
                    val product = Product(
                        name = itemObj.getString("productName"),
                        price = itemObj.getString("productPrice"),
                        imageResId = 0 // Will be mapped later
                    )
                    items.add(CartItem(product, itemObj.getDouble("quantity")))
                }
                
                // Parse location
                val locationObj = obj.optJSONObject("deliveryLocation")
                val location = if (locationObj != null) {
                    Pair(locationObj.getDouble("latitude"), locationObj.getDouble("longitude"))
                } else null
                
                val order = Order(
                    orderId = obj.getString("orderId"),
                    items = items,
                    totalAmount = obj.getDouble("totalAmount"),
                    deliveryLocation = location,
                    status = OrderStatus.valueOf(obj.getString("status")),
                    timestamp = obj.getLong("timestamp")
                )
                orders.add(order)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return orders
    }
}
