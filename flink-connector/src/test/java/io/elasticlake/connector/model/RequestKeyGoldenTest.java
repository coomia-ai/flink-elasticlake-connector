package io.elasticlake.connector.model;

import static io.elasticlake.connector.model.ModelFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import io.elasticlake.client.api.v2.ClientApi;
import io.elasticlake.connector.api.v3.ConnectorApi;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class RequestKeyGoldenTest {
  @TestFactory
  Stream<DynamicTest> matchesIndependentKeyFramesAndNewParts()
      throws IOException, NoSuchAlgorithmException {
    var properties = GoldenFixtureInputs.read("key-vectors.properties");
    assertEquals(23, properties.size());
    return properties.stringPropertyNames().stream()
        .sorted()
        .map(
            name ->
                DynamicTest.dynamicTest(
                    name,
                    () -> {
                      var f = properties.getProperty(name).split("\\|", -1);
                      assertEquals(7, f.length);
                      String lineage = text(f[0]);
                      String tableId = text(f[1]);
                      byte[] checkpoint = HexFormat.of().parseHex(f[2]);
                      int slot = Integer.parseInt(f[3]);
                      int budget = Integer.parseInt(f[4]);
                      byte[] frame =
                          RequestKeys.canonicalFrame(
                              lineage, tableId, bytes(checkpoint), slot, budget);
                      assertArrayEquals(HexFormat.of().parseHex(f[5]), frame);
                      assertEquals(
                          f[6],
                          "elk1:"
                              + HexFormat.of()
                                  .formatHex(MessageDigest.getInstance("SHA-256").digest(frame)));
                      if (!name.startsWith("ambiguous-")
                          && !name.equals("manual-minimal")
                          && !name.equals("slot-max")) {
                        var cursor = ByteBuffer.wrap(checkpoint);
                        cursor.position(4 + cursor.getInt());
                        int size = cursor.getInt();
                        byte[] originalLineage = new byte[size];
                        cursor.get(originalLineage);
                        assertEquals(lineage, new String(originalLineage, StandardCharsets.UTF_8));
                        long generation = cursor.getLong();
                        long number = cursor.getLong();
                        byte[] coverage = new byte[32];
                        cursor.get(coverage);
                        var epoch =
                            new ConnectorApi.EpochIdentity(
                                new ConnectorApi.Lineage(lineage, "uid", DIGEST, DIGEST, DIGEST),
                                generation,
                                number,
                                new ClientApi.Digest(
                                    "sha256-v1",
                                    "sha256-v1:" + HexFormat.of().formatHex(coverage)));
                        var table = table();
                        var bound =
                            new ClientApi.TableBinding(
                                table.namespace(),
                                table.table(),
                                tableId,
                                table.tableId(),
                                table.schemaRevision(),
                                table.schema(),
                                table.versionDomain(),
                                table.pkCodec(),
                                table.protocolProfileDigest());
                        var context =
                            new ClientApi.CheckpointContext(
                                "fixture",
                                bytes(checkpoint),
                                "original-wire",
                                0,
                                "fixture-comparator",
                                ClientApi.Coverage.EXACT,
                                0,
                                Optional.empty());
                        if (name.equals("table-nfd")) {
                          assertNotEquals(tableId, bound.opaqueIdentity());
                          var failure =
                              assertThrows(
                                  ClientApi.ClientException.class,
                                  () ->
                                      RequestKeys.newPartKey(
                                          epoch, bound, context, tableId, slot, 2, budget));
                          assertEquals(ClientApi.FailureCode.CONFLICT, failure.failure().code());
                          assertTrue(
                              failure.getMessage().contains("RequestKeys.originalOpaqueIdentity"));
                        } else {
                          assertEquals(
                              f[6],
                              RequestKeys.newPartKey(
                                  epoch, bound, context, tableId, slot, 2, budget));
                        }
                      }
                    }));
  }

  @TestFactory
  Stream<DynamicTest> rejectsIndependentKeyInputs() throws IOException, NoSuchAlgorithmException {
    var properties = GoldenFixtureInputs.read("key-rejections.properties");
    assertEquals(10, properties.size());
    // >Integer.MAX_VALUE belongs to oracle/parser admission, outside the typed Java slot domain.
    return properties.stringPropertyNames().stream()
        .filter(name -> !name.equals("slot-overflow"))
        .sorted()
        .map(
            name ->
                DynamicTest.dynamicTest(
                    name,
                    () -> {
                      var f = properties.getProperty(name).split("\\|", -1);
                      var failure =
                          assertThrows(
                              ClientApi.ClientException.class,
                              () ->
                                  RequestKeys.canonicalFrame(
                                      text(f[0]),
                                      text(f[1]),
                                      bytes(HexFormat.of().parseHex(f[2])),
                                      Integer.parseInt(f[3]),
                                      Integer.parseInt(f[4])));
                      assertEquals(ClientApi.FailureCode.valueOf(f[5]), failure.failure().code());
                    }));
  }

  @Test
  void rejectsForeignContextsAndOriginalCoordinates() {
    var e = epoch(7, 1);
    var t = table();
    var c = context(e);
    for (int[] pair : new int[][] {{-1, 1}, {0, 0}, {1, 1}, {0, 1_000_001}}) {
      assertThrows(
          ClientApi.ClientException.class,
          () -> RequestKeys.newPartKey(e, t, c, t.opaqueIdentity(), pair[0], pair[1], 4096));
    }
    for (var foreign :
        new ClientApi.CheckpointContext[] {
          context(epoch(8, 1)),
          new ClientApi.CheckpointContext(
              c.domain(),
              c.checkpointCanonical(),
              c.checkpointWire(),
              c.registryRevision(),
              c.comparatorId(),
              ClientApi.Coverage.CUMULATIVE,
              c.schemaRevision(),
              c.versionDomain()),
          new ClientApi.CheckpointContext(
              c.domain(),
              c.checkpointCanonical(),
              c.checkpointWire(),
              c.registryRevision(),
              c.comparatorId(),
              c.coverage(),
              1,
              c.versionDomain()),
          new ClientApi.CheckpointContext(
              c.domain(),
              c.checkpointCanonical(),
              c.checkpointWire(),
              c.registryRevision(),
              c.comparatorId(),
              c.coverage(),
              c.schemaRevision(),
              Optional.of("foreign"))
        }) {
      var failure =
          assertThrows(
              ClientApi.ClientException.class,
              () -> RequestKeys.newPartKey(e, t, foreign, t.opaqueIdentity(), 0, 1, 4096));
      assertEquals(ClientApi.FailureCode.CONFLICT, failure.failure().code());
    }
    assertEquals(
        RequestKeys.newPartKey(e, t, c, t.opaqueIdentity(), 0, 1, 4096),
        RequestKeys.newPartKey(e, t, c, t.opaqueIdentity(), 0, 2, 4096));
  }

  @Test
  void enforcesBudgetBeforeCopyAndRejectsLyingBytePorts() {
    var huge =
        new ClientApi.Bytes() {
          public int size() {
            return Integer.MAX_VALUE;
          }

          public byte[] copy() {
            throw new AssertionError("must reject before copy");
          }
        };
    var failure =
        assertThrows(
            ClientApi.ClientException.class,
            () -> RequestKeys.canonicalFrame("L", "T", huge, 0, 132));
    assertEquals(ClientApi.FailureCode.CAPACITY, failure.failure().code());
    var lying =
        new ClientApi.Bytes() {
          public int size() {
            return 1;
          }

          public byte[] copy() {
            return new byte[2];
          }
        };
    assertThrows(
        ClientApi.ClientException.class, () -> RequestKeys.canonicalFrame("L", "T", lying, 0, 132));
  }

  private static String text(String utf16Hex) {
    return ByteBuffer.wrap(HexFormat.of().parseHex(utf16Hex)).asCharBuffer().toString();
  }
}
