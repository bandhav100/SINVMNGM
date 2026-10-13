Feature: Sales Management API Integration Tests

  Background:
    * url baseUrl
    * def uniqueId = function() { return java.util.UUID.randomUUID().toString().substring(0, 8); }

  Scenario: Full Sales Order Lifecycle - Create Draft, Update, Complete, Atomic Stock Deduction, and Idempotency
    * def uid = uniqueId()

    # 1. Create Category and Product with initial stock of 25
    * def catPayload = { "name": '#("Cat-Sale-" + uid)' }
    Given path '/api/v1/categories'
    And request catPayload
    When method post
    Then status 201
    * def categoryId = response.id

    * def prodPayload =
    """
    {
      "name": '#("Product-Sale-" + uid)',
      "sku": '#("SKU-S-" + uid)',
      "categoryId": '#(categoryId)',
      "price": 30.00,
      "quantity": 25,
      "reorderLevel": 5
    }
    """
    Given path '/api/v1/products'
    And request prodPayload
    When method post
    Then status 201
    * def productId = response.id
    And match response.quantity == 25

    # 2. Create Draft Sales Order requesting 5 units
    * def salePayload =
    """
    {
      "customerName": "John Doe",
      "customerContact": "+1-555-0188",
      "notes": "Fast shipment requested",
      "items": [
        {
          "productId": '#(productId)',
          "quantity": 5,
          "unitPrice": 30.00
        }
      ]
    }
    """
    Given path '/api/v1/sales'
    And request salePayload
    When method post
    Then status 201
    And match response.status == 'DRAFT'
    And match response.totalAmount == 150.00
    And match response.items[0].lineTotal == 150.00
    * def saleId = response.id

    # Verify product stock unchanged while order is in DRAFT
    Given path '/api/v1/products', productId
    When method get
    Then status 200
    And match response.quantity == 25

    # 3. Update Draft Sales Order to 8 units
    * def updatePayload =
    """
    {
      "customerName": "John Doe Updated",
      "customerContact": "+1-555-0188",
      "items": [
        {
          "productId": '#(productId)',
          "quantity": 8,
          "unitPrice": 30.00
        }
      ]
    }
    """
    Given path '/api/v1/sales', saleId
    And request updatePayload
    When method put
    Then status 200
    And match response.customerName == 'John Doe Updated'
    And match response.totalAmount == 240.00
    And match response.items[0].quantity == 8
    And match response.items[0].lineTotal == 240.00

    # Stock still unchanged
    Given path '/api/v1/products', productId
    When method get
    Then status 200
    And match response.quantity == 25

    # 4. Complete the Sale Order
    Given path '/api/v1/sales', saleId, 'complete'
    When method post
    Then status 200
    And match response.status == 'COMPLETED'
    And match response.completedAt != null

    # 5. Verify product stock is accurately deducted (25 - 8 = 17)
    Given path '/api/v1/products', productId
    When method get
    Then status 200
    And match response.quantity == 17

    # 6. Duplicate completion attempt must fail with 400 Bad Request
    Given path '/api/v1/sales', saleId, 'complete'
    When method post
    Then status 400

    # Stock remains 17 after duplicate attempt
    Given path '/api/v1/products', productId
    When method get
    Then status 200
    And match response.quantity == 17

    # 7. Completed sale cannot be updated or deleted
    Given path '/api/v1/sales', saleId
    And request updatePayload
    When method put
    Then status 400

    Given path '/api/v1/sales', saleId
    When method delete
    Then status 400

  Scenario: Insufficient Stock Rejection
    * def uid = uniqueId()

    # Create category and product with quantity = 4
    Given path '/api/v1/categories'
    And request { "name": '#("Cat-Low-" + uid)' }
    When method post
    Then status 201
    * def categoryId = response.id

    Given path '/api/v1/products'
    And request { "name": '#("Prod-Low-" + uid)', "sku": '#("SKU-LOW-" + uid)', "categoryId": '#(categoryId)', "price": 15.00, "quantity": 4 }
    When method post
    Then status 201
    * def productId = response.id

    # Create draft requesting 10 units (available is only 4)
    * def salePayload =
    """
    {
      "customerName": "Oversell Attempt",
      "items": [{ "productId": '#(productId)', "quantity": 10, "unitPrice": 15.00 }]
    }
    """
    Given path '/api/v1/sales'
    And request salePayload
    When method post
    Then status 201
    * def saleId = response.id

    # Completing the sale must fail with 400 Bad Request
    Given path '/api/v1/sales', saleId, 'complete'
    When method post
    Then status 400
    And match response.message contains 'Insufficient stock'

    # Stock must remain untouched at 4
    Given path '/api/v1/products', productId
    When method get
    Then status 200
    And match response.quantity == 4

  Scenario: Sales Order Cancellation Flow
    * def uid = uniqueId()

    Given path '/api/v1/categories'
    And request { "name": '#("Cat-Can-" + uid)' }
    When method post
    Then status 201
    * def categoryId = response.id

    Given path '/api/v1/products'
    And request { "name": '#("Prod-Can-" + uid)', "sku": '#("SKU-CAN-" + uid)', "categoryId": '#(categoryId)', "price": 20.00, "quantity": 10 }
    When method post
    Then status 201
    * def productId = response.id

    # Create draft sale
    * def salePayload =
    """
    {
      "customerName": "Cancel Test",
      "items": [{ "productId": '#(productId)', "quantity": 3, "unitPrice": 20.00 }]
    }
    """
    Given path '/api/v1/sales'
    And request salePayload
    When method post
    Then status 201
    * def saleId = response.id

    # Cancel the draft order
    Given path '/api/v1/sales', saleId, 'cancel'
    When method post
    Then status 200
    And match response.status == 'CANCELLED'

    # Cannot complete a cancelled order
    Given path '/api/v1/sales', saleId, 'complete'
    When method post
    Then status 400

    # Stock was never touched
    Given path '/api/v1/products', productId
    When method get
    Then status 200
    And match response.quantity == 10

  Scenario: RBAC Authorization - SALES_STAFF can create, PURCHASE_MANAGER is forbidden
    * def uid = uniqueId()

    # 1. Admin creates a user with SALES_STAFF role
    * def salesUser =
    """
    {
      "username": '#("sales_staff_" + uid)',
      "email": '#("sales_staff_" + uid + "@sims.local")',
      "fullName": "Sales Staff Person",
      "password": "Password@123",
      "role": "SALES_STAFF"
    }
    """
    Given path '/api/v1/users'
    And request salesUser
    When method post
    Then status 201

    # 2. Admin creates a user with PURCHASE_MANAGER role
    * def purchaseUser =
    """
    {
      "username": '#("purchaser_" + uid)',
      "email": '#("purchaser_" + uid + "@sims.local")',
      "fullName": "Purchase Manager Person",
      "password": "Password@123",
      "role": "PURCHASE_MANAGER"
    }
    """
    Given path '/api/v1/users'
    And request purchaseUser
    When method post
    Then status 201

    # 3. Create a test product
    Given path '/api/v1/categories'
    And request { "name": '#("Cat-RBAC-" + uid)' }
    When method post
    Then status 201
    * def categoryId = response.id

    Given path '/api/v1/products'
    And request { "name": '#("Prod-RBAC-" + uid)', "sku": '#("SKU-RBAC-" + uid)', "categoryId": '#(categoryId)', "price": 10.00, "quantity": 20 }
    When method post
    Then status 201
    * def productId = response.id

    # 4. Login as SALES_STAFF
    * configure headers = null
    Given path '/api/v1/auth/login'
    And header X-Client-Type = 'api'
    And request { usernameOrEmail: '#(salesUser.username)', password: "Password@123" }
    When method post
    Then status 200
    * def salesToken = response.token

    # 5. SALES_STAFF can create a sales order
    * configure headers = { Authorization: '#("Bearer " + salesToken)' }
    * def salePayload =
    """
    {
      "customerName": "Allowed Customer",
      "items": [{ "productId": '#(productId)', "quantity": 2, "unitPrice": 10.00 }]
    }
    """
    Given path '/api/v1/sales'
    And request salePayload
    When method post
    Then status 201

    # 6. Login as PURCHASE_MANAGER
    * configure headers = null
    Given path '/api/v1/auth/login'
    And header X-Client-Type = 'api'
    And request { usernameOrEmail: '#(purchaseUser.username)', password: "Password@123" }
    When method post
    Then status 200
    * def purchaserToken = response.token

    # 7. PURCHASE_MANAGER can GET sales (200)
    * configure headers = { Authorization: '#("Bearer " + purchaserToken)' }
    Given path '/api/v1/sales'
    When method get
    Then status 200

    # 8. PURCHASE_MANAGER cannot POST sales (403 Forbidden)
    Given path '/api/v1/sales'
    And request salePayload
    When method post
    Then status 403
