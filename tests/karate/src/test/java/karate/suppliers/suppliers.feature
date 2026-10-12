Feature: Supplier Management REST API Tests

  Background:
    * url suppliersUrl
    * def generateSupplierName = function() { return 'Supplier-' + java.util.UUID.randomUUID().toString().substring(0, 8); }

  Scenario: Create a valid supplier and retrieve by ID
    * def uniqueName = generateSupplierName()
    * def payload =
    """
    {
      "name": '#(uniqueName)',
      "contactPerson": "Alice Johnson",
      "email": "alice@supplier.com",
      "phone": "+1-555-8822",
      "address": "456 Vendor Boulevard",
      "active": true
    }
    """
    Given request payload
    When method post
    Then status 201
    And match response.id == '#number'
    And match response.name == uniqueName
    And match response.email == 'alice@supplier.com'
    * def createdId = response.id

    # Retrieve by ID
    Given path createdId
    When method get
    Then status 200
    And match response.id == createdId
    And match response.name == uniqueName

  Scenario: Validation error on invalid email returns 400 Bad Request
    * def invalidPayload =
    """
    {
      "name": "Invalid Email Vendor",
      "email": "not_an_email"
    }
    """
    Given request invalidPayload
    When method post
    Then status 400
    And match response.status == 400
    And match response.validationErrors.email != null

  Scenario: Validation error on missing company name returns 400 Bad Request
    Given request { name: '' }
    When method post
    Then status 400
    And match response.status == 400
    And match response.validationErrors.name != null

  Scenario: Retrieve nonexistent supplier returns 404 Not Found
    Given path 999999
    When method get
    Then status 404
    And match response.status == 404

  Scenario: List suppliers and filter active
    * def uniqueName = generateSupplierName()
    * def payload =
    """
    {
      "name": '#(uniqueName)',
      "contactPerson": "Test Contact",
      "email": "test@testsupplier.com",
      "active": true
    }
    """
    Given request payload
    When method post
    Then status 201

    # List active suppliers
    Given path 'active'
    When method get
    Then status 200
    And match response == '#array'

    # Search suppliers
    Given param search = uniqueName
    When method get
    Then status 200
    And match response.content[0].name == uniqueName

  Scenario: Update, toggle status, and delete supplier lifecycle
    * def uniqueName = generateSupplierName()
    * def payload =
    """
    {
      "name": '#(uniqueName)',
      "contactPerson": "Initial",
      "active": true
    }
    """
    Given request payload
    When method post
    Then status 201
    * def supplierId = response.id

    # Toggle status to false
    Given path supplierId, 'status'
    And request { active: false }
    When method patch
    Then status 200
    And match response.active == false

    # Update supplier
    * def updatedName = uniqueName + '-Updated'
    * def updatePayload =
    """
    {
      "name": '#(updatedName)',
      "contactPerson": "Updated Contact",
      "active": true
    }
    """
    Given path supplierId
    And request updatePayload
    When method put
    Then status 200
    And match response.name == updatedName

    # Delete supplier
    Given path supplierId
    When method delete
    Then status 204

    # Verify deleted
    Given path supplierId
    When method get
    Then status 404
