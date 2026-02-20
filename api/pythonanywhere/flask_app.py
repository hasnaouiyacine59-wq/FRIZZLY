from flask import Flask, request, jsonify
from flask_cors import CORS
import firebase_admin
from firebase_admin import credentials, firestore
from datetime import datetime
import os

app = Flask(__name__)
CORS(app)

# Initialize Firebase Admin
db = None
try:
    # Try multiple paths for serviceAccountKey.json
    possible_paths = [
        os.path.join(os.path.dirname(__file__), 'serviceAccountKey.json'),
        os.path.join(os.path.dirname(os.path.abspath(__file__)), 'serviceAccountKey.json'),
        '/home/yacinedev84/mysite/serviceAccountKey.json',
        'serviceAccountKey.json'
    ]
    
    cred_path = None
    for path in possible_paths:
        if os.path.exists(path):
            cred_path = path
            break
    
    if not cred_path:
        print("ERROR: serviceAccountKey.json not found in any of these locations:")
        for path in possible_paths:
            print(f"  - {path}")
        raise FileNotFoundError("serviceAccountKey.json not found")
    
    print(f"Loading Firebase credentials from: {cred_path}")
    cred = credentials.Certificate(cred_path)
    firebase_admin.initialize_app(cred)
    db = firestore.client()
    print("Firebase initialized successfully!")
except Exception as e:
    print(f"Firebase initialization error: {e}")
    import traceback
    traceback.print_exc()
    db = None

# ==================== ROOT & HEALTH ====================

@app.route('/', methods=['GET'])
def home():
    """API welcome page"""
    return jsonify({
        'name': 'FRIZZLY API',
        'version': '1.0.0',
        'status': 'running',
        'platform': 'PythonAnywhere',
        'endpoints': {
            'health': '/api/health',
            'orders': '/api/orders',
            'products': '/api/products',
            'users': '/api/users',
            'analytics': '/api/analytics/orders'
        }
    }), 200

@app.route('/api/health', methods=['GET'])
def health_check():
    """API health check"""
    firebase_status = 'connected' if db else 'disconnected'
    
    # Debug info
    debug_info = {
        'current_dir': os.getcwd(),
        'script_dir': os.path.dirname(os.path.abspath(__file__)),
        'files_in_dir': os.listdir(os.path.dirname(os.path.abspath(__file__))) if os.path.dirname(os.path.abspath(__file__)) else []
    }
    
    return jsonify({
        'status': 'healthy',
        'firebase': firebase_status,
        'timestamp': datetime.now().isoformat(),
        'debug': debug_info
    }), 200

# ==================== ORDERS API ====================

@app.route('/api/orders', methods=['GET'])
def get_orders():
    """Get all orders for a user"""
    try:
        if not db:
            return jsonify({'error': 'Database not initialized'}), 500
            
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
        if not db:
            return jsonify({'error': 'Database not initialized'}), 500
            
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
        if not db:
            return jsonify({'error': 'Database not initialized'}), 500
            
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
        if not db:
            return jsonify({'error': 'Database not initialized'}), 500
            
        db.collection('orders').document(order_id).delete()
        return jsonify({'success': True}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ==================== PRODUCTS API ====================

@app.route('/api/products', methods=['GET'])
def get_products():
    """Get all products"""
    try:
        if not db:
            return jsonify({'error': 'Database not initialized'}), 500
            
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
        if not db:
            return jsonify({'error': 'Database not initialized'}), 500
            
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
        if not db:
            return jsonify({'error': 'Database not initialized'}), 500
            
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
        if not db:
            return jsonify({'error': 'Database not initialized'}), 500
            
        db.collection('products').document(product_id).delete()
        return jsonify({'success': True}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# ==================== USERS API ====================

@app.route('/api/users/<user_id>', methods=['GET'])
def get_user(user_id):
    """Get user profile"""
    try:
        if not db:
            return jsonify({'error': 'Database not initialized'}), 500
            
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
        if not db:
            return jsonify({'error': 'Database not initialized'}), 500
            
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
        if not db:
            return jsonify({'error': 'Database not initialized'}), 500
            
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

# For local testing
if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=5000)
