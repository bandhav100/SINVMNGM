Feature: Authentication, Session Management, and Role Restrictions API Tests

  Background:
    * url baseUrl
    * def uniqueId = function() { return java.util.UUID.randomUUID().toString().substring(0, 8); }

  Scenario: Setup status check and repeat setup rejection
    Given path '/api/v1/auth/setup-status'
    When method get
    Then status 200
    And match response.setupRequired == '#boolean'

    # Once initialized, repeated setup must return 409 Conflict
    Given path '/api/v1/auth/setup-admin'
    And request { username: 'intruder', email: 'intruder@sims.local', fullName: 'Intruder', password: 'Password@123' }
    When method post
    Then status 409
    And match response.error == 'Conflict'

  Scenario: User login with valid credentials returns 200 and issues token
    Given path '/api/v1/auth/login'
    And header X-Client-Type = 'api'
    And request { usernameOrEmail: 'admin', password: 'Admin@12345' }
    When method post
    Then status 200
    And match response.token == '#string'
    And match response.user.username == 'admin'
    And match response.user.role == 'ADMIN'

  Scenario: User login with invalid credentials returns 401 Unauthorized
    * configure headers = null
    Given path '/api/v1/auth/login'
    And request { usernameOrEmail: 'admin', password: 'WrongPassword!@#' }
    When method post
    Then status 401
    And match response.status == 401
    And match response.error == 'Unauthorized'

  Scenario: Authenticated user profile retrieval via /api/v1/auth/me
    Given path '/api/v1/auth/me'
    When method get
    Then status 200
    And match response.username == 'admin'
    And match response.role == 'ADMIN'

  Scenario: Invalid and malformed Bearer tokens are rejected with 401
    * configure headers = { Authorization: 'Bearer this.is.an.invalid.token' }
    Given path '/api/v1/auth/me'
    When method get
    Then status 401
    And match response.status == 401

  Scenario: User logout revokes the token and invalidates subsequent requests
    # 1. Login to get fresh token
    * configure headers = null
    Given path '/api/v1/auth/login'
    And header X-Client-Type = 'api'
    And request { usernameOrEmail: 'admin', password: 'Admin@12345' }
    When method post
    Then status 200
    * def ephemeralToken = response.token

    # 2. Logout with token
    * eval karate.configure('headers', { Authorization: 'Bearer ' + ephemeralToken })
    Given path '/api/v1/auth/logout'
    When method post
    Then status 200
    And match response.message == 'Logged out successfully'

    # 3. Attempt to use revoked token -> 401 Unauthorized
    * eval karate.configure('headers', { Authorization: 'Bearer ' + ephemeralToken })
    Given path '/api/v1/auth/me'
    When method get
    Then status 401
