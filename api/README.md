# FRIZZLY API - Firebase Python Backend

## Setup Instructions

### 1. Install Python Dependencies
```bash
cd api
pip install -r requirements.txt
```

### 2. Get Firebase Service Account Key

1. Go to Firebase Console: https://console.firebase.google.com
2. Select your FRIZZLY project
3. Go to Project Settings (gear icon) → Service Accounts
4. Click "Generate New Private Key"
5. Save the JSON file as `serviceAccountKey.json` in the `api/` folder

### 3. Run the API
```bash
python app.py
```

The API will run on `http://localhost:5000`

---

## API Endpoints

### Orders

#### Get All Orders for User
```
GET /api/orders?userId={userId}
```

#### Create Order
```
POST /api/orders
Content-Type: application/json

{
  "userId": "user123",
  "orderId": "ORD123456",
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
```

#### Update Order Status
```
PUT /api/orders/{orderId}
Content-Type: application/json

{
  "status": "DELIVERED"
}
```

#### Delete Order
```
DELETE /api/orders/{orderId}
```

---

### Products

#### Get All Products
```
GET /api/products
```

#### Create Product
```
POST /api/products
Content-Type: application/json

{
  "name": "Apple",
  "price": "$2.99/kg",
  "category": "Fruits",
  "imageUrl": "https://example.com/apple.jpg",
  "description": "Fresh red apples",
  "inStock": true
}
```

#### Update Product
```
PUT /api/products/{productId}
Content-Type: application/json

{
  "price": "$3.49/kg",
  "inStock": false
}
```

#### Delete Product
```
DELETE /api/products/{productId}
```

---

### Users

#### Get User Profile
```
GET /api/users/{userId}
```

#### Create User Profile
```
POST /api/users
Content-Type: application/json

{
  "userId": "user123",
  "email": "user@example.com",
  "displayName": "John Doe",
  "phoneNumbers": ["+1234567890", "+0987654321"]
}
```

---

### Analytics

#### Get Order Analytics
```
GET /api/analytics/orders?userId={userId}

Response:
{
  "totalOrders": 25,
  "totalRevenue": 1250.50,
  "statusCounts": {
    "PENDING": 5,
    "DELIVERED": 18,
    "CANCELLED": 2
  }
}
```

---

### Health Check
```
GET /api/health
```

---

## Firebase Firestore Structure

### Collections

#### `orders`
```
{
  "orderId": "ORD123456",
  "userId": "user123",
  "items": [...],
  "totalAmount": 7.48,
  "deliveryLocation": {...},
  "status": "PENDING",
  "timestamp": Timestamp,
  "createdAt": "2026-02-10T00:00:00"
}
```

#### `products`
```
{
  "name": "Apple",
  "price": "$2.99/kg",
  "category": "Fruits",
  "imageUrl": "...",
  "description": "...",
  "inStock": true,
  "createdAt": Timestamp
}
```

#### `users`
```
{
  "userId": "user123",
  "email": "user@example.com",
  "displayName": "John Doe",
  "phoneNumbers": [...],
  "createdAt": Timestamp
}
```

---

## Firebase Free Tier Limits

- **Firestore**: 1 GB storage, 50K reads/day, 20K writes/day
- **Authentication**: Unlimited users
- **Hosting**: 10 GB storage, 360 MB/day transfer

Perfect for development and small-scale production!

---

## Deploy to Production

### Option 1: Google Cloud Run (Recommended)
```bash
gcloud run deploy frizzly-api --source . --region us-central1
```

### Option 2: Heroku
```bash
heroku create frizzly-api
git push heroku main
```

### Option 3: AWS Lambda with Zappa
```bash
pip install zappa
zappa init
zappa deploy production
```

---

## Security Notes

1. **Never commit `serviceAccountKey.json`** - Add to `.gitignore`
2. Add authentication middleware for production
3. Enable Firebase Security Rules in Firestore
4. Use environment variables for sensitive data
