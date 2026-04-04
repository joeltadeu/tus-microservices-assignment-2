package com.pms.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.karatelabs.core.Runner;
import org.junit.jupiter.api.Test;

/** Test Runner for Patient Service Executes only Patient-related test scenarios */
public class PatientServiceTest {

  @Test
  public void testPatientService() {
    var results =
        Runner.path("classpath:com/pms/integration/features/Patient.feature")
            .tags("@PatientService")
            .outputCucumberJson(true)
            .outputJunitXml(true)
            .parallel(1);

    assertThat(results.getFailedFeatures()).isEmpty();
    assertThat(results.getErrors()).isEmpty();
    assertEquals(0, results.getScenarioFailedCount());
  }
}
