Feature: Dashboard Integration API Tests

  Background:
    * url dashboardUrl

  Scenario: Admin retrieves full dashboard summary with KPIs and activity feed
    Given path 'summary'
    When method get
    Then status 200
    And match response.totalProducts == '#number'
    And match response.totalStockUnits == '#number'
    And match response.lowStockCount == '#number'
    And match response.outOfStockCount == '#number'
    And match response.completedSalesCount == '#number'
    And match response.completedSalesRevenue == '#number'
    And match response.purchaseMetricsVisible == true
    And match response.totalPurchasesCount == '#number'
    And match response.receivedPurchasesCount == '#number'
    And match response.criticalAlerts == '#[]'
    And match response.recentActivity == '#[]'

  Scenario: Sales staff retrieves dashboard summary with purchase metrics masked
    * def unique = java.util.UUID.randomUUID().toString().substring(0, 8)
    * def staffUsername = 'dash_staff_' + unique
    * def createUserPayload =
      """
      {
        "username": '#(staffUsername)',
        "email": '#(staffUsername + "@example.com")',
        "fullName": "Dashboard Staff",
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

    * url dashboardUrl + '/summary'
    * configure headers = { Authorization: '#("Bearer " + staffToken)' }
    * method get
    * status 200
    * match response.totalProducts == '#number'
    * match response.purchaseMetricsVisible == false
    * match response.totalPurchasesCount == 0
    * match response.receivedPurchasesTotalCost == 0

  Scenario: Unauthenticated request returns 401 Unauthorized
    Given path 'summary'
    * configure headers = null
    When method get
    Then status 401
