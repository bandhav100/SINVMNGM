Feature: User Management and Role-Based Access Control API Tests

  Background:
    * url baseUrl
    * def uniqueId = function() { return java.util.UUID.randomUUID().toString().substring(0, 8); }

  Scenario: Admin can list, create, retrieve, update, and deactivate users
    * def uid = uniqueId()
    * def newStaff =
    """
    {
      "username": '#("staff_" + uid)',
      "email": '#("staff_" + uid + "@sims.local")',
      "fullName": "Staff Member",
      "password": "Password@123",
      "role": "INVENTORY_MANAGER"
    }
    """

    # 1. Create user
    Given path '/api/v1/users'
    And request newStaff
    When method post
    Then status 201
    And match response.username == newStaff.username
    And match response.role == 'INVENTORY_MANAGER'
    And match response.active == true
    * def createdUserId = response.id

    # 2. Get user by ID
    Given path '/api/v1/users', createdUserId
    When method get
    Then status 200
    And match response.id == createdUserId
    And match response.username == newStaff.username

    # 3. List users with search
    Given path '/api/v1/users'
    And param search = uid
    When method get
    Then status 200
    And match response.totalElements == 1
    And match response.content[0].username == newStaff.username

    # 4. Update user role to SALES_STAFF
    Given path '/api/v1/users', createdUserId
    And request { email: '#("staff_" + uid + "@sims.local")', fullName: "Staff Member Updated", role: "SALES_STAFF" }
    When method put
    Then status 200
    And match response.role == 'SALES_STAFF'

    # 5. Toggle status (Deactivate)
    Given path '/api/v1/users', createdUserId, 'status'
    And request { active: false }
    When method patch
    Then status 200
    And match response.active == false

    # 6. Deactivated user cannot log in
    * configure headers = null
    Given path '/api/v1/auth/login'
    And request { usernameOrEmail: '#(newStaff.username)', password: "Password@123" }
    When method post
    Then status 403
    And match response.message contains 'deactivated'

  Scenario: Non-admin staff is forbidden from accessing user management
    * def uid = uniqueId()
    * def staffUsername = 'sales_' + uid
    # 1. Admin creates a sales staff user
    Given path '/api/v1/users'
    And request { username: staffUsername, email: '#(staffUsername + "@sims.local")', fullName: "Sales Guy", password: "Password@123", role: "SALES_STAFF" }
    When method post
    Then status 201

    # 2. Sales staff logs in
    * configure headers = null
    Given path '/api/v1/auth/login'
    And header X-Client-Type = 'api'
    And request { usernameOrEmail: staffUsername, password: "Password@123" }
    When method post
    Then status 200
    * def salesToken = response.token

    # 3. Sales staff attempts to access /api/v1/users -> 403 Forbidden
    * eval karate.configure('headers', { Authorization: 'Bearer ' + salesToken })
    Given path '/api/v1/users'
    When method get
    Then status 403

    # 4. Sales staff attempts to access /api/v1/suppliers -> 403 Forbidden
    Given path '/api/v1/suppliers'
    When method get
    Then status 403

    # 5. Sales staff CAN read products -> 200 OK
    Given path '/api/v1/products'
    When method get
    Then status 200

  Scenario: Last active admin protection blocks self-demotion and self-deactivation
    # 1. Admin attempts to self-demote -> 400 Bad Request
    # Retrieve current admin details
    Given path '/api/v1/auth/me'
    When method get
    Then status 200
    * def adminId = response.id

    Given path '/api/v1/users', adminId
    And request { email: "admin@sims.local", fullName: "System Admin", role: "PURCHASE_MANAGER" }
    When method put
    Then status 400
    And match response.message contains 'cannot change their own role'

    # 2. Admin attempts to self-deactivate -> 400 Bad Request
    Given path '/api/v1/users', adminId, 'status'
    And request { active: false }
    When method patch
    Then status 400
    And match response.message contains 'cannot deactivate their own account'
