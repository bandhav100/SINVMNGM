Feature: Complete Inventory Management API Tests

  Background:
    * url inventoryUrl

  Scenario: Retrieve inventory summary and stock levels
    Given path 'summary'
    When method get
    Then status 200
    And match response.totalProducts == '#number'
    And match response.totalStockUnits == '#number'
    And match response.totalRetailValue == '#number'

    Given path 'stock'
    When method get
    Then status 200
    And match response.content == '#[]'

  Scenario: Perform authorized stock adjustment and verify ledger update
    * def unique = java.util.UUID.randomUUID().toString().substring(0, 8)
    # Create product for adjustment test
    * url productsUrl
    * def createProductPayload =
      """
      {
        "name": '#("InvProd-" + unique)',
        "sku": '#("SKU-INV-" + unique)',
        "price": 45.00,
        "quantity": 10,
        "reorderLevel": 5
      }
      """
    * request createProductPayload
    * method post
    * status 201
    * def testProdId = response.id

    # 1. Perform adjustment (+8)
    * url inventoryUrl + '/adjustments'
    * def adjustPayload =
      """
      {
        "productId": '#(testProdId)',
        "quantityDelta": 8,
        "reason": "Cycle Count Correction",
        "notes": "Discovered in back aisle"
      }
      """
    * request adjustPayload
    * method post
    * status 201
    * match response.quantityDelta == 8
    * match response.quantityBefore == 10
    * match response.quantityAfter == 18
    * match response.reason == "Cycle Count Correction"

    # 2. Check updated stock
    * url inventoryUrl + '/stock'
    * param search = 'SKU-INV-' + unique
    * method get
    * status 200
    * match response.content[0].quantity == 18

    # 3. Check transaction ledger contains adjustment
    * url inventoryUrl + '/transactions'
    * param productId = testProdId
    * method get
    * status 200
    * match response.content[0].transactionType == 'MANUAL_ADJUSTMENT'
    * match response.content[0].quantityDelta == 8

  Scenario: Prevent negative stock adjustment
    * def unique = java.util.UUID.randomUUID().toString().substring(0, 8)
    * url productsUrl
    * def createProductPayload =
      """
      {
        "name": '#("NegProd-" + unique)',
        "sku": '#("SKU-NEG-" + unique)',
        "price": 20.00,
        "quantity": 5,
        "reorderLevel": 2
      }
      """
    * request createProductPayload
    * method post
    * status 201
    * def negProdId = response.id

    * url inventoryUrl + '/adjustments'
    * def negAdjustPayload =
      """
      {
        "productId": '#(negProdId)',
        "quantityDelta": -20,
        "reason": "Attempting negative adjustment",
        "notes": "Should be rejected"
      }
      """
    * request negAdjustPayload
    * method post
    * status 400

  Scenario: Non-authorized role (SALES_STAFF) is rejected from adjusting stock
    * def unique = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def staffUsername = 'inv_staff_' + unique
    * def createUserPayload =
      """
      {
        "username": '#(staffUsername)',
        "email": '#(staffUsername + "@example.com")',
        "fullName": "Inventory Staff",
        "role": "SALES_STAFF",
        "password": "StaffPassword@123"
      }
      """
    * url usersUrl
    * request createUserPayload
    * method post
    * status 201

    * url authUrl + '/login'
    * configure headers = null
    * header X-Client-Type = 'api'
    * request { usernameOrEmail: '#(staffUsername)', password: 'StaffPassword@123' }
    * method post
    * status 200
    * def staffToken = response.token

    * url inventoryUrl + '/adjustments'
    * configure headers = { Authorization: '#("Bearer " + staffToken)' }
    * request { productId: 1, quantityDelta: 5, reason: "Unauthorized attempt" }
    * method post
    * status 403
