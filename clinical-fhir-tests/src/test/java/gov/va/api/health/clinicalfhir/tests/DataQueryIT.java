package gov.va.api.health.clinicalfhir.tests;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import gov.va.api.health.sentinel.ServiceDefinition;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Slf4j
public class DataQueryIT {
  SystemDefinition def = SystemDefinitions.systemDefinition();
  ServiceDefinition r4 = def.r4();
  String apiPath = r4.apiPath();

  static Stream<Arguments> resourceQueries() {
    var testIds = SystemDefinitions.systemDefinition().publicIds();
    return Stream.of(
        arguments("Condition?patient=" + testIds.patient(), 200),
        arguments("Patient/" + testIds.patient(), 200),
        arguments("Practitioner/" + testIds.practitioner(), 200));
  }

  @ParameterizedTest
  @MethodSource("resourceQueries")
  void routeAppropriateResourceToDataQuery(String query, int expectedStatus) {
    assumeTrue(def.isDqAvailable(), "data-query is unavailable.");
    var request = apiPath + query;
    log.info("Verify {} has status (200)", request);
    TestClients.internal().get(request).expect(expectedStatus);
  }
}
