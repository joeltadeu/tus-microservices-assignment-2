package com.pms.performance.chain;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import com.pms.performance.config.SimulationConfig;
import io.gatling.javaapi.core.*;

public final class AuthChains {

  private AuthChains() {}

  /**
   * Authenticates as the admin user and saves the Bearer token into the session variable {@code
   * authToken}. All subsequent requests that need authorization should read {@code #{authToken}}
   * from the session.
   */
  public static ChainBuilder loginAsAdmin() {
    String payload = "{ \"email\": \"admin@pms.ie\", \"password\": \"Admin@1234!\" }";

    return exec(http("Admin login")
            .post(SimulationConfig.getAuthUrl() + "/v1/auth/login")
            .header("Content-Type", "application/json")
            .body(StringBody(payload))
            .asJson()
            .check(status().is(200))
            .check(jsonPath("$.accessToken").saveAs("authToken")))
        .exitHereIfFailed()
        .pause(1);
  }
}
