Feature: Appointment Step Definitions

  Background:
    * def authResponse = call read('Auth.feature')
    * def authHeader = 'Bearer ' + authResponse.accessToken
    * def patient_url = patientHost + config.uri.patient
    * def appointments_url = appointmentHost + config.uri.appointments

  @CreateAppointment
  @ignore
  Scenario: Insert Appointment
    * def req = read('data/appointment_request.json')
    * req.startTime = '2026-06-01T10:00:00'
    * req.doctorId = doctorId
    Given url patient_url + '/' + patientId + config.uri.appointment
    And header Authorization = authHeader
    And request req
    When method POST
    Then status 201

  @GetAppointmentById
  @ignore
  Scenario: Get Appointment By Id
    Given url patient_url + '/' + patientId + config.uri.appointment + '/' + id
    And header Authorization = authHeader
    When method GET
    Then status 200
    And match response == '#notnull'
    And match response contains deep {patient: {id: '#number', firstName: '#string', lastName: '#string', email: '#string'}}
    And match response contains deep {doctor: {id: '#number', firstName: '#string', lastName: '#string', title: '#string', speciality: '#string'}}
    And match response contains deep {id: '#number'}
    And match response contains deep {startTime: '#string'}
    And match response contains deep {endTime: '#string'}
    And match response contains deep {duration: '#number'}
    And match response contains deep {description: '#string'}
    And match response contains deep {type: '#string'}
    And match response contains deep {status: '#string'}

  @GetAppointmentByIdNotFound
  @ignore
  Scenario: Get Appointment By Id Not Found
    Given url patient_url + '/' + patientId + config.uri.appointment + '/' + id
    And header Authorization = authHeader
    When method GET
    Then status 404
    And match response == '#notnull'
    And match response contains deep {code: '#number'}
    And match response contains deep {status: '#string'}
    And match response contains deep {description: '#string'}

  @DeleteAppointment
  @ignore
  Scenario: Delete Appointment
    Given url patient_url + '/' + patientId + config.uri.appointment + '/' + id
    And header Authorization = authHeader
    When method DELETE
    Then status 204
