package io.elasticlake.connector.recovery.s3.buildprobe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ProviderProbeTest {
  @Test
  void providerRunsOnJava17AgainstOnlyTheLogicalApiVariant() {
    assertEquals(17, Runtime.version().feature());
    assertEquals("I06_NON_PRODUCT_PROBE", ProviderProbe.marker());
    assertThrows(
        ClassNotFoundException.class,
        () -> Class.forName("io.elasticlake.connector.buildprobe.MainProbe"));
  }

  @Test
  void providerDoesNotLoadArchitectureRulesInOrdinaryTests() {
    assertThrows(
        ClassNotFoundException.class,
        () -> Class.forName("io.elasticlake.arch.rules.InjectionRules"));
    assertThrows(
        ClassNotFoundException.class, () -> Class.forName("com.tngtech.archunit.lang.ArchRule"));
  }
}
