@ignore
Feature: Setup and Authenticate Admin for Karate Suite

  Scenario:
    Given url baseUrl + '/api/v1/auth/setup-status'
    When method get
    Then status 200
    * def setupNeeded = response.setupRequired

    * eval if (setupNeeded) karate.call('classpath:karate/do-setup.feature', { baseUrl: baseUrl })

    Given url baseUrl + '/api/v1/auth/login'
    And header X-Client-Type = 'api'
    And request { usernameOrEmail: 'admin', password: 'Admin@12345' }
    When method post
    Then status 200
    * def adminToken = response.token
