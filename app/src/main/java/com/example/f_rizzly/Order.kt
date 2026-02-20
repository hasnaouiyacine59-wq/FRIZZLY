package com.example.f_rizzly

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import java.text.SimpleDateFormat
import java.util.*

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING_ORDER,
    READY_FOR_PICKUP,
    ON_WAY,
    OUT_FOR_DELIVERY,
    DELIVERED,
    DELIVERY_ATTEMPT_FAILED,
    COMPLETED,
    CANCELLED,
    RETURNED
}

data class Order(
    val orderId: String,
    val items: List<CartItem>,
    val totalAmount: Double,
    val deliveryLocation: Pair<Double, Double>?,
    val timestamp: Long = System.currentTimeMillis(),
    var status: OrderStatus = OrderStatus.PENDING
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    fun getStatusColor(): androidx.compose.ui.graphics.Color {
        return when (status) {
            OrderStatus.PENDING -> AppColors.Orange
            OrderStatus.CONFIRMED -> androidx.compose.ui.graphics.Color(0xFF2196F3)
            OrderStatus.PREPARING_ORDER -> androidx.compose.ui.graphics.Color(0xFFFF9800)
            OrderStatus.READY_FOR_PICKUP -> androidx.compose.ui.graphics.Color(0xFF00BCD4)
            OrderStatus.ON_WAY -> androidx.compose.ui.graphics.Color(0xFF9C27B0)
            OrderStatus.OUT_FOR_DELIVERY -> androidx.compose.ui.graphics.Color(0xFF673AB7)
            OrderStatus.DELIVERED -> AppColors.ActiveGreen
            OrderStatus.DELIVERY_ATTEMPT_FAILED -> androidx.compose.ui.graphics.Color(0xFFFF5722)
            OrderStatus.COMPLETED -> AppColors.ActiveGreen
            OrderStatus.CANCELLED -> androidx.compose.ui.graphics.Color(0xFFDC3545)
            OrderStatus.RETURNED -> androidx.compose.ui.graphics.Color(0xFF795548)
        }
    }
    
    fun getStatusText(): String {
        return when (status) {
            OrderStatus.PENDING -> "Pending"
            OrderStatus.CONFIRMED -> "Confirmed"
            OrderStatus.PREPARING_ORDER -> "Preparing"
            OrderStatus.READY_FOR_PICKUP -> "Ready"
            OrderStatus.ON_WAY -> "On the Way"
            OrderStatus.OUT_FOR_DELIVERY -> "Out for Delivery"
            OrderStatus.DELIVERED -> "Delivered"
            OrderStatus.DELIVERY_ATTEMPT_FAILED -> "Delivery Failed"
            OrderStatus.COMPLETED -> "Completed"
            OrderStatus.CANCELLED -> "Cancelled"
            OrderStatus.RETURNED -> "Returned"
        }
    }
    
    fun getStatusIcon(): androidx.compose.ui.graphics.vector.ImageVector {
        return when (status) {
            OrderStatus.PENDING -> Icons.Filled.Schedule
            OrderStatus.CONFIRMED -> Icons.Filled.CheckCircle
            OrderStatus.PREPARING_ORDER -> Icons.Filled.Restaurant
            OrderStatus.READY_FOR_PICKUP -> Icons.Filled.ShoppingBag
            OrderStatus.ON_WAY -> Icons.Filled.LocalShipping
            OrderStatus.OUT_FOR_DELIVERY -> Icons.Filled.DeliveryDining
            OrderStatus.DELIVERED -> Icons.Filled.Done
            OrderStatus.DELIVERY_ATTEMPT_FAILED -> Icons.Filled.ErrorOutline
            OrderStatus.COMPLETED -> Icons.Filled.TaskAlt
            OrderStatus.CANCELLED -> Icons.Filled.Cancel
            OrderStatus.RETURNED -> Icons.Filled.KeyboardReturn
        }
    }
}
