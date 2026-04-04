package com.pms.performance.scenario;

import static io.gatling.javaapi.core.CoreDsl.*;

import com.pms.performance.chain.AppointmentChains;
import com.pms.performance.chain.AuthChains;
import com.pms.performance.chain.DoctorChains;
import com.pms.performance.chain.PatientChains;
import io.gatling.javaapi.core.*;

public final class PmsEndToEndScenario {

  private PmsEndToEndScenario() {}

  public static ScenarioBuilder build() {
    return scenario("PMS End-to-End Scenario")
        // Authenticate once per virtual user — token is reused for all subsequent requests
        .exec(AuthChains.loginAsAdmin())
        // Doctor flow
        .exec(DoctorChains.createDoctor())
        .exec(DoctorChains.getDoctorById())
        .exec(DoctorChains.getAllDoctors())
        // Patient flow
        .exec(PatientChains.createPatient())
        .exec(PatientChains.getPatientById())
        .exec(PatientChains.getAllPatients())
        // Appointment flow — requires patientId and doctorId from previous steps
        .exec(AppointmentChains.createAppointment())
        .exec(AppointmentChains.getAppointmentById());
  }
}
