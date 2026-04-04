package com.pms.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.karatelabs.core.Runner;
import org.junit.jupiter.api.Test;

/** Test Runner for Doctor Service Executes only Doctor-related test scenarios */
public class DoctorServiceTest {

  @Test
  public void testDoctorService() {
    var results =
        Runner.path("classpath:com/pms/integration/features/Doctor.feature")
            .tags("@DoctorService")
            .outputCucumberJson(true)
            .outputJunitXml(true)
            .parallel(1);

    assertThat(results.getFailedFeatures()).isEmpty();
    assertThat(results.getErrors()).isEmpty();
    assertEquals(0, results.getScenarioFailedCount());
  }
}
