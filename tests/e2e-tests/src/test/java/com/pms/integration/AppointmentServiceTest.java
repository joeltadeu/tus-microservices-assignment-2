package com.pms.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.karatelabs.core.Runner;
import org.junit.jupiter.api.Test;

/** Test Runner for Appointment Service Executes only Appointment-related test scenarios */
public class AppointmentServiceTest {

  @Test
  public void testAppointmentService() {
    var results =
        Runner.path("classpath:com/pms/integration/features/Appointment.feature")
            .tags("@AppointmentService")
            .outputCucumberJson(true)
            .outputJunitXml(true)
            .parallel(1);

    assertThat(results.getFailedFeatures()).isEmpty();
    assertThat(results.getErrors()).isEmpty();
    assertEquals(0, results.getScenarioFailedCount());
  }
}
