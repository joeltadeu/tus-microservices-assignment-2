Feature: Appointment - Create, Update, Get by Id, Get All, Cancel, Delete
  Background:
    * def authResponse = call read('Auth.feature')
    * def authHeader = 'Bearer ' + authResponse.accessToken
    * def patient_url = patientHost + config.uri.patient
    * def appointments_url = appointmentHost + config.uri.appointments

  @AppointmentService
  @CreatingAppointment
  Scenario: Creating an Appointment
    * def doctorResponse = call read('Doctor.feature@CreateDoctor')
    And match doctorResponse.response.id == '#notnull'
    * def doctorId = doctorResponse.response.id
    * def patientResponse = call read('Patient.feature@CreatePatient')
    And match patientResponse.response.id == '#notnull'
    * def patientId = patientResponse.response.id
    * def appointmentResponse = call read('Appointment.feature@CreateAppointment') { doctorId: '#(doctorId)', patientId: '#(patientId)'}
    * def appointmentId = appointmentResponse.response.id
    * call read('Appointment.feature@DeleteAppointment') { patientId: '#(patientId)', id: '#(appointmentId)'}
    * call read('Doctor.feature@DeleteDoctor') { id: '#(doctorId)'}
    * call read('Patient.feature@DeletePatient') { id: '#(patientId)'}

  @AppointmentService
  @UpdatingAppointment
  Scenario: Updating an Appointment
    * def doctorResponse = call read('Doctor.feature@CreateDoctor')
    And match doctorResponse.response.id == '#notnull'
    * def doctorId = doctorResponse.response.id
    * def patientResponse = call read('Patient.feature@CreatePatient')
    And match patientResponse.response.id == '#notnull'
    * def patientId = patientResponse.response.id
    * def appointmentResponse = call read('Appointment.feature@CreateAppointment') { doctorId: '#(doctorId)', patientId: '#(patientId)'}
    * def appointmentId = appointmentResponse.response.id
    * def newStartTime = '2026-06-15T11:00:00'
    Given url patient_url + '/' + patientId + config.uri.appointment + '/' + appointmentId
    And header Authorization = authHeader
    And request { doctorId: '#(doctorId)', startTime: '#(newStartTime)', type: 'CONSULTATION', title: 'Updated Consultation', description: 'Rescheduled appointment for follow-up.' }
    When method PUT
    Then status 200
    And match response.id == appointmentId
    And match response.startTime == '#notnull'
    * call read('Appointment.feature@DeleteAppointment') { patientId: '#(patientId)', id: '#(appointmentId)'}
    * call read('Doctor.feature@DeleteDoctor') { id: '#(doctorId)'}
    * call read('Patient.feature@DeletePatient') { id: '#(patientId)'}

  @AppointmentService
  @GettingAppointment
  Scenario: Getting an Appointment
    * def doctorResponse = call read('Doctor.feature@CreateDoctor')
    And match doctorResponse.response.id == '#notnull'
    * def doctorId = doctorResponse.response.id
    * def patientResponse = call read('Patient.feature@CreatePatient')
    And match patientResponse.response.id == '#notnull'
    * def patientId = patientResponse.response.id
    * def appointmentResponse = call read('Appointment.feature@CreateAppointment') { doctorId: '#(doctorId)', patientId: '#(patientId)'}
    * def appointmentId = appointmentResponse.response.id
    * def appointmentGetResponse = call read('Appointment.feature@GetAppointmentById') { patientId: '#(patientId)', id: '#(appointmentId)' }
    * call read('Appointment.feature@DeleteAppointment') { patientId: '#(patientId)', id: '#(appointmentId)'}
    * call read('Doctor.feature@DeleteDoctor') { id: '#(doctorId)'}
    * call read('Patient.feature@DeletePatient') { id: '#(patientId)'}

  @AppointmentService
  @GetPatientAppointmentList
  Scenario: Get Appointment List for a Patient
    * def doctorResponse = call read('Doctor.feature@CreateDoctor')
    And match doctorResponse.response.id == '#notnull'
    * def doctorId = doctorResponse.response.id
    * def patientResponse = call read('Patient.feature@CreatePatient')
    And match patientResponse.response.id == '#notnull'
    * def patientId = patientResponse.response.id
    * def appointmentResponse = call read('Appointment.feature@CreateAppointment') { doctorId: '#(doctorId)', patientId: '#(patientId)'}
    * def appointmentId = appointmentResponse.response.id
    Given url patient_url + '/' + patientId + config.uri.appointment
    And header Authorization = authHeader
    When method GET
    Then status 200
    And match response.content == '#notnull'
    And match each response.content[*] contains deep {id: '#number'}
    And match each response.content[*] contains deep {startTime: '#string'}
    And match each response.content[*] contains deep {type: '#string'}
    And match each response.content[*] contains deep {status: '#string'}
    * call read('Appointment.feature@DeleteAppointment') { patientId: '#(patientId)', id: '#(appointmentId)'}
    * call read('Doctor.feature@DeleteDoctor') { id: '#(doctorId)'}
    * call read('Patient.feature@DeletePatient') { id: '#(patientId)'}

  @AppointmentService
  @GetAllAppointmentsAdmin
  Scenario: Get All Appointments as Admin
    * def doctorResponse = call read('Doctor.feature@CreateDoctor')
    And match doctorResponse.response.id == '#notnull'
    * def doctorId = doctorResponse.response.id
    * def patientResponse = call read('Patient.feature@CreatePatient')
    And match patientResponse.response.id == '#notnull'
    * def patientId = patientResponse.response.id
    * def appointmentResponse = call read('Appointment.feature@CreateAppointment') { doctorId: '#(doctorId)', patientId: '#(patientId)'}
    * def appointmentId = appointmentResponse.response.id
    Given url appointments_url
    And header Authorization = authHeader
    When method GET
    Then status 200
    And match response.content == '#notnull'
    And match each response.content[*] contains deep {id: '#number'}
    And match each response.content[*] contains deep {startTime: '#string'}
    And match each response.content[*] contains deep {type: '#string'}
    And match each response.content[*] contains deep {status: '#string'}
    * call read('Appointment.feature@DeleteAppointment') { patientId: '#(patientId)', id: '#(appointmentId)'}
    * call read('Doctor.feature@DeleteDoctor') { id: '#(doctorId)'}
    * call read('Patient.feature@DeletePatient') { id: '#(patientId)'}

  @AppointmentService
  @CancellingAppointment
  Scenario: Cancelling a Scheduled Appointment
    * def doctorResponse = call read('Doctor.feature@CreateDoctor')
    And match doctorResponse.response.id == '#notnull'
    * def doctorId = doctorResponse.response.id
    * def patientResponse = call read('Patient.feature@CreatePatient')
    And match patientResponse.response.id == '#notnull'
    * def patientId = patientResponse.response.id
    * def appointmentResponse = call read('Appointment.feature@CreateAppointment') { doctorId: '#(doctorId)', patientId: '#(patientId)'}
    * def appointmentId = appointmentResponse.response.id
    And match appointmentResponse.response.status == 'SCHEDULED'
    Given url patient_url + '/' + patientId + config.uri.appointment + '/' + appointmentId + '/cancel'
    And header Authorization = authHeader
    And request { reason: 'Patient requested cancellation during integration test.' }
    When method POST
    Then status 200
    And match response.status == 'CANCELLED'
    And match response.cancellationReason == '#notnull'

  @AppointmentService
  @DeletingAppointment
  Scenario: Deleting an Appointment
    * def doctorResponse = call read('Doctor.feature@CreateDoctor')
    And match doctorResponse.response.id == '#notnull'
    * def doctorId = doctorResponse.response.id
    * def patientResponse = call read('Patient.feature@CreatePatient')
    And match patientResponse.response.id == '#notnull'
    * def patientId = patientResponse.response.id
    * def appointmentResponse = call read('Appointment.feature@CreateAppointment') { doctorId: '#(doctorId)', patientId: '#(patientId)'}
    * def appointmentId = appointmentResponse.response.id
    * call read('Appointment.feature@DeleteAppointment') { patientId: '#(patientId)', id: '#(appointmentId)'}
    * def appointmentGetResponse = call read('Appointment.feature@GetAppointmentByIdNotFound') { patientId: '#(patientId)', id: '#(appointmentId)' }
    * call read('Doctor.feature@DeleteDoctor') { id: '#(doctorId)'}
    * call read('Patient.feature@DeletePatient') { id: '#(patientId)'}

  # ── Reusable ignored scenarios ─────────────────────────────────────────────

  @CreateAppointment
  @Ignore
  Scenario: Insert Appointment
    * def req = read('data/appointment_request.json')
    * req.startTime = '2026-06-01T10:00:00'
    * req.doctorId = doctorId
    Given url patient_url + '/' + patientId + config.uri.appointment
    And header Authorization = authHeader
    And request req
    When method POST
    Then status 201

  @GetAppointmentByIdNotFound
  @Ignore
  Scenario: Get Appointment By Id Not Found
    Given url patient_url + '/' + patientId + config.uri.appointment + '/' + id
    And header Authorization = authHeader
    When method GET
    Then status 404
    And match response == '#notnull'
    And match response contains deep {code: '#number'}
    And match response contains deep {status: '#string'}
    And match response contains deep {description: '#string'}

  @GetAppointmentById
  @Ignore
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

  @DeleteAppointment
  @Ignore
  Scenario: Delete Appointment
    Given url patient_url + '/' + patientId + config.uri.appointment + '/' + id
    And header Authorization = authHeader
    When method DELETE
    Then status 204