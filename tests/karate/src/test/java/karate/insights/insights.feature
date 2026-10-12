Feature: Smart Inventory Insights API Integration Tests

  Background:
    * url baseUrl
    * def uniqueId = function() { return java.util.UUID.randomUUID().toString().substring(0, 8); }

  Scenario: Inventory Insights Overview, Velocity, Financials, and Purchase-vs-Sales Workflow
    * def uid = uniqueId()

    # 1. Create Category and Products
    Given path '/api/v1/categories'
    And request { "name": '#("Cat-Insight-" + uid)' }
    When method post
    Then status 201
    * def catId = response.id

    # Product 1: Stock 50, reorder 10
    Given path '/api/v1/products'
    And request
    """
    {
      "name": '#("Prod-InStock-" + uid)',
      "sku": '#("SKU-INS-" + uid)',
      "categoryId": '#(catId)',
      "price": 100.00,
      "quantity": 50,
      "reorderLevel": 10
    }
    """
    When method post
    Then status 201
    * def prodAId = response.id

    # Product 2: Out of stock (qty 0, reorder 15)
    Given path '/api/v1/products'
    And request
    """
    {
      "name": '#("Prod-OOS-" + uid)',
      "sku": '#("SKU-OOS-" + uid)',
      "categoryId": '#(catId)',
      "price": 60.00,
      "quantity": 0,
      "reorderLevel": 15
    }
    """
    When method post
    Then status 201
    * def prodBId = response.id

    # 2. Create Supplier & Purchase Order, then receive it
    Given path '/api/v1/suppliers'
    And request { "name": '#("Supp-" + uid)', "email": '#("supp_" + uid + "@sims.local")' }
    When method post
    Then status 201
    * def suppId = response.id

    Given path '/api/v1/purchases'
    And request
    """
    {
      "supplierId": '#(suppId)',
      "items": [
        {
          "productId": '#(prodAId)',
          "quantity": 20,
          "unitCost": 50.00
        }
      ]
    }
    """
    When method post
    Then status 201
    * def poId = response.id

    # Submit purchase order (DRAFT -> ORDERED)
    Given path '/api/v1/purchases', poId, 'submit'
    When method post
    Then status 200
    And match response.status == 'ORDERED'

    # Receive purchase order (ORDERED -> RECEIVED)
    Given path '/api/v1/purchases', poId, 'receive'
    When method post
    Then status 200
    And match response.status == 'RECEIVED'

    # 3. Create Sales Order and complete it (selling 10 units of Product A)
    Given path '/api/v1/sales'
    And request
    """
    {
      "customerName": "Insight Buyer",
      "items": [
        {
          "productId": '#(prodAId)',
          "quantity": 10,
          "unitPrice": 100.00
        }
      ]
    }
    """
    When method post
    Then status 201
    * def soId = response.id

    Given path '/api/v1/sales', soId, 'complete'
    When method post
    Then status 200
    And match response.status == 'COMPLETED'

    # 4. Verify Insights Overview
    Given path '/api/v1/insights/overview'
    When method get
    Then status 200
    And assert response.totalProducts >= 2
    And assert response.outOfStockCount >= 1
    And match response.costValuationAvailable == false
    And match response.valuationExplanation contains 'Reliable unit cost'
    And assert response.totalPotentialRetailValue >= 0.0

    # 5. Verify Fast-Moving Products includes Product A
    Given path '/api/v1/insights/fast-moving-products'
    And param days = 30
    When method get
    Then status 200
    And match response == '#array'
    And assert response.length >= 1
    And assert response[0].unitsSold >= 10
    And assert response[0].totalRevenue >= 1000.00

    # 6. Verify Slow-Moving Products includes Product B as NEVER_SOLD
    Given path '/api/v1/insights/slow-moving-products'
    And param days = 30
    When method get
    Then status 200
    And match response == '#array'
    And assert response.length >= 1
    * def neverSoldItems = karate.jsonPath(response, "$[?(@.classification == 'NEVER_SOLD')]")
    And match neverSoldItems == '#array'
    And assert neverSoldItems.length >= 1

    # 7. Verify Purchase vs Sales Comparison
    Given path '/api/v1/insights/purchase-vs-sales'
    And param days = 30
    When method get
    Then status 200
    And assert response.purchasedUnits >= 20
    And assert response.purchaseCostTotal >= 1000.00
    And assert response.soldUnits >= 10
    And assert response.salesRevenueTotal >= 1000.00
    And match response.netUnitsDelta != null

    # 8. Verify Reorder Recommendations recommends replenishment for Product B
    Given path '/api/v1/insights/reorder-recommendations'
    When method get
    Then status 200
    And match response == '#array'
    And assert response.length >= 1
    * def oosRec = karate.jsonPath(response, "$[?(@.productId == " + prodBId + ")]")
    And assert oosRec[0].suggestedReplenishmentQuantity >= 15
    And match oosRec[0].recommendationType == 'POLICY_SAFETY_STOCK_MINIMUM'

    # 9. Verify Stock Valuation reports reliable cost as unavailable
    Given path '/api/v1/insights/stock-valuation'
    When method get
    Then status 200
    And match response.costValuationAvailable == false
    And match response.explanation contains 'Reliable unit cost'
    And match response.products.content == '#array'
    And assert response.products.content.length >= 1

    # 10. Verify Invalid Date Range is rejected with 400 Bad Request
    Given path '/api/v1/insights/purchase-vs-sales'
    And param startDate = '2026-12-31'
    And param endDate = '2026-01-01'
    When method get
    Then status 400

  Scenario: RBAC Permissions for Insights and Stock Valuation
    * def uid = uniqueId()
    * def staffUsername = 'staff_ins_' + uid

    # Admin creates SALES_STAFF
    Given path '/api/v1/users'
    And request { username: '#(staffUsername)', email: '#(staffUsername + "@sims.local")', fullName: "Sales Staff", password: "Password@123", role: "SALES_STAFF" }
    When method post
    Then status 201

    # Login as SALES_STAFF
    * configure headers = null
    Given path '/api/v1/auth/login'
    And header X-Client-Type = 'api'
    And request { usernameOrEmail: '#(staffUsername)', password: "Password@123" }
    When method post
    Then status 200
    * def salesToken = response.token

    # SALES_STAFF can access overview, fast-moving, and alerts
    * eval karate.configure('headers', { Authorization: 'Bearer ' + salesToken })
    Given path '/api/v1/insights/overview'
    When method get
    Then status 200

    Given path '/api/v1/insights/fast-moving-products'
    When method get
    Then status 200

    Given path '/api/v1/alerts'
    When method get
    Then status 200

    # SALES_STAFF is forbidden from stock-valuation and reorder-recommendations
    Given path '/api/v1/insights/stock-valuation'
    When method get
    Then status 403

    Given path '/api/v1/insights/reorder-recommendations'
    When method get
    Then status 403
