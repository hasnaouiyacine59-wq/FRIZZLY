#!/bin/bash

# Download free PNG images for FRIZZLY app
cd /home/oo33/AndroidStudioProjects/FRIZZLY/app/src/main/res/drawable-nodpi

echo "Downloading product images..."

# Using free PNG images from pngimg.com (free for commercial use)
curl -L "https://pngimg.com/uploads/apple/apple_PNG12405.png" -o ic_apple.png
curl -L "https://pngimg.com/uploads/banana/banana_PNG825.png" -o ic_banana.png
curl -L "https://pngimg.com/uploads/orange/orange_PNG797.png" -o ic_orange.png
curl -L "https://pngimg.com/uploads/strawberry/strawberry_PNG2598.png" -o ic_strawberry.png
curl -L "https://pngimg.com/uploads/tomato/tomato_PNG12592.png" -o ic_tomato.png
curl -L "https://pngimg.com/uploads/carrot/carrot_PNG4985.png" -o ic_carrot.png
curl -L "https://pngimg.com/uploads/broccoli/broccoli_PNG72.png" -o ic_broccoli.png
curl -L "https://pngimg.com/uploads/lettuce/lettuce_PNG96.png" -o ic_lettuce.png
curl -L "https://pngimg.com/uploads/pepper/pepper_PNG3249.png" -o ic_pepper.png
curl -L "https://pngimg.com/uploads/cucumber/cucumber_PNG84.png" -o ic_cucumber.png

echo "Done! Images downloaded."
ls -lh *.png
