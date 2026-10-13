Feature: Product Management REST API Tests

  Background:
    * url productsUrl
    * def generateSku = function() { return 'TEST-' + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase(); }

  Scenario: Create a valid product and retrieve it by ID
    * def uniqueSku = generateSku()
    * def newProduct =
    """
    {
      "name": "Wireless Ergonomic Mouse",
      "sku": '#(uniqueSku)',
      "description": "High precision wireless mouse",
      "price": 49.99,
      "quantity": 100,
      "reorderLevel": 15
    }
    """
    Given request newProduct
    When method post
    Then status 201
    And match response.id == '#number'
    And match response.name == 'Wireless Ergonomic Mouse'
    And match response.sku == uniqueSku
    And match response.price == 49.99
    And match response.quantity == 100
    And match response.reorderLevel == 15
    * def createdId = response.id

    # Retrieve by ID
    Given path createdId
    When method get
    Then status 200
    And match response.id == createdId
    And match response.sku == uniqueSku

  Scenario: Attempt to create product with duplicate SKU returns 409 Conflict
    * def uniqueSku = generateSku()
    * def productPayload =
    """
    {
      "name": "Mechanical Keyboard Blue",
      "sku": '#(uniqueSku)',
      "description": "Clicky switches",
      "price": 89.00,
      "quantity": 30,
      "reorderLevel": 5
    }
    """
    # First create
    Given request productPayload
    When method post
    Then status 201

    # Second create with duplicate SKU
    Given request productPayload
    When method post
    Then status 409
    And match response.status == 409
    And match response.message contains uniqueSku

  Scenario: Validation error when required fields are missing or invalid returns 400 Bad Request
    * def invalidProduct =
    """
    {
      "name": "",
      "sku": "",
      "description": "Negative price and quantity",
      "price": -10.00,
      "quantity": -5,
      "reorderLevel": -1
    }
    """
    Given request invalidProduct
    When method post
    Then status 400
    And match response.status == 400
    And match response.validationErrors.name != null
    And match response.validationErrors.sku != null
    And match response.validationErrors.price != null
    And match response.validationErrors.quantity != null

  Scenario: Retrieve nonexistent product returns 404 Not Found
    Given path 999999
    When method get
    Then status 404
    And match response.status == 404
    And match response.message contains '999999'

  Scenario: List products with pagination and search
    * def uniqueSku = generateSku()
    * def productPayload =
    """
    {
      "name": "Noise Cancelling Headphones",
      "sku": '#(uniqueSku)',
      "description": "Over-ear bluetooth headset",
      "price": 199.99,
      "quantity": 25,
      "reorderLevel": 5
    }
    """
    Given request productPayload
    When method post
    Then status 201

    # List products
    Given param page = 0
    And param size = 10
    When method get
    Then status 200
    And match response.content == '#array'
    And match response.pageNumber == 0
    And match response.pageSize == 10

    # Search products by SKU
    Given param search = uniqueSku
    When method get
    Then status 200
    And match response.content[0].sku == uniqueSku

  Scenario: Update and Delete product lifecycle
    * def uniqueSku = generateSku()
    * def productPayload =
    """
    {
      "name": "USB-C Hub Multiport",
      "sku": '#(uniqueSku)',
      "description": "7 in 1 adapter",
      "price": 35.50,
      "quantity": 80,
      "reorderLevel": 10
    }
    """
    Given request productPayload
    When method post
    Then status 201
    * def productId = response.id

    # Update product
    * def updatePayload =
    """
    {
      "name": "USB-C Hub Multiport Pro",
      "sku": '#(uniqueSku)',
      "description": "Updated 8 in 1 adapter",
      "price": 42.00,
      "quantity": 90,
      "reorderLevel": 12
    }
    """
    Given path productId
    And request updatePayload
    When method put
    Then status 200
    And match response.name == 'USB-C Hub Multiport Pro'
    And match response.price == 42.00
    And match response.quantity == 90

    # Delete product
    Given path productId
    When method delete
    Then status 204

    # Verify product is deleted
    Given path productId
    When method get
    Then status 404
