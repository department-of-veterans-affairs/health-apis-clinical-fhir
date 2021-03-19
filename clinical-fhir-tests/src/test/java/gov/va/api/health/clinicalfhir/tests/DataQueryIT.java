package gov.va.api.health.clinicalfhir.tests;

import gov.va.api.health.sentinel.Environment;
import gov.va.api.health.sentinel.ServiceDefinition;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static gov.va.api.health.sentinel.EnvironmentAssumptions.assumeEnvironmentNotIn;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Slf4j
public class DataQueryIT {
    SystemDefinition def = SystemDefinitions.systemDefinition();
    ServiceDefinition r4 = def.getR4();
    String patientId=def.getPublicIds().getPatient();
    String apiPath= r4.apiPath();

    static Stream<Arguments> resourceQueries() {
        var testIds = SystemDefinitions.systemDefinition().getPublicIds();
        return Stream.of(
                arguments("Patient?identifier=" + testIds.getPatient(), 200),
                arguments("Condition?patient=" + testIds.getPatient(), 200),
                arguments("Immunization?patient=" + testIds.getPatient(), 404));
    }
    @ParameterizedTest
    @MethodSource("resourceQueries")
    void routeAppropriateResourceToDataQuery(String query, int expectedStatus) {
        assumeEnvironmentNotIn(Environment.STAGING);
        var apiPath = r4.apiPath();
        var request = apiPath + query;
        log.info("Verify {} has status (200)", request);
        TestClients.internal().get(request).expect(expectedStatus);
    }
}
