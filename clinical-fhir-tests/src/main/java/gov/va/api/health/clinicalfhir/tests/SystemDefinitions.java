package gov.va.api.health.clinicalfhir.tests;

import static gov.va.api.health.sentinel.SentinelProperties.magicAccessToken;

import gov.va.api.health.sentinel.Environment;
import gov.va.api.health.sentinel.SentinelProperties;
import gov.va.api.health.sentinel.ServiceDefinition;
import java.util.Locale;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.BooleanUtils;

@UtilityClass
public final class SystemDefinitions {
  private static SystemDefinition lab() {
    String url = "https://blue.lab.lighthouse.va.gov";
    return SystemDefinition.builder()
        .internal(serviceDefinition("internal", url, 443, magicAccessToken(), "/clinical-fhir/v0/"))
        .r4(serviceDefinition("r4", url, 443, magicAccessToken(), "/clinical-fhir/v0/r4"))
        .publicIds(syntheticIds())
        .isDqAvailable(isDqAvailable())
        .isVfqAvailable(isVfqAvailable())
        .build();
  }

  private static SystemDefinition local() {
    String url = "http://localhost";
    return SystemDefinition.builder()
        .internal(serviceDefinition("internal", url, 8120, null, "/clinical-fhir/v0"))
        .r4(serviceDefinition("r4", url, 8120, null, "/clinical-fhir/v0/r4"))
        .publicIds(localIds())
        .isDqAvailable(isDqAvailable())
        .isVfqAvailable(isVfqAvailable())
        .build();
  }

  private static TestIds localIds() {
    return TestIds.builder().patient("1011537977V693883").build();
  }

  private static TestIds productionIds() {
    return TestIds.builder().patient("1011537977V693883").build();
  }

  private static SystemDefinition qa() {
    String url = "https://blue.qa.lighthouse.va.gov";
    return SystemDefinition.builder()
        .internal(serviceDefinition("internal", url, 443, magicAccessToken(), "/clinical-fhir/v0/"))
        .r4(serviceDefinition("r4", url, 443, magicAccessToken(), "/clinical-fhir/v0/r4"))
        .publicIds(syntheticIds())
        .isDqAvailable(isDqAvailable())
        .isVfqAvailable(isVfqAvailable())
        .build();
  }

  private static ServiceDefinition serviceDefinition(
      String name, String url, int port, String accessToken, String apiPath) {
    return SentinelProperties.forName(name)
        .accessToken(() -> Optional.ofNullable(accessToken))
        .defaultUrl(url)
        .defaultPort(port)
        .defaultApiPath(apiPath)
        .defaultUrl(url)
        .build()
        .serviceDefinition();
  }

  private static SystemDefinition staging() {
    String url = "https://blue.staging.lighthouse.va.gov";
    return SystemDefinition.builder()
        .internal(serviceDefinition("internal", url, 443, magicAccessToken(), "/clinical-fhir/v0/"))
        .r4(serviceDefinition("r4", url, 443, magicAccessToken(), "/clinical-fhir/v0/r4"))
        .publicIds(productionIds())
        .isDqAvailable(isDqAvailable())
        .isVfqAvailable(isVfqAvailable())
        .build();
  }

  private static SystemDefinition stagingLab() {
    String url = "https://blue.staging-lab.lighthouse.va.gov";
    return SystemDefinition.builder()
        .internal(serviceDefinition("internal", url, 443, magicAccessToken(), "/clinical-fhir/v0/"))
        .r4(serviceDefinition("r4", url, 443, magicAccessToken(), "/clinical-fhir/v0/r4"))
        .publicIds(syntheticIds())
        .isDqAvailable(isDqAvailable())
        .isVfqAvailable(isVfqAvailable())
        .build();
  }

  private static TestIds syntheticIds() {
    return TestIds.builder().patient("1011537977V693883").build();
  }

  /** Return the applicable system definition for the current environment. */
  public static SystemDefinition systemDefinition() {
    switch (Environment.get()) {
      case LAB:
        return lab();
      case LOCAL:
        return local();
      case QA:
        return qa();
      case STAGING:
        return staging();
      case STAGING_LAB:
        return stagingLab();
      default:
        throw new IllegalArgumentException("Unknown sentinel environment: " + Environment.get());
    }
  }

  private boolean isDqAvailable() {
    return BooleanUtils.toBoolean(systemPropertyOrEnvVar("data-query.is-available", "false"));
  }

  private boolean isVfqAvailable() {
    return BooleanUtils.toBoolean(systemPropertyOrEnvVar("vista-fhir-query.is-available", "false"));
  }

  private String systemPropertyOrEnvVar(String property, String defaultValue) {
    var value = System.getProperty(property);
    if (value == null) {
      value =
          System.getenv(property.replace('.', '_').replace('-', '_').toUpperCase(Locale.ENGLISH));
    }
    return value == null ? defaultValue : value;
  }
}
