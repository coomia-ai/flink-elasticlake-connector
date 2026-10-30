package io.elasticlake.connector.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.elasticlake.client.api.v2.ClientApi;
import io.elasticlake.connector.api.v3.ConnectorApi;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class IndependentEpochGoldenTest {
  @TestFactory
  Stream<DynamicTest> matchesIndependentOriginalBytes()
      throws IOException, NoSuchAlgorithmException {
    var properties = GoldenFixtureInputs.read("epoch-vectors.properties");
    assertEquals(12, properties.size());
    return properties.stringPropertyNames().stream()
        .sorted()
        .map(
            name ->
                DynamicTest.dynamicTest(
                    name,
                    () -> {
                      String[] fields = properties.getProperty(name).split("\\|", -1);
                      assertEquals(6, fields.length);
                      var hex = HexFormat.of();
                      var epoch =
                          identity(
                              new String(hex.parseHex(fields[0]), StandardCharsets.UTF_8),
                              fields[1],
                              fields[2],
                              fields[3]);
                      assertArrayEquals(
                          hex.parseHex(fields[5]),
                          EpochFrames.canonical(epoch, Integer.parseInt(fields[4])));
                    }));
  }

  @TestFactory
  Stream<DynamicTest> rejectsIndependentMalformedInputs()
      throws IOException, NoSuchAlgorithmException {
    var properties = GoldenFixtureInputs.read("epoch-rejections.properties");
    assertEquals(12, properties.size());
    // >Long.MAX_VALUE cannot enter the typed API; I-05 checks these two at its numeric boundary.
    return properties.stringPropertyNames().stream()
        .filter(name -> !name.endsWith("overflow"))
        .sorted()
        .map(
            name ->
                DynamicTest.dynamicTest(
                    name,
                    () -> {
                      String[] fields = properties.getProperty(name).split("\\|", -1);
                      assertEquals(6, fields.length);
                      var chars =
                          ByteBuffer.wrap(HexFormat.of().parseHex(fields[0])).asCharBuffer();
                      String lineage = chars.toString();
                      var failure =
                          assertThrows(
                              ClientApi.ClientException.class,
                              () -> {
                                var epoch = identity(lineage, fields[1], fields[2], fields[3]);
                                EpochFrames.canonical(epoch, Integer.parseInt(fields[4]));
                              });
                      assertEquals(
                          ClientApi.FailureCode.valueOf(fields[5]), failure.failure().code());
                    }));
  }

  private static ConnectorApi.EpochIdentity identity(
      String lineage, String generation, String checkpoint, String coverage) {
    var binding = ModelFixtures.DIGEST;
    return new ConnectorApi.EpochIdentity(
        new ConnectorApi.Lineage(lineage, "fixture-uid", binding, binding, binding),
        Long.parseLong(generation),
        Long.parseLong(checkpoint),
        new ClientApi.Digest("sha256-v1", "sha256-v1:" + coverage));
  }
}
