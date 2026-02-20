import requests
import json

BASE_URL = "http://localhost:5000/api"

def test_health():
    """Test API health"""
    response = requests.get(f"{BASE_URL}/health")
    print("Health Check:", response.json())

def test_create_order():
    """Test creating an order"""
    order = {
        "userId": "test_user_123",
        "orderId": "ORD" + str(int(1000000)),
        "items": [
            {
                "product": {
                    "name": "Apple",
                    "price": "$2.99/kg"
                },
                "quantity": 2.5
            }
        ],
        "totalAmount": 7.48,
        "deliveryLocation": {
            "latitude": 40.7128,
            "longitude": -74.0060
        },
        "status": "PENDING"
    }
    
    response = requests.post(f"{BASE_URL}/orders", json=order)
    print("Create Order:", response.json())
    return order["orderId"]

def test_get_orders(user_id):
    """Test getting orders"""
    response = requests.get(f"{BASE_URL}/orders?userId={user_id}")
    print("Get Orders:", response.json())

def test_update_order(order_id):
    """Test updating order status"""
    response = requests.put(f"{BASE_URL}/orders/{order_id}", json={"status": "DELIVERED"})
    print("Update Order:", response.json())

def test_analytics(user_id):
    """Test analytics"""
    response = requests.get(f"{BASE_URL}/analytics/orders?userId={user_id}")
    print("Analytics:", response.json())

if __name__ == "__main__":
    print("=== Testing FRIZZLY API ===\n")
    
    # Test health
    test_health()
    print()
    
    # Test create order
    order_id = test_create_order()
    print()
    
    # Test get orders
    test_get_orders("test_user_123")
    print()
    
    # Test update order
    test_update_order(order_id)
    print()
    
    # Test analytics
    test_analytics("test_user_123")
