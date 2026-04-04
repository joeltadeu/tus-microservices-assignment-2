Feature: Doctor - Create, Update, Get by Id, Get All, Delete

  Background:
    * def authResponse = call read('Auth.feature')
    * def authHeader = 'Bearer ' + authResponse.accessToken
    * def doctor_url = doctorHost + config.uri.doctor
    * def RandomData = Java.type('com.pms.integration.util.RandomData')

  @DoctorService
  @CreatingDoctor
  Scenario: Creating a Doctor
    * def doctorResponse = call read('DoctorSteps.feature@CreateDoctor')
    And match doctorResponse.response.id == '#notnull'
    * call read('DoctorSteps.feature@DeleteDoctor') ({ id: doctorResponse.response.id })

  @DoctorService
  @UpdatingDoctor
  Scenario: Updating a Doctor
    * def doctorCreateResponse = call read('DoctorSteps.feature@CreateDoctor')
    And match doctorCreateResponse.response.id == '#notnull'
    * def doctorId = doctorCreateResponse.response.id
    * print doctorId
    * call read('DoctorSteps.feature@UpdateDoctor') ({ id: doctorId, firstName: 'Karate', lastName: 'Doctor', email: 'karate.doctor@medicalclinic.com' })
    * def doctorGetResponse = call read('DoctorSteps.feature@GetDoctorById') ({ id: doctorId })
    And match doctorGetResponse.response.firstName == 'Karate'
    And match doctorGetResponse.response.lastName == 'Doctor'
    And match doctorGetResponse.response.email == 'karate.doctor@medicalclinic.com'
    * call read('DoctorSteps.feature@DeleteDoctor') ({ id: doctorId })

  @DoctorService
  @GettingDoctor
  Scenario: Getting a Doctor
    * def doctorCreateResponse = call read('DoctorSteps.feature@CreateDoctor')
    And match doctorCreateResponse.response.id == '#notnull'
    * def doctorGetResponse = call read('DoctorSteps.feature@GetDoctorById') ({ id: doctorCreateResponse.response.id })
    * call read('DoctorSteps.feature@DeleteDoctor') ({ id: doctorGetResponse.response.id })

  @DoctorService
  @DeletingDoctor
  Scenario: Deleting a Doctor
    * def doctorCreateResponse = call read('DoctorSteps.feature@CreateDoctor')
    And match doctorCreateResponse.response.id == '#notnull'
    * print doctorCreateResponse
    * def doctorId = doctorCreateResponse.response.id
    * call read('DoctorSteps.feature@DeleteDoctor') ({ id: doctorId })
    * def doctorGetResponse = call read('DoctorSteps.feature@GetDoctorByIdNotFound') ({ id: doctorId })

  @DoctorService
  @GetDoctorList
  Scenario: Get Doctor List
    * def doctorCreateResponse = call read('DoctorSteps.feature@CreateDoctor')
    And match doctorCreateResponse.response.id == '#notnull'
    Given url doctor_url
    And header Authorization = authHeader
    When method GET
    Then status 200
    And match each response.content[*] == '#notnull'
    And match each response.content[*] contains deep {id: '#number'}
    And match each response.content[*] contains deep {firstName: '#string'}
    And match each response.content[*] contains deep {lastName: '#string'}
    And match each response.content[*] contains deep {title: '#string'}
    And match each response.content[*] contains deep {speciality: '#string'}
    And match each response.content[*] contains deep {email: '#string'}
    And match each response.content[*] contains deep {phone: '#string'}
    And match each response.content[*] contains deep {department: '#string'}
    * call read('DoctorSteps.feature@DeleteDoctor') ({ id: doctorCreateResponse.response.id })
