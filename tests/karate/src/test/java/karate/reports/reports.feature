Feature: Business & Inventory Reports API Integration Tests

  Background:
    * url baseUrl
    * def uniqueId = function() { return java.util.UUID.randomUUID().toString().substring(0, 8); }

  Scenario: Reports Lifecycle - Inventory, Sales, Purchases, Movements, and Valuation
    * def uid = uniqueId()

    # 1. Create Category and Supplier
    Given path '/api/v1/categories'
    And request { "name": '#("Cat-Rep-" + uid)' }
    When method post
    Then status 201
    * def catId = response.id

    Given path '/api/v1/suppliers'
    And request
    """
    {
      "name": '#("Supp-Rep-" + uid)',
      "email": '#("supp_" + uid + "@reports.local")',
      "phone": "+1-555-0199"
    }
    """
    When method post
    Then status 201
    * def suppId = response.id

    # 2. Create Products
    # Product A: Stock 50, reorder 10, price 100.00
    Given path '/api/v1/products'
    And request
    """
    {
      "name": '#("Prod-Rep-A-" + uid)',
      "sku": '#("SKU-RPA-" + uid)',
      "categoryId": '#(catId)',
      "price": 100.00,
      "quantity": 50,
      "reorderLevel": 10
    }
    """
    When method post
    Then status 201
    * def prodAId = response.id

    # Product B: Stock 0, reorder 15, price 60.00 (Out of stock)
    Given path '/api/v1/products'
    And request
    """
    {
      "name": '#("Prod-Rep-B-" + uid)',
      "sku": '#("SKU-RPB-" + uid)',
      "categoryId": '#(catId)',
      "price": 60.00,
      "quantity": 0,
      "reorderLevel": 15
    }
    """
    When method post
    Then status 201
    * def prodBId = response.id

    # 3. Create & Receive a Purchase Order (20 units of Product A @ 50.00 = 1000.00)
    Given path '/api/v1/purchases'
    And request
    """
    {
      "supplierId": '#(suppId)',
      "notes": "Stock replenishment for report test",
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

    # Transition PO from DRAFT -> ORDERED
    Given path '/api/v1/purchases/' + poId + '/submit'
    When method post
    Then status 200

    # Receive PO -> Updates stock and records PURCHASE_RECEIPT transaction in ledger
    Given path '/api/v1/purchases/' + poId + '/receive'
    When method post
    Then status 200

    # 4. Create & Complete a Sales Order (10 units of Product A @ 100.00 = 1000.00)
    Given path '/api/v1/sales'
    And request
    """
    {
      "customerName": "Test Report Customer",
      "customerContact": "customer@reports.local",
      "notes": "Sale order for report test",
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

    # Complete Sales Order -> Deducts stock and records SALE_FULFILLMENT transaction in ledger
    Given path '/api/v1/sales/' + soId + '/complete'
    When method post
    Then status 200

    # 5. Verify Inventory Report
    Given path '/api/v1/reports/inventory'
    And param categoryId = catId
    And param status = 'ALL'
    When method get
    Then status 200
    And assert response.summary.totalProducts >= 2
    And assert response.summary.totalStockUnits >= 60
    And assert response.summary.outOfStockCount >= 1
    And match response.items.content == '#array'
    And assert response.items.content.length >= 2

    # 6. Verify Sales Report
    Given path '/api/v1/reports/sales'
    And param days = 30
    When method get
    Then status 200
    And assert response.summary.completedOrdersCount >= 1
    And assert response.summary.totalUnitsSold >= 10
    And assert response.summary.totalRevenue >= 1000.00
    And match response.topSellingProducts == '#array'
    And assert response.topSellingProducts.length >= 1
    And match response.items.content == '#array'
    And assert response.items.content.length >= 1

    # 7. Verify Purchase Report
    Given path '/api/v1/reports/purchases'
    And param days = 30
    When method get
    Then status 200
    And assert response.summary.totalOrdersCount >= 1
    And assert response.summary.receivedOrdersCount >= 1
    And assert response.summary.receivedUnits >= 20
    And assert response.summary.receivedTotalCost >= 1000.00
    And match response.supplierSummaries == '#array'
    And assert response.supplierSummaries.length >= 1
    And match response.items.content == '#array'
    And assert response.items.content.length >= 1

    # 8. Verify Inventory Movement Report
    Given path '/api/v1/reports/inventory-movements'
    And param productId = prodAId
    And param days = 30
    When method get
    Then status 200
    And assert response.summary.totalMovements >= 2
    And assert response.summary.totalInboundUnits >= 20
    And assert response.summary.totalOutboundUnits >= 10
    And assert response.summary.netQuantityChange >= 10
    And match response.items.content == '#array'
    And assert response.items.content.length >= 2

    # 9. Verify Stock Valuation Report
    Given path '/api/v1/reports/stock-valuation'
    And param categoryId = catId
    When method get
    Then status 200
    And match response.costValuationAvailable == false
    And match response.explanation contains 'Reliable per-unit purchase cost'
    And assert response.summary.totalActiveProducts >= 2
    And assert response.summary.totalPotentialRetailValue >= 6000.00
    And match response.items.content == '#array'
    And assert response.items.content.length >= 2

    # 10. Verify Invalid Date Range Validation
    Given path '/api/v1/reports/sales'
    And param startDate = '2026-12-31'
    And param endDate = '2026-01-01'
    When method get
    Then status 400

  Scenario: RBAC Permissions for Reports
    * def uid = uniqueId()

    # 1. Create a SALES_STAFF user
    Given path '/api/v1/users'
    And request
    """
    {
      "username": '#("sales_" + uid)',
      "email": '#("sales_" + uid + "@reports.local")',
      "password": "Password@123",
      "fullName": "Report Sales Staff",
      "role": "SALES_STAFF"
    }
    """
    When method post
    Then status 201

    # Login as SALES_STAFF
    * configure headers = null
    Given path '/api/v1/auth/login'
    And header X-Client-Type = 'api'
    And request { usernameOrEmail: '#("sales_" + uid)', password: "Password@123" }
    When method post
    Then status 200
    * def salesToken = response.token

    # 2. Create a PURCHASE_MANAGER user
    * eval karate.configure('headers', { Authorization: 'Bearer ' + adminToken })
    Given path '/api/v1/users'
    And request
    """
    {
      "username": '#("purch_" + uid)',
      "email": '#("purch_" + uid + "@reports.local")',
      "password": "Password@123",
      "fullName": "Report Purchase Manager",
      "role": "PURCHASE_MANAGER"
    }
    """
    When method post
    Then status 201

    # Login as PURCHASE_MANAGER
    * configure headers = null
    Given path '/api/v1/auth/login'
    And header X-Client-Type = 'api'
    And request { usernameOrEmail: '#("purch_" + uid)', password: "Password@123" }
    When method post
    Then status 200
    * def purchToken = response.token

    # 3. Test SALES_STAFF permissions
    # Permitted: inventory, sales
    Given path '/api/v1/reports/inventory'
    And header Authorization = 'Bearer ' + salesToken
    When method get
    Then status 200

    Given path '/api/v1/reports/sales'
    And header Authorization = 'Bearer ' + salesToken
    When method get
    Then status 200

    # Forbidden: purchases, movements, valuation
    Given path '/api/v1/reports/purchases'
    And header Authorization = 'Bearer ' + salesToken
    When method get
    Then status 403

    Given path '/api/v1/reports/inventory-movements'
    And header Authorization = 'Bearer ' + salesToken
    When method get
    Then status 403

    Given path '/api/v1/reports/stock-valuation'
    And header Authorization = 'Bearer ' + salesToken
    When method get
    Then status 403

    # 4. Test PURCHASE_MANAGER permissions
    # Permitted: inventory, purchases, movements, valuation
    Given path '/api/v1/reports/inventory'
    And header Authorization = 'Bearer ' + purchToken
    When method get
    Then status 200

    Given path '/api/v1/reports/purchases'
    And header Authorization = 'Bearer ' + purchToken
    When method get
    Then status 200

    Given path '/api/v1/reports/inventory-movements'
    And header Authorization = 'Bearer ' + purchToken
    When method get
    Then status 200

    Given path '/api/v1/reports/stock-valuation'
    And header Authorization = 'Bearer ' + purchToken
    When method get
    Then status 200

    # Forbidden: sales
    Given path '/api/v1/reports/sales'
    And header Authorization = 'Bearer ' + purchToken
    When method get
    Then status 403
