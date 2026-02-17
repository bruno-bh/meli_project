#!/bin/bash

# Product API - cURL Examples
# Test all endpoints with proper ProductType enum values

BASE_URL="http://localhost:8080/api/v1"

echo "=========================================="
echo "Product API - Testing Examples"
echo "=========================================="

# 1. List all products
echo -e "\n1. GET all products"
echo "curl -X GET $BASE_URL/products"

# 2. Get total count
echo -e "\n2. GET total count"
echo "curl -X GET $BASE_URL/products/stats/count"

# 3. Create iPhone (CELLPHONES type)
echo -e "\n3. POST create iPhone (CELLPHONES)"
echo "curl -X POST $BASE_URL/products \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{"
echo '    "name": "iPhone 15 Pro",'
echo '    "description": "Latest Apple smartphone",'
echo '    "price": 999.99,'
echo '    "type": "CELLPHONES",'
echo '    "color": "Space Black",'
echo '    "size": "6.1 inches",'
echo '    "weight": 0.187,'
echo '    "imageUrl": "https://example.com/iphone15.jpg",'
echo '    "metadata": {'
echo '      "brand": "Apple",'
echo '      "storage_gb": "256",'
echo '      "memory_gb": "8",'
echo '      "screen_size": "6.1",'
echo '      "camera_mp": "48"'
echo '    }'
echo '  }'"
echo "'"

# 4. Create Clothing product
echo -e "\n4. POST create clothing (CLOTHING)"
echo "curl -X POST $BASE_URL/products \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{"
echo '    "name": "Nike Running Shoes",'
echo '    "description": "Professional running shoes",'
echo '    "price": 149.99,'
echo '    "type": "CLOTHING",'
echo '    "color": "Blue",'
echo '    "size": "10",'
echo '    "weight": 0.28,'
echo '    "imageUrl": "https://example.com/shoes.jpg",'
echo '    "metadata": {'
echo '      "size_us": "10",'
echo '      "size_eu": "42",'
echo '      "material": "Mesh + Rubber",'
echo '      "composition": "Synthetic fibers",'
echo '      "color_variations": "3"'
echo '    }'
echo '  }'"
echo "'"

# 5. Create FOOD product
echo -e "\n5. POST create food (FOOD)"
echo "curl -X POST $BASE_URL/products \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{"
echo '    "name": "Organic Coffee Beans",'
echo '    "description": "Premium organic coffee",'
echo '    "price": 25.99,'
echo '    "type": "FOOD",'
echo '    "color": "Brown",'
echo '    "size": "500g",'
echo '    "weight": 0.5,'
echo '    "imageUrl": "https://example.com/coffee.jpg",'
echo '    "metadata": {'
echo '      "origin": "Brazil",'
echo '      "manufacturing_date": "2024-01-15",'
echo '      "expiration_date": "2025-01-15",'
echo '      "nutriscore": "A",'
echo '      "weight": "500g"'
echo '    }'
echo '  }'"
echo "'"

# 6. Create COMPUTER product
echo -e "\n6. POST create computer (COMPUTERS)"
echo "curl -X POST $BASE_URL/products \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{"
echo '    "name": "Dell XPS 15",'
echo '    "description": "Premium laptop",'
echo '    "price": 1999.99,'
echo '    "type": "COMPUTERS",'
echo '    "color": "Silver",'
echo '    "size": "15.6 inches",'
echo '    "weight": 2.0,'
echo '    "imageUrl": "https://example.com/xps.jpg",'
echo '    "metadata": {'
echo '      "brand": "Dell",'
echo '      "processor": "Intel i7",'
echo '      "ram_gb": "16",'
echo '      "storage_gb": "512",'
echo '      "screen_size": "15.6"'
echo '    }'
echo '  }'"
echo "'"

# 7. Search by type (CELLPHONES)
echo -e "\n7. GET search by type (CELLPHONES)"
echo "curl -X GET \"$BASE_URL/products/search?type=CELLPHONES\""

# 8. Search by type (CLOTHING)
echo -e "\n8. GET search by type (CLOTHING)"
echo "curl -X GET \"$BASE_URL/products/search?type=CLOTHING\""

# 9. Search by type (FOOD)
echo -e "\n9. GET search by type (FOOD)"
echo "curl -X GET \"$BASE_URL/products/search?type=FOOD\""

# 10. Search by name
echo -e "\n10. GET search by name"
echo "curl -X GET \"$BASE_URL/products/search?name=iPhone\""

# 11. Search by name and type
echo -e "\n11. GET search by name AND type"
echo "curl -X GET \"$BASE_URL/products/search?name=Coffee&type=FOOD\""

# 12. Get product by ID (replace {id} with actual UUID)
echo -e "\n12. GET product by ID"
echo "curl -X GET \"$BASE_URL/products/{id}\""

# 13. Update product
echo -e "\n13. PUT update product"
echo "curl -X PUT \"$BASE_URL/products/{id}\" \\"
echo "  -H 'Content-Type: application/json' \\"
echo "  -d '{"
echo '    "name": "iPhone 15 Pro Max",'
echo '    "price": 1099.99,'
echo '    "type": "CELLPHONES",'
echo '    "metadata": {'
echo '      "brand": "Apple",'
echo '      "storage_gb": "512",'
echo '      "memory_gb": "8",'
echo '      "screen_size": "6.7",'
echo '      "camera_mp": "48"'
echo '    }'
echo '  }'"
echo "'"

# 14. Delete product
echo -e "\n14. DELETE product"
echo "curl -X DELETE \"$BASE_URL/products/{id}\""

echo -e "\n=========================================="
echo "Available ProductTypes:"
echo "=========================================="
echo "- CELLPHONES     (Smartphones)"
echo "- COMPUTERS      (Computadores)"
echo "- CLOTHING       (Roupas)"
echo "- FOOD           (Alimentos)"
echo "- BEVERAGES      (Bebidas)"
echo "- FURNITURE      (Móveis)"
echo "- BOOKS          (Livros)"
echo "- SPORTS         (Esportes)"

echo -e "\n=========================================="
echo "Example Response:"
echo "=========================================="
cat << 'EOF'
{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "name": "iPhone 15 Pro",
  "description": "Latest Apple smartphone",
  "price": 999.99,
  "type": "CELLPHONES",
  "color": "Space Black",
  "size": "6.1 inches",
  "weight": 0.187,
  "imageUrl": "https://example.com/iphone15.jpg",
  "metadata": {
    "brand": "Apple",
    "storage_gb": "256",
    "memory_gb": "8",
    "screen_size": "6.1",
    "camera_mp": "48"
  }
}
EOF

echo -e "\n=========================================="
echo "To run the API:"
echo "=========================================="
echo "mvn spring-boot:run"
echo ""
echo "Then test with curl commands above"
echo "=========================================="
