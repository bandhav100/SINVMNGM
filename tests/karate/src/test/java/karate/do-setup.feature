@ignore
Feature: Do Initial Admin Setup

  Scenario:
    Given url baseUrl + '/api/v1/auth/setup-admin'
    And header X-Client-Type = 'api'
    And request { username: 'admin', email: 'admin@sims.local', fullName: 'System Admin', password: 'Admin@12345' }
    When method post
    Then status 201
