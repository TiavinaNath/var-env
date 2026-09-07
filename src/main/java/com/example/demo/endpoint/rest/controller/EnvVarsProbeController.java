package com.example.demo.endpoint.rest.controller;

import com.example.demo.PojaGenerated;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Temporary probe: reports whether each lookup form reaches an SSM-backed env var, without ever
 * disclosing its value. Values are reported as length plus a short digest, so that two forms can be
 * proven to see the same value without exposing it over this public endpoint.
 *
 * <p>Delete once the env var delivery mechanism is settled.
 */
@PojaGenerated
@RestController
public class EnvVarsProbeController {

  private static final String UNRESOLVED = "<unresolved>";

  private final ConfigurableEnvironment environment;
  private final String upperViaValue;
  private final String dottedViaValue;

  public EnvVarsProbeController(
      ConfigurableEnvironment environment,
      @Value("${POJA_PROBE_VALUE:" + UNRESOLVED + "}") String upperViaValue,
      @Value("${poja.probe.value:" + UNRESOLVED + "}") String dottedViaValue) {
    this.environment = environment;
    this.upperViaValue = upperViaValue;
    this.dottedViaValue = dottedViaValue;
  }

  @GetMapping("/env-vars-probe")
  public Map<String, Object> probe(
      @RequestParam(defaultValue = "POJA_PROBE_VALUE") String name) {
    String dotted = name.toLowerCase(Locale.ROOT).replace('_', '.');
    Map<String, Object> report = new LinkedHashMap<>();
    report.put("probedUpper", name);
    report.put("probedDotted", dotted);
    report.put("System.getenv(upper)", describe(System.getenv(name)));
    report.put("System.getenv(dotted)", describe(System.getenv(dotted)));
    report.put("getProperty(upper)", describe(environment.getProperty(name)));
    report.put("getProperty(dotted)", describe(environment.getProperty(dotted)));
    report.put("@Value(${POJA_PROBE_VALUE})", describe(nullIfUnresolved(upperViaValue)));
    report.put("@Value(${poja.probe.value})", describe(nullIfUnresolved(dottedViaValue)));
    report.put(
        "spring.datasource.url", describe(environment.getProperty("spring.datasource.url")));
    report.put("propertySources", propertySourceNames());
    return report;
  }

  private List<String> propertySourceNames() {
    return environment.getPropertySources().stream()
        .map(source -> source.getName() + " :: " + source.getClass().getSimpleName())
        .toList();
  }

  private static String nullIfUnresolved(String value) {
    return UNRESOLVED.equals(value) ? null : value;
  }

  private static String describe(String value) {
    if (value == null) {
      return "NULL";
    }
    return "len=" + value.length() + " sha=" + digestPrefix(value);
  }

  private static String digestPrefix(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder();
      for (int i = 0; i < 4; i++) {
        hex.append(String.format("%02x", digest[i]));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
