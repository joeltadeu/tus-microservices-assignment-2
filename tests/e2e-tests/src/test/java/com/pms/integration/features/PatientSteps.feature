Feature: Patient Step Definitions

  Background:
    * def authResponse = call read('Auth.feature')
    * def authHeader = 'Bearer ' + authResponse.accessToken
    * def patient_url = patientHost + config.uri.patient
    * def RandomData = Java.type('com.pms.integration.util.RandomData')

  @CreatePatient
  @ignore
  Scenario: Insert Patient
    * def req = read('data/patient_request.json')
    * req.firstName = RandomData.randomFirstName()
    * req.lastName = RandomData.randomLastName()
    * req.email = RandomData.randomEmail(req.firstName, req.lastName)
    Given url patient_url
    And header Authorization = authHeader
    And request req
    When method POST
    Then status 201

  @CreatePatientWithParams
  @ignore
  Scenario: Insert Patient With Params
    * def req = read('data/patient_request.json')
    * req.firstName = firstName
    * req.lastName = lastName
    * req.email = email
    Given url patient_url
    And header Authorization = authHeader
    And request req
    When method POST
    Then status 201

  @GetPatientById
  @ignore
  Scenario: Get Patient By Id
    Given url patient_url + '/' + id
    And header Authorization = authHeader
    When method GET
    Then status 200
    And match response == '#notnull'
    * print response
    And match response contains deep {id: '#number'}
    And match response contains deep {firstName: '#string'}
    And match response contains deep {lastName: '#string'}
    And match response contains deep {email: '#string'}
    And match response contains deep {address: '#string'}
    And match response contains deep {dateOfBirth: '#string'}

  @GetPatientByIdNotFound
  @ignore
  Scenario: Get Patient By Id Not Found
    Given url patient_url + '/' + id
    And header Authorization = authHeader
    When method GET
    Then status 404
    And match response == '#notnull'
    And match response contains deep {code: '#number'}
    And match response contains deep {status: '#string'}
    And match response contains deep {description: '#string'}

  @UpdatePatient
  @ignore
  Scenario: Update Patient
    * def req = read('data/patient_request.json')
    * req.firstName = firstName
    * req.lastName = lastName
    * req.email = email
    Given url patient_url + '/' + id
    And header Authorization = authHeader
    And request req
    When method PUT
    Then status 200

  @DeletePatient
  @ignore
  Scenario: Delete Patient
    Given url patient_url + '/' + id
    And header Authorization = authHeader
    When method DELETE
    Then status 204
