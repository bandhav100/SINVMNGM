Feature: Purchase Management API Integration Tests

  Background:
    * url baseUrl
    * def uniqueId = function() { return java.util.UUID.randomUUID().toString().substring(0, 8); }

  Scenario: Full Purchase Order Lifecycle - Create Draft, Order, Atomic Receive, and Ledger Verification
    * def uid = uniqueId()
    
    # 1. Create a Supplier
    * def supplierPayload =
    """
    {
      "name": '#("Supplier-" + uid)',
      "email": '#("supplier-" + uid + "@test.com")',
      "phone": "+1-555-0199",
      "active": true
    }
    """
    Given path '/api/v1/suppliers'
    And request supplierPayload
    When method post
    Then status 201
    * def supplierId = response.id

    # 2. Create a Category and Product
    * def catPayload = { "name": '#("Cat-" + uid)', "description": "Karate Category" }
    Given path '/api/v1/categories'
    And request catPayload
    When method post
    Then status 201
    * def categoryId = response.id

    * def prodPayload =
    """
    {
      "name": '#("Product-" + uid)',
      "sku": '#("SKU-" + uid)',
      "categoryId": '#(categoryId)',
      "price": 25.00,
      "quantity": 15,
      "reorderLevel": 5
    }
    """
    Given path '/api/v1/products'
    And request prodPayload
    When method post
    Then status 201
    * def productId = response.id
    * def initialStock = response.quantity
    And match initialStock == 15

    # 3. Create Draft Purchase Order
    * def poPayload =
    """
    {
      "supplierId": '#(supplierId)',
      "notes": "Karate test purchase order",
      "items": [
        {
          "productId": '#(productId)',
          "quantity": 20,
          "unitCost": 12.50
        }
      ]
    }
    """
    Given path '/api/v1/purchases'
    And request poPayload
    When method post
    Then status 201
    And match response.status == 'DRAFT'
    And match response.supplierId == supplierId
    And match response.totalAmount == 250.00
    And match response.items[0].totalCost == 250.00
    * def poId = response.id

    # Verify product stock unchanged while in DRAFT
    Given path '/api/v1/products', productId
    When method get
    Then status 200
    And match response.quantity == 15

    # 4. Transition from DRAFT to ORDERED
    Given path '/api/v1/purchases', poId, 'submit'
    When method post
    Then status 200
    And match response.status == 'ORDERED'

    # Verify product stock still unchanged while ORDERED
    Given path '/api/v1/products', productId
    When method get
    Then status 200
    And match response.quantity == 15

    # 5. Receive shipment atomically
    Given path '/api/v1/purchases', poId, 'receive'
    When method post
    Then status 200
    And match response.status == 'RECEIVED'
    And match response.receivedAt != null

    # 6. Verify product stock increased by exactly 20 (15 + 20 = 35)
    Given path '/api/v1/products', productId
    When method get
    Then status 200
    And match response.quantity == 35

    # 7. Duplicate receive attempt must fail with 400 Bad Request
    Given path '/api/v1/purchases', poId, 'receive'
    When method post
    Then status 400

    # Stock must remain unchanged after failed duplicate receive
    Given path '/api/v1/products', productId
    When method get
    Then status 200
    And match response.quantity == 35

    # 8. Verify Purchase Order details after receipt
    Given path '/api/v1/purchases', poId
    When method get
    Then status 200
    And match response.status == 'RECEIVED'
    And match response.receivedAt != null

  Scenario: Purchase Order Cancellation Flow
    * def uid = uniqueId()

    # 1. Create Supplier and Product
    Given path '/api/v1/suppliers'
    And request { "name": '#("Supplier-Cancel-" + uid)', "email": '#("supp-" + uid + "@test.com")', "active": true }
    When method post
    Then status 201
    * def supplierId = response.id

    Given path '/api/v1/categories'
    And request { "name": '#("Cat-Cancel-" + uid)' }
    When method post
    Then status 201
    * def categoryId = response.id

    Given path '/api/v1/products'
    And request { "name": '#("Prod-Cancel-" + uid)', "sku": '#("SKU-C-" + uid)', "categoryId": '#(categoryId)', "price": 20.0, "quantity": 5 }
    When method post
    Then status 201
    * def productId = response.id

    # 2. Create Draft PO and Cancel directly
    * def poPayload =
    """
    {
      "supplierId": '#(supplierId)',
      "items": [{ "productId": '#(productId)', "quantity": 10, "unitCost": 10.00 }]
    }
    """
    Given path '/api/v1/purchases'
    And request poPayload
    When method post
    Then status 201
    * def poId = response.id

    Given path '/api/v1/purchases', poId, 'cancel'
    When method post
    Then status 200
    And match response.status == 'CANCELLED'

    # Cannot transition a cancelled PO to ORDERED
    Given path '/api/v1/purchases', poId, 'submit'
    When method post
    Then status 400

    # Stock was never touched
    Given path '/api/v1/products', productId
    When method get
    Then status 200
    And match response.quantity == 5

  Scenario: RBAC Authorization - SALES_STAFF is forbidden from Purchase Management
    * def uid = uniqueId()

    # 1. Create a user with SALES_STAFF role as Admin
    * def salesUser =
    """
    {
      "username": '#("sales_" + uid)',
      "email": '#("sales_" + uid + "@sims.local")',
      "fullName": "Sales Staff User",
      "password": "Password@123",
      "role": "SALES_STAFF"
    }
    """
    Given path '/api/v1/users'
    And request salesUser
    When method post
    Then status 201

    # 2. Login as SALES_STAFF
    * configure headers = null
    Given path '/api/v1/auth/login'
    And header X-Client-Type = 'api'
    And request { usernameOrEmail: '#(salesUser.username)', password: 'Password@123' }
    When method post
    Then status 200
    * def salesToken = response.token

    # 3. SALES_STAFF attempts GET /api/v1/purchases -> 403 Forbidden
    * configure headers = { Authorization: '#("Bearer " + salesToken)' }
    Given path '/api/v1/purchases'
    When method get
    Then status 403

    # 4. SALES_STAFF attempts POST /api/v1/purchases -> 403 Forbidden
    Given path '/api/v1/purchases'
    And request { "supplierId": 1, "items": [] }
    When method post
    Then status 403
