#!/bin/bash

# Script com exemplos de chamadas à API de Produtos
# Use este script para testar os endpoints da API

API_URL="http://localhost:8080/api/v1/products"

echo "=== API de Produtos - Exemplos de Uso ==="

# 1. Criar um novo produto - ELETRÔNICOS
echo ""
echo "1. Criando um novo produto de ELETRÔNICOS..."
RESPONSE1=$(curl -s -X POST $API_URL \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15 Pro",
    "description": "Smartphone de última geração",
    "price": 5999.99,
    "size": "M",
    "weight": 0.187,
    "color": "Titânio",
    "type": "ELETRÔNICOS",
    "metadata": {
      "marca": "Apple",
      "voltagem": "110V",
      "garantia_meses": 12,
      "modelo": "2024"
    }
  }')

echo "$RESPONSE1" | jq '.'
PRODUCT_ID_1=$(echo "$RESPONSE1" | jq -r '.id')
echo "ID do produto criado: $PRODUCT_ID_1"

# 2. Criar um produto de ROUPAS
echo ""
echo "2. Criando um novo produto de ROUPAS..."
RESPONSE2=$(curl -s -X POST $API_URL \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Camiseta Premium",
    "description": "Camiseta de alta qualidade",
    "price": 129.90,
    "size": "M",
    "weight": 0.250,
    "color": "Branco",
    "type": "ROUPAS",
    "metadata": {
      "tamanho_usa": "M",
      "tamanho_eu": "40",
      "material": "100% algodão"
    }
  }')

echo "$RESPONSE2" | jq '.'
PRODUCT_ID_2=$(echo "$RESPONSE2" | jq -r '.id')
echo "ID do produto criado: $PRODUCT_ID_2"

# 3. Criar um produto de ALIMENTOS
echo ""
echo "3. Criando um novo produto de ALIMENTOS..."
RESPONSE3=$(curl -s -X POST $API_URL \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Café Premium 500g",
    "description": "Café torrado e moído",
    "price": 45.90,
    "size": "500g",
    "weight": 0.5,
    "color": "Marrom",
    "type": "ALIMENTOS",
    "metadata": {
      "data_vencimento": "2025-12-31",
      "origem": "Brasil",
      "nutriscore": "A"
    }
  }')

echo "$RESPONSE3" | jq '.'
PRODUCT_ID_3=$(echo "$RESPONSE3" | jq -r '.id')
echo "ID do produto criado: $PRODUCT_ID_3"

# 4. Listar todos os produtos
echo ""
echo "4. Listando todos os produtos..."
curl -s $API_URL | jq '.'

# 5. Obter um produto específico
echo ""
echo "5. Obtendo um produto específico (ID: $PRODUCT_ID_1)..."
curl -s "$API_URL/$PRODUCT_ID_1" | jq '.'

# 6. Atualizar um produto
echo ""
echo "6. Atualizando um produto (ID: $PRODUCT_ID_1)..."
UPDATED=$(curl -s -X PUT "$API_URL/$PRODUCT_ID_1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "iPhone 15 Pro Max",
    "description": "Smartphone com tela maior",
    "price": 6999.99,
    "size": "L",
    "weight": 0.221,
    "color": "Titânio Azul",
    "type": "ELETRÔNICOS",
    "metadata": {
      "marca": "Apple",
      "voltagem": "110V",
      "garantia_meses": 12,
      "modelo": "2024",
      "tela": "6.7 polegadas"
    }
  }')

echo "$UPDATED" | jq '.'

# 7. Contar total de produtos
echo ""
echo "7. Contando total de produtos..."
TOTAL=$(curl -s "$API_URL/stats/count")
echo "Total de produtos: $TOTAL"

# 8. Deletar um produto
echo ""
echo "8. Deletando um produto (ID: $PRODUCT_ID_3)..."
curl -s -X DELETE "$API_URL/$PRODUCT_ID_3" -w "\nStatus: %{http_code}\n"

# 9. Verificar que foi deletado
echo ""
echo "9. Tentando obter produto deletado (deve retornar 404)..."
curl -s "$API_URL/$PRODUCT_ID_3" | jq '.'

# 10. Listar produtos finais
echo ""
echo "10. Listando produtos finais..."
curl -s $API_URL | jq '.'

echo ""
echo "=== Teste concluído! ==="
