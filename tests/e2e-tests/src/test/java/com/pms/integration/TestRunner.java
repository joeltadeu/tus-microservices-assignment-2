package com.pms.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.karatelabs.core.Runner;
import org.junit.jupiter.api.Test;

/**
 * Main Test Runner for Karate Integration Tests This class executes all feature files and generates
 * HTML reports
 */
public class TestRunner {

  @Test
  public void testParallel() {
    // Run all tests in parallel with 5 threads
    // HTML report is automatically generated in target/karate-reports
    var results =
        Runner.path("classpath:com/pms/integration/features")
            .outputCucumberJson(true)
            .outputJunitXml(true)
            .parallel(5);

    assertThat(results.getFailedFeatures()).isEmpty();
    assertThat(results.getErrors()).isEmpty();
    assertEquals(0, results.getScenarioFailedCount());
  }
}
