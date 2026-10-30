package io.elasticlake.connector.buildprobe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.elasticlake.connector.api.v3.ApiProbe;
import org.junit.jupiter.api.Test;

class MainProbeTest {
  @Test
  void runsOnJava17AndLoadsOneApiWithTheAcceptedSdk() {
    assertEquals(17, Runtime.version().feature());
    assertEquals("I06_NON_PRODUCT_PROBE", MainProbe.marker());
    assertEquals(MainProbe.marker(), ApiProbe.marker());
  }

  @Test
  void architectureRulesAreNotInOrdinaryJava17Tests() {
    assertThrows(
        ClassNotFoundException.class,
        () -> Class.forName("io.elasticlake.arch.rules.InjectionRules"));
    assertThrows(
        ClassNotFoundException.class, () -> Class.forName("com.tngtech.archunit.lang.ArchRule"));
  }
}
