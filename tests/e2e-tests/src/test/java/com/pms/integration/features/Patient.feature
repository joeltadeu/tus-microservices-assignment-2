Feature: Patient - Create, Update, Get by Id, Get All, Delete

  Background:
    * def authResponse = call read('Auth.feature')
    * def authHeader = 'Bearer ' + authResponse.accessToken
    * def patient_url = patientHost + config.uri.patient
    * def RandomData = Java.type('com.pms.integration.util.RandomData')

  @PatientService
  @CreatingPatient
  Scenario: Creating a Patient
    * def patientResponse = call read('PatientSteps.feature@CreatePatient')
    And match patientResponse.response.id == '#notnull'
    * call read('PatientSteps.feature@DeletePatient') ({ id: patientResponse.response.id })

  @PatientService
  @UpdatingPatient
  Scenario: Updating a Patient
    * def patientCreateResponse = call read('PatientSteps.feature@CreatePatient')
    And match patientCreateResponse.response.id == '#notnull'
    * def patientId = patientCreateResponse.response.id
    * call read('PatientSteps.feature@UpdatePatient') ({ id: patientId, firstName: 'Karate', lastName: 'Patient', email: 'karate.patient@gmail.com' })
    * def patientGetResponse = call read('PatientSteps.feature@GetPatientById') ({ id: patientId })
    And match patientGetResponse.response.firstName == 'Karate'
    And match patientGetResponse.response.lastName == 'Patient'
    And match patientGetResponse.response.email == 'karate.patient@gmail.com'
    * call read('PatientSteps.feature@DeletePatient') ({ id: patientId })

  @PatientService
  @GettingPatient
  Scenario: Getting a Patient
    * def patientCreateResponse = call read('PatientSteps.feature@CreatePatient')
    And match patientCreateResponse.response.id == '#notnull'
    * def patientGetResponse = call read('PatientSteps.feature@GetPatientById') ({ id: patientCreateResponse.response.id })
    * call read('PatientSteps.feature@DeletePatient') ({ id: patientGetResponse.response.id })

  @PatientService
  @DeletingPatient
  Scenario: Deleting a Patient
    * def patientCreateResponse = call read('PatientSteps.feature@CreatePatient')
    And match patientCreateResponse.response.id == '#notnull'
    * print patientCreateResponse
    * def patientId = patientCreateResponse.response.id
    * call read('PatientSteps.feature@DeletePatient') ({ id: patientId })
    * def patientGetResponse = call read('PatientSteps.feature@GetPatientByIdNotFound') ({ id: patientId })

  @PatientService
  @GetPatientList
  Scenario: Get Patient List
    * def patientCreateResponse = call read('PatientSteps.feature@CreatePatient')
    And match patientCreateResponse.response.id == '#notnull'
    Given url patient_url
    And header Authorization = authHeader
    When method GET
    Then status 200
    And match each response.content[*] == '#notnull'
    And match each response.content[*] contains deep {id: '#number'}
    And match each response.content[*] contains deep {firstName: '#string'}
    And match each response.content[*] contains deep {lastName: '#string'}
    And match each response.content[*] contains deep {email: '#string'}
    And match each response.content[*] contains deep {address: '#string'}
    And match each response.content[*] contains deep {dateOfBirth: '#string'}
    * call read('PatientSteps.feature@DeletePatient') ({ id: patientCreateResponse.response.id })
