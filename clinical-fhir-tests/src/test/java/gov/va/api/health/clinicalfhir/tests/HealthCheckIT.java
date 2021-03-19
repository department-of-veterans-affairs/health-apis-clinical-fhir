package gov.va.api.health.clinicalfhir.tests;

import static gov.va.api.health.sentinel.EnvironmentAssumptions.assumeEnvironmentIn;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import gov.va.api.health.sentinel.Environment;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class HealthCheckIT {
  SystemDefinition def = SystemDefinitions.systemDefinition();

  @Test
  void healthCheckIsUnprotected() {
    assumeEnvironmentIn(Environment.LOCAL);
    assumeTrue(def.isChapiAvailable(), "clinical-fhir-nginx-proxy is unavailable.");
    var requestPath = "/clinical-fhir/v0/health";
    log.info("Running health-check for path: {}", requestPath);
    TestClients.internal().get(requestPath).response().then().body("status", equalTo("UP"));
  }
}
