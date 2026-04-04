Feature: Doctor Step Definitions

  Background:
    * def authResponse = call read('Auth.feature')
    * def authHeader = 'Bearer ' + authResponse.accessToken
    * def doctor_url = doctorHost + config.uri.doctor
    * def RandomData = Java.type('com.pms.integration.util.RandomData')

  @CreateDoctor
  @ignore
  Scenario: Insert Doctor
    * def req = read('data/doctor_request.json')
    * req.firstName = RandomData.randomFirstName()
    * req.lastName = RandomData.randomLastName()
    * req.email = RandomData.randomEmail(req.firstName, req.lastName)
    * print req.email
    Given url doctor_url
    And header Authorization = authHeader
    And request req
    When method POST
    Then status 201

  @CreateDoctorWithParams
  @ignore
  Scenario: Insert Doctor With Params
    * def req = read('data/doctor_request.json')
    * req.firstName = firstName
    * req.lastName = lastName
    * req.email = email
    Given url doctor_url
    And header Authorization = authHeader
    And request req
    When method POST
    Then status 201

  @GetDoctorById
  @ignore
  Scenario: Get Doctor By Id
    Given url doctor_url + '/' + id
    And header Authorization = authHeader
    When method GET
    Then status 200
    And match response == '#notnull'
    And match response contains deep {id: '#number'}
    And match response contains deep {firstName: '#string'}
    And match response contains deep {lastName: '#string'}
    And match response contains deep {speciality: '#string'}
    And match response contains deep {email: '#string'}
    And match response contains deep {title: '#string'}
    And match response contains deep {phone: '#string'}
    And match response contains deep {department: '#string'}

  @GetDoctorByIdNotFound
  @ignore
  Scenario: Get Doctor By Id Not Found
    Given url doctor_url + '/' + id
    And header Authorization = authHeader
    When method GET
    Then status 404
    And match response == '#notnull'
    And match response contains deep {code: '#number'}
    And match response contains deep {status: '#string'}
    And match response contains deep {description: '#string'}

  @UpdateDoctor
  @ignore
  Scenario: Update Doctor
    * def req = read('data/doctor_request.json')
    * req.firstName = firstName
    * req.lastName = lastName
    * req.email = email
    * print req
    * print doctor_url + '/' + id
    Given url doctor_url + '/' + id
    And header Authorization = authHeader
    And request req
    When method PUT
    Then status 200

  @DeleteDoctor
  @ignore
  Scenario: Delete Doctor
    Given url doctor_url + '/' + id
    And header Authorization = authHeader
    When method DELETE
    Then status 204
