
import os
from flask import Flask, jsonify
from pymongo import MongoClient
from pymongo.errors import ConnectionFailure

app = Flask(__name__)

# It's a good practice to use environment variables for configuration.
# The docker-compose file will set this variable.
# Defaulting to a standard local MongoDB instance for non-Docker development.
MONGO_URI = os.environ.get('MONGO_URI', 'mongodb://localhost:27017/')

@app.route('/check_status')
def check_status():
    """
    Checks the connection to the MongoDB database.
    """
    try:
        # Initialize the client with a server selection timeout.
        # This will prevent the app from hanging indefinitely if the DB is not available.
        client = MongoClient(MONGO_URI, serverSelectionTimeoutMS=5000)
        
        # The ismaster command is cheap and does not require auth.
        client.admin.command('ismaster')
        
        # If we reach here, the connection is successful.
        return jsonify({"status": "ok"})
    except ConnectionFailure as e:
        # If the connection fails, return an error message.
        return jsonify({"status": "failed", "issue": str(e)}), 500
    except Exception as e:
        # Catch any other potential exceptions during client initialization.
        return jsonify({"status": "failed", "issue": f"An unexpected error occurred: {str(e)}"}), 500

if __name__ == '__main__':
    # Run the app, making it accessible from other containers (0.0.0.0)
    # and using port 5000 by default.
    app.run(host='0.0.0.0', port=5000)
