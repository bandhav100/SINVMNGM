Feature: Low-Stock Alerts and Threshold Detection API Integration Tests

  Background:
    * url baseUrl
    * def uniqueId = function() { return java.util.UUID.randomUUID().toString().substring(0, 8); }

  Scenario: Low-Stock Alert Generation, Classification by Severity, and Filtering
    * def uid = uniqueId()

    # 1. Create Category
    Given path '/api/v1/categories'
    And request { "name": '#("Cat-Alert-" + uid)' }
    When method post
    Then status 201
    * def catId = response.id

    # 2. Product 1: Out of Stock (qty 0, reorder 10)
    Given path '/api/v1/products'
    And request
    """
    {
      "name": '#("Prod-OOS-" + uid)',
      "sku": '#("SKU-OOS-" + uid)',
      "categoryId": '#(catId)',
      "price": 25.00,
      "quantity": 0,
      "reorderLevel": 10
    }
    """
    When method post
    Then status 201
    * def oosProdId = response.id

    # 3. Product 2: Low Stock (qty 4, reorder 10)
    Given path '/api/v1/products'
    And request
    """
    {
      "name": '#("Prod-Low-" + uid)',
      "sku": '#("SKU-LOW-" + uid)',
      "categoryId": '#(catId)',
      "price": 35.00,
      "quantity": 4,
      "reorderLevel": 10
    }
    """
    When method post
    Then status 201
    * def lowProdId = response.id

    # 4. Verify Alert Summary Counts
    Given path '/api/v1/alerts/summary'
    When method get
    Then status 200
    And assert response.criticalCount >= 1
    And assert response.warningCount >= 1
    And assert response.totalAlerts >= 2

    # 5. Filter Alerts by CRITICAL Severity
    Given path '/api/v1/alerts'
    And param severity = 'CRITICAL'
    When method get
    Then status 200
    And match response.content == '#array'
    And assert response.content.length >= 1
    * def oosAlert = karate.jsonPath(response.content, "$[?(@.productId == " + oosProdId + ")]")
    And match oosAlert[0].severity == 'CRITICAL'
    And match oosAlert[0].alertType == 'OUT_OF_STOCK'
    And match oosAlert[0].currentStock == 0
    And match oosAlert[0].message contains 'completely out of stock'

    # 6. Filter Alerts by WARNING Severity
    Given path '/api/v1/alerts'
    And param severity = 'WARNING'
    When method get
    Then status 200
    And match response.content == '#array'
    And assert response.content.length >= 1
    * def lowAlert = karate.jsonPath(response.content, "$[?(@.productId == " + lowProdId + ")]")
    And match lowAlert[0].severity == 'WARNING'
    And match lowAlert[0].alertType == 'LOW_STOCK'
    And match lowAlert[0].currentStock == 4
    And match lowAlert[0].suggestedReplenishmentQuantity == 16

    # 7. Low-Stock Convenience Endpoint
    Given path '/api/v1/alerts/low-stock'
    When method get
    Then status 200
    And match response == '#array'
    And assert response.length >= 2
