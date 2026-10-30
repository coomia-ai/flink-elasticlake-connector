package io.elasticlake.connector.model;

import static org.junit.jupiter.api.Assertions.*;

import io.elasticlake.client.api.v2.ClientApi;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

class RequestKeyPropertyTest {
  @Provide
  Arbitrary<String> opaqueText() {
    return Arbitraries.strings().withCharRange('\u0000', '\ud7ff').ofMaxLength(24);
  }

  @Provide
  Arbitrary<byte[]> checkpoints() {
    return Arbitraries.bytes().array(byte[].class).ofMaxSize(256);
  }

  @Property(tries = 500, seed = "20261028165")
  void matchesNaiveStreamFrameAndExactBudget(
      @ForAll("opaqueText") String lineage,
      @ForAll("opaqueText") String table,
      @ForAll("checkpoints") byte[] checkpoint,
      @ForAll @IntRange(min = 0) int slot)
      throws Exception {
    var out = new ByteArrayOutputStream();
    var data = new DataOutputStream(out);
    byte[] domain = "elasticlake-connector-request-key".getBytes(StandardCharsets.US_ASCII);
    data.writeInt(domain.length);
    data.write(domain);
    data.writeShort(1);
    for (byte[] value :
        new byte[][] {
          lineage.getBytes(StandardCharsets.UTF_8),
          table.getBytes(StandardCharsets.UTF_8),
          checkpoint
        }) {
      data.writeInt(value.length);
      data.write(value);
    }
    data.writeInt(slot);
    byte[] expected = out.toByteArray();
    assertArrayEquals(
        expected,
        RequestKeys.canonicalFrame(
            lineage, table, ModelFixtures.bytes(checkpoint), slot, expected.length));
    var failure =
        assertThrows(
            ClientApi.ClientException.class,
            () ->
                RequestKeys.canonicalFrame(
                    lineage, table, ModelFixtures.bytes(checkpoint), slot, expected.length - 1));
    assertEquals(ClientApi.FailureCode.CAPACITY, failure.failure().code());
  }
}
