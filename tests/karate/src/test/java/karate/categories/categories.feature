Feature: Category Management REST API Tests

  Background:
    * url categoriesUrl
    * def generateCatName = function() { return 'Cat-' + java.util.UUID.randomUUID().toString().substring(0, 8); }

  Scenario: Create a valid category and retrieve it by ID
    * def uniqueName = generateCatName()
    * def payload =
    """
    {
      "name": '#(uniqueName)',
      "description": "Test category description"
    }
    """
    Given request payload
    When method post
    Then status 201
    And match response.id == '#number'
    And match response.name == uniqueName
    * def createdId = response.id

    # Retrieve by ID
    Given path createdId
    When method get
    Then status 200
    And match response.id == createdId
    And match response.name == uniqueName

  Scenario: Duplicate category name case-insensitively returns 409 Conflict
    * def uniqueName = generateCatName()
    * def payload1 =
    """
    {
      "name": '#(uniqueName)',
      "description": "First"
    }
    """
    Given request payload1
    When method post
    Then status 201

    # Second request with lowercase name
    * def lowerName = uniqueName.toLowerCase()
    * def payload2 =
    """
    {
      "name": '#(lowerName)',
      "description": "Duplicate case insensitive"
    }
    """
    Given request payload2
    When method post
    Then status 409
    And match response.status == 409
    And match response.message contains lowerName

  Scenario: Validation error when name is empty returns 400 Bad Request
    Given request { name: "", description: "Empty name" }
    When method post
    Then status 400
    And match response.status == 400
    And match response.validationErrors.name != null

  Scenario: Retrieve nonexistent category returns 404 Not Found
    Given path 999999
    When method get
    Then status 404
    And match response.status == 404

  Scenario: Update and delete category lifecycle
    * def uniqueName = generateCatName()
    * def payload =
    """
    {
      "name": '#(uniqueName)',
      "description": "Initial"
    }
    """
    Given request payload
    When method post
    Then status 201
    * def categoryId = response.id

    # Update category
    * def updatedName = uniqueName + '-Updated'
    * def updatePayload =
    """
    {
      "name": '#(updatedName)',
      "description": "Updated description"
    }
    """
    Given path categoryId
    And request updatePayload
    When method put
    Then status 200
    And match response.name == updatedName

    # Delete category
    Given path categoryId
    When method delete
    Then status 204

    # Verify deleted
    Given path categoryId
    When method get
    Then status 404
