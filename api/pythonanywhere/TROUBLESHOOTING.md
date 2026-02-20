# Fix "Database not initialized" Error

## Quick Fix Steps:

### 1. Check if serviceAccountKey.json is uploaded
```bash
# In PythonAnywhere Bash console:
cd ~/frizzly-api
ls -la
# You should see: serviceAccountKey.json
```

### 2. If file is missing:
1. Go to Firebase Console: https://console.firebase.google.com
2. Select your FRIZZLY project
3. Go to **Project Settings** (gear icon) → **Service Accounts**
4. Click **Generate New Private Key**
5. Download the JSON file
6. Upload to PythonAnywhere: `/home/YOUR_USERNAME/frizzly-api/serviceAccountKey.json`

### 3. Update flask_app.py path:
Edit line 15 in `flask_app.py`:
```python
'/home/YOUR_USERNAME/frizzly-api/serviceAccountKey.json',
```
Replace `YOUR_USERNAME` with your actual PythonAnywhere username!

### 4. Check Error Log:
1. Go to **Web** tab in PythonAnywhere
2. Click **Error log** link
3. Look for the actual error message

### 5. Reload:
Click green **Reload** button in Web tab

### 6. Test:
Visit: `https://YOUR_USERNAME.pythonanywhere.com/api/health`

Should show:
```json
{
  "firebase": "connected",
  "status": "healthy"
}
```

---

## Common Issues:

### Issue 1: Wrong file path
**Solution:** Check the debug info at `/api/health` to see current directory

### Issue 2: Invalid JSON file
**Solution:** Re-download serviceAccountKey.json from Firebase

### Issue 3: Firebase Admin not installed
**Solution:**
```bash
pip3.8 install --user --force-reinstall firebase-admin
```

### Issue 4: Permission error
**Solution:** Make sure file is in your home directory:
```bash
chmod 644 ~/frizzly-api/serviceAccountKey.json
```

---

## Still not working?

Check the error log and send me the error message!
