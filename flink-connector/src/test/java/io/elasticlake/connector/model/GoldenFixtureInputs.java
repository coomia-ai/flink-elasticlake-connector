package io.elasticlake.connector.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;

/** I-05 expected bytes are pinned independently of the model under test. */
public final class GoldenFixtureInputs {
  private static final Map<String, String> PINS =
      Map.of(
          "epoch-vectors.properties",
          "dcc621efe1231d1894a03dbc22d700682824cd062769628b74708f39d51eb675",
          "epoch-rejections.properties",
          "3c12cd8adadf90d1f45c957e92c953fc398e059a9ad36ba03a134237a7f525f6",
          "key-vectors.properties",
          "3e0e2d8d3f5cd7f0e17dc31fc26f1d02a955289a1db3125cfb87910e12eb139c",
          "key-rejections.properties",
          "c4bea9924c617c152cd15ebac6b09fd918e937a3493061c498f43c5f1b509178");

  private GoldenFixtureInputs() {}

  static Properties read(String name) throws IOException, NoSuchAlgorithmException {
    String expected = PINS.get(name);
    if (expected == null) {
      throw new IllegalArgumentException("unbound I-05 fixture: " + name);
    }
    byte[] bytes;
    try (var stream = GoldenFixtureInputs.class.getResourceAsStream("/i05/" + name)) {
      if (stream == null) {
        throw new IllegalStateException("missing I-05 fixture: " + name);
      }
      bytes = stream.readAllBytes();
    }
    String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    if (!expected.equals(actual)) {
      throw new IllegalStateException("I-05 fixture SHA mismatch: " + name + " actual=" + actual);
    }
    var properties = new Properties();
    properties.load(new ByteArrayInputStream(bytes));
    return properties;
  }

  public static void main(String[] arguments) throws IOException, NoSuchAlgorithmException {
    for (String name : PINS.keySet()) {
      read(name);
    }
    System.out.println("I05_FIXTURE_PINS_OK resources=" + PINS.size());
  }
}
