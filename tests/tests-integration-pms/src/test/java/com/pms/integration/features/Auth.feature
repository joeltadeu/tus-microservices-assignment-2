Feature: Authentication

  @Ignore
  Scenario: Login as admin and retrieve access token
    * def auth_url = authHost + config.uri.auth
    Given url auth_url
    And header Content-Type = 'application/json'
    And request { email: '#(config.params.admin.email)', password: '#(config.params.admin.password)' }
    When method POST
    Then status 200
    * def accessToken = response.accessToken