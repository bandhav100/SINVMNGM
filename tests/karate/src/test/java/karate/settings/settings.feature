Feature: System & Business Settings API Integration Tests

  Background:
    * url baseUrl
    * def uniqueId = function() { return java.util.UUID.randomUUID().toString().substring(0, 8); }

  Scenario: Get and Update System Settings as ADMIN
    * def uid = uniqueId()

    # 1. Fetch current settings as ADMIN
    Given path '/api/v1/settings'
    When method get
    Then status 200
    And match response.id == 1
    And match response.companyName == '#string'
    And match response.currency == '#string'
    And assert response.defaultReorderThreshold >= 0
    And match response.lowStockAlertsEnabled == '#boolean'

    # 2. Update settings as ADMIN
    Given path '/api/v1/settings'
    And request
    """
    {
      "companyName": '#("Acme Global " + uid)',
      "companyEmail": '#("ops_" + uid + "@acme.local")',
      "companyPhone": "+1 (555) 019-9988",
      "currency": "EUR",
      "timezone": "Europe/Paris",
      "defaultReorderThreshold": 18,
      "lowStockAlertsEnabled": true,
      "outOfStockAlertsEnabled": false,
      "allowNegativeStock": false,
      "requirePurchaseApproval": true
    }
    """
    When method put
    Then status 200
    And match response.companyName == "Acme Global " + uid
    And match response.currency == "EUR"
    And match response.timezone == "Europe/Paris"
    And match response.defaultReorderThreshold == 18
    And match response.outOfStockAlertsEnabled == false
    And match response.requirePurchaseApproval == true

    # 3. Verify persistence with subsequent GET
    Given path '/api/v1/settings'
    When method get
    Then status 200
    And match response.companyName == "Acme Global " + uid
    And match response.currency == "EUR"
    And match response.defaultReorderThreshold == 18

    # 4. Validation: Reject invalid negative threshold
    Given path '/api/v1/settings'
    And request
    """
    {
      "companyName": "Acme Global",
      "companyEmail": "ops@acme.local",
      "companyPhone": "+1 (555) 019-9988",
      "currency": "EUR",
      "timezone": "Europe/Paris",
      "defaultReorderThreshold": -10,
      "lowStockAlertsEnabled": true,
      "outOfStockAlertsEnabled": false,
      "allowNegativeStock": false,
      "requirePurchaseApproval": true
    }
    """
    When method put
    Then status 400

  Scenario: Non-ADMIN User is Forbidden from Settings API
    * def uid = uniqueId()
    * def staffUsername = 'staff_set_' + uid

    # 1. Admin creates a SALES_STAFF user
    Given path '/api/v1/users'
    And request { username: '#(staffUsername)', email: '#(staffUsername + "@sims.local")', fullName: "Settings Staff", password: "Password@123", role: "SALES_STAFF" }
    When method post
    Then status 201

    # 2. Login as SALES_STAFF
    * configure headers = null
    Given path '/api/v1/auth/login'
    And header X-Client-Type = 'api'
    And request { usernameOrEmail: '#(staffUsername)', password: "Password@123" }
    When method post
    Then status 200
    * def staffToken = response.token

    # 3. SALES_STAFF GET is Forbidden
    Given path '/api/v1/settings'
    And header Authorization = 'Bearer ' + staffToken
    When method get
    Then status 403

    # 4. SALES_STAFF PUT is Forbidden
    Given path '/api/v1/settings'
    And header Authorization = 'Bearer ' + staffToken
    And request
    """
    {
      "companyName": "Hacked Corp",
      "companyEmail": "hacked@corp.local",
      "currency": "USD",
      "timezone": "UTC",
      "defaultReorderThreshold": 10,
      "lowStockAlertsEnabled": true,
      "outOfStockAlertsEnabled": true,
      "allowNegativeStock": false,
      "requirePurchaseApproval": false
    }
    """
    When method put
    Then status 403
