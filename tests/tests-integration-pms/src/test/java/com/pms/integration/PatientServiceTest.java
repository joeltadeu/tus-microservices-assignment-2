package com.pms.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.junit.jupiter.api.Test;

/** Test Runner for Patient Service Executes only Patient-related test scenarios */
public class PatientServiceTest {

  @Test
  public void testPatientService() {
    Results results =
        Runner.path("classpath:com/pms/integration/features/Patient.feature")
            .tags("@PatientService")
            .outputCucumberJson(true)
            .outputJunitXml(true)
            .parallel(1);

    assertEquals(0, results.getFailCount(), results.getErrorMessages());
  }
}
