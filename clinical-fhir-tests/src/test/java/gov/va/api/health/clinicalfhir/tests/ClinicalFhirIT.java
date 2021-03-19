package gov.va.api.health.clinicalfhir.tests;

import static gov.va.api.health.sentinel.EnvironmentAssumptions.assumeEnvironmentNotIn;
import static org.hamcrest.CoreMatchers.equalTo;

import gov.va.api.health.sentinel.Environment;
import gov.va.api.health.sentinel.ServiceDefinition;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class ClinicalFhirIT {
  SystemDefinition def = SystemDefinitions.systemDefinition();
  ServiceDefinition r4 = def.getR4();
  String patientId = def.getPublicIds().getPatient();
  String apiPath = r4.apiPath();

  @Test
  void healthCheckIsUnprotected() {
    assumeEnvironmentNotIn(Environment.LOCAL);
    var requestPath = "/clinical-fhir/v0/health";
    log.info("Running health-check for path: {}", requestPath);
    TestClients.internal().get(requestPath).response().then().body("status", equalTo("UP"));
  }

  @Test
  void unsupportedResource() {
    assumeEnvironmentNotIn(Environment.LOCAL);
    var request = apiPath + "Immunization?patient=" + patientId;
    log.info("Verify {} has status (404)", request);
    TestClients.internal().get(request).expect(404);
  }
}
