from flask import Flask, request, jsonify
from flask_cors import CORS
import firebase_admin
from firebase_admin import credentials, firestore, auth
import os
from datetime import datetime

app = Flask(__name__)
CORS(app)

# Initialize Firebase Admin
cred = credentials.Certificate('serviceAccountKey.json')
firebase_admin.initialize_app(cred)
db = firestore.client()

# ==================== ORDERS API ====================

@app.route('/api/orders', methods=['GET'])
def get_orders():
    """Get all orders for a user"""
    try:
        user_id = request.args.get('userId')
        if not user_id:
            return jsonify({'error': 'userId required'}), 400
        
        orders_ref = db.collection('orders').where('userId', '==', user_id).stream()
        orders = []
        for doc in orders_ref:
            order = doc.to_dict()
            order['id'] = doc.id
            orders.append(order)
        
        return jsonify({'orders': orders}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/orders', methods=['POST'])
def create_order():
    """Create a new order"""
    try:
        data = request.json
        user_id = data.get('userId')
        
        if not user_id:
            return jsonify({'error': 'userId required'}), 400
        
        order = {
            'userId': user_id,
            'orderId': data.get('orderId'),
            'items': data.get('items', []),
            'totalAmount': data.get('totalAmount', 0),
            'deliveryLocation': data.get('deliveryLocation'),
            'status': data.get('status', 'PENDING'),
            'timestamp': firestore.SERVER_TIMESTAMP,
            'createdAt': datetime.now().isoformat()
        }
        
        doc_ref = db.collection('orders').document(order['orderId'])
        doc_ref.set(order)
        
        return jsonify({'success': True, 'orderId': order['orderId']}), 201
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/orders/<order_id>', methods=['PUT'])
def update_order(order_id):
    """Update order status"""
    try:
        data = request.json
        status = data.get('status')
        
        if not status:
            return jsonify({'error': 'status required'}), 400
        
        doc_ref = db.collection('orders').document(order_id)
        doc_ref.update({
            'status': status,
            'updatedAt': firestore.SERVER_TIMESTAMP
        })
        
        return jsonify({'success': True}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/orders/<order_id>', methods=['DELETE'])
def delete_order(order_id):
    """Delete/cancel an order"""
    try:
        db.collection('orders').document(order_id).delete()
        return jsonify({'success': True}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ==================== PRODUCTS API ====================

@app.route('/api/products', methods=['GET'])
def get_products():
    """Get all products"""
    try:
        products_ref = db.collection('products').stream()
        products = []
        for doc in products_ref:
            product = doc.to_dict()
            product['id'] = doc.id
            products.append(product)
        
        return jsonify({'products': products}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/products', methods=['POST'])
def create_product():
    """Create a new product"""
    try:
        data = request.json
        
        product = {
            'name': data.get('name'),
            'price': data.get('price'),
            'category': data.get('category'),
            'imageUrl': data.get('imageUrl'),
            'description': data.get('description', ''),
            'inStock': data.get('inStock', True),
            'createdAt': firestore.SERVER_TIMESTAMP
        }
        
        doc_ref = db.collection('products').add(product)
        
        return jsonify({'success': True, 'productId': doc_ref[1].id}), 201
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/products/<product_id>', methods=['PUT'])
def update_product(product_id):
    """Update a product"""
    try:
        data = request.json
        
        doc_ref = db.collection('products').document(product_id)
        doc_ref.update(data)
        
        return jsonify({'success': True}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/products/<product_id>', methods=['DELETE'])
def delete_product(product_id):
    """Delete a product"""
    try:
        db.collection('products').document(product_id).delete()
        return jsonify({'success': True}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ==================== USERS API ====================

@app.route('/api/users/<user_id>', methods=['GET'])
def get_user(user_id):
    """Get user profile"""
    try:
        doc = db.collection('users').document(user_id).get()
        if doc.exists:
            user = doc.to_dict()
            user['id'] = doc.id
            return jsonify({'user': user}), 200
        else:
            return jsonify({'error': 'User not found'}), 404
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/users', methods=['POST'])
def create_user():
    """Create user profile"""
    try:
        data = request.json
        user_id = data.get('userId')
        
        if not user_id:
            return jsonify({'error': 'userId required'}), 400
        
        user = {
            'userId': user_id,
            'email': data.get('email'),
            'displayName': data.get('displayName'),
            'phoneNumbers': data.get('phoneNumbers', []),
            'createdAt': firestore.SERVER_TIMESTAMP
        }
        
        db.collection('users').document(user_id).set(user)
        
        return jsonify({'success': True}), 201
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ==================== ANALYTICS API ====================

@app.route('/api/analytics/orders', methods=['GET'])
def get_order_analytics():
    """Get order statistics"""
    try:
        user_id = request.args.get('userId')
        
        query = db.collection('orders')
        if user_id:
            query = query.where('userId', '==', user_id)
        
        orders = list(query.stream())
        
        total_orders = len(orders)
        total_revenue = sum(doc.to_dict().get('totalAmount', 0) for doc in orders)
        
        status_counts = {}
        for doc in orders:
            status = doc.to_dict().get('status', 'UNKNOWN')
            status_counts[status] = status_counts.get(status, 0) + 1
        
        return jsonify({
            'totalOrders': total_orders,
            'totalRevenue': total_revenue,
            'statusCounts': status_counts
        }), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ==================== HEALTH CHECK ====================

@app.route('/', methods=['GET'])
def home():
    """API welcome page"""
    return jsonify({
        'name': 'FRIZZLY API',
        'version': '1.0.0',
        'status': 'running',
        'endpoints': {
            'health': '/api/health',
            'orders': '/api/orders',
            'products': '/api/products',
            'users': '/api/users',
            'analytics': '/api/analytics/orders'
        },
        'documentation': 'See README.md for full API documentation'
    }), 200

@app.route('/api/health', methods=['GET'])
def health_check():
    """API health check"""
    return jsonify({
        'status': 'healthy',
        'timestamp': datetime.now().isoformat()
    }), 200

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)
