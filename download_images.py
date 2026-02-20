#!/usr/bin/env python3
"""
Script to download high-quality fruit and vegetable images for Android app.
Uses Unsplash Source API (no API key required for basic usage).
"""

import os
import urllib.request
import urllib.error
from pathlib import Path

# Use a browser User-Agent so image CDNs don't block the request
REQUEST_HEADERS = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; rv:91.0) Gecko/20100101 Firefox/91.0"}

# Base URL for Unsplash Source API (free, no API key needed)
UNSPLASH_SOURCE = "https://source.unsplash.com"

# Product images to download (optional "fallback_url" used if main url fails)
PRODUCTS = {
    "apple": {
        "url": "https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?w=400&h=400&fit=crop",
        "filename": "product_apple.png"
    },
    "banana": {
        "url": "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?w=400&h=400&fit=crop",
        "filename": "product_banana.png"
    },
    "orange": {
        "url": "https://images.unsplash.com/photo-1580052614034-c55d20bfee3b?w=400&h=400&fit=crop",
        "filename": "product_orange.png"
    },
    "strawberry": {
        "url": "https://images.unsplash.com/photo-1464965911861-746a04b4bca6?w=400&h=400&fit=crop",
        "filename": "product_strawberry.png"
    },
    "tomato": {
        "url": "https://images.unsplash.com/photo-1592841200221-a6898f307baa?w=400&h=400&fit=crop",
        "filename": "product_tomato.png"
    },
    "carrot": {
        "url": "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?w=400&h=400&fit=crop",
        "filename": "product_carrot.png"
    },
    "broccoli": {
        "url": "https://images.unsplash.com/photo-1584270354949-c26b0d5b4a0c?w=400&h=400&fit=crop",
        "filename": "product_broccoli.png"
    },
    "lettuce": {
        "url": "https://images.unsplash.com/photo-1622206151226-18ca2c9ab4a1?w=400&h=400&fit=crop",
        "filename": "product_lettuce.png"
    },
    "pepper": {
        "url": "https://images.unsplash.com/photo-1563565375-f3fdfdbefa83?w=400&h=400&fit=crop",
        "filename": "product_pepper.png"
    },
    "cucumber": {
        "url": "https://images.unsplash.com/photo-1607301405793-7e3e7d4fa96d?w=400&h=400&fit=crop",
        "filename": "product_cucumber.png",
        "fallback_url": "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=400&h=400&fit=crop",
        "fallback_url_2": "https://images.unsplash.com/photo-1604977042946-1ee3f42d0e0e?w=400&h=400&fit=crop"
    }
}

def download_image(url: str, filepath: Path) -> bool:
    """Download an image from URL and save to filepath."""
    try:
        print(f"Downloading {filepath.name}...")
        req = urllib.request.Request(url, headers=REQUEST_HEADERS)
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = resp.read()
        filepath.write_bytes(data)
        if len(data) < 1000:
            print(f"✗ {filepath.name}: file too small ({len(data)} bytes), may be HTML error page")
            return False
        print(f"✓ Successfully downloaded {filepath.name} ({len(data)} bytes)")
        return True
    except urllib.error.URLError as e:
        print(f"✗ Error downloading {filepath.name}: {e}")
        return False
    except Exception as e:
        print(f"✗ Unexpected error downloading {filepath.name}: {e}")
        return False

def main():
    # Get the drawable directory
    script_dir = Path(__file__).parent
    drawable_dir = script_dir / "app" / "src" / "main" / "res" / "drawable"
    
    # Create drawable directory if it doesn't exist
    drawable_dir.mkdir(parents=True, exist_ok=True)
    
    print("=" * 60)
    print("Downloading high-quality fruit and vegetable images...")
    print("=" * 60)
    
    success_count = 0
    total_count = len(PRODUCTS)
    
    for product_name, product_info in PRODUCTS.items():
        filepath = drawable_dir / product_info["filename"]
        
        # Skip only if a PNG already exists (so we don't overwrite good images every time)
        if filepath.exists() and filepath.stat().st_size > 1000:
            print(f"⊘ {filepath.name} already exists, skipping...")
            success_count += 1
            continue
        
        urls_to_try = [product_info["url"]]
        for key in ("fallback_url", "fallback_url_2"):
            if key in product_info:
                urls_to_try.append(product_info[key])
        success = False
        for url in urls_to_try:
            if download_image(url, filepath):
                success = True
                break
        if success:
            success_count += 1
    
    print("=" * 60)
    print(f"Download complete: {success_count}/{total_count} images")
    print("=" * 60)
    print(f"\nImages saved to: {drawable_dir}")
    print("\nNext steps:")
    print("1. Update MainActivity.kt to use the new image filenames")
    print("2. Replace R.drawable.ic_* with R.drawable.product_*")
    print("3. Build and run your app!")

if __name__ == "__main__":
    main()
