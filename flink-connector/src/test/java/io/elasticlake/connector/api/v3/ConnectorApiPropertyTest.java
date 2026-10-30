package io.elasticlake.connector.api.v3;

import static org.junit.jupiter.api.Assertions.*;

import io.elasticlake.client.api.v2.ClientApi;
import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.*;

class ConnectorApiPropertyTest {
  @Provide
  Arbitrary<byte[]> payloads() {
    return Arbitraries.bytes().array(byte[].class).ofMinSize(0).ofMaxSize(4096);
  }

  @Property(tries = 300, seed = "20261028071")
  void allSixByteFieldsSnapshotForeignBackingAndReturnCopies(@ForAll("payloads") byte[] input)
      throws Exception {
    int checked = 0;
    for (Class<?> type : ConnectorApiTest.records()) {
      var components = type.getRecordComponents();
      for (int index = 0; index < components.length; index++) {
        var component = components[index];
        if (component.getType() != ClientApi.Bytes.class) continue;
        checked++;
        byte[] original = input.clone();
        byte[] expected = original.clone();
        var args = ConnectorApiTest.arguments(type);
        args[index] = ConnectorApiTest.bytes(original);
        Object dto = ConnectorApiTest.construct(type, args);
        int hash = dto.hashCode();
        java.util.Arrays.fill(original, (byte) 77);
        var stored = (ClientApi.Bytes) component.getAccessor().invoke(dto);
        assertArrayEquals(expected, stored.copy());
        java.util.Arrays.fill(stored.copy(), (byte) 88);
        assertArrayEquals(expected, stored.copy());
        assertEquals(hash, dto.hashCode());
        args[index] = ConnectorApiTest.bytes(expected);
        assertEquals(dto, ConnectorApiTest.construct(type, args));
      }
    }
    assertEquals(6, checked);
  }

  @Property(tries = 300, seed = "20261028072")
  void nestedCoverageDoesNotFollowSourceOrReturnedArrays(@ForAll("payloads") byte[] input) {
    byte[] expected = input.clone();
    var domain = new ConnectorApi.SourceDomain("system", "partition", "comparator", 0);
    var range =
        new ConnectorApi.SourceRange(
            domain,
            ConnectorApiTest.bytes(input),
            ConnectorApiTest.bytes(input),
            ConnectorApiTest.digest("sha256-v1"),
            0,
            0,
            0,
            0,
            0);
    var ranges = new ArrayList<>(List.of(range));
    var coverage = new ConnectorApi.SourceCoverage(ranges, ConnectorApiTest.digest("sha256-v1"));
    ranges.clear();
    java.util.Arrays.fill(input, (byte) 42);
    assertEquals(1, coverage.ranges().size());
    assertArrayEquals(expected, coverage.ranges().get(0).start().copy());
    assertArrayEquals(expected, coverage.ranges().get(0).end().copy());
  }

  @Example
  void malformedForeignBytesFailLoud() {
    var domain = new ConnectorApi.SourceDomain("s", "p", "c", 0);
    for (int declared : new int[] {-1, 0, 2}) {
      ClientApi.Bytes malformed =
          new ClientApi.Bytes() {
            public int size() {
              return declared;
            }

            public byte[] copy() {
              return new byte[] {1};
            }
          };
      assertThrows(
          ClientApi.ClientException.class,
          () -> new ConnectorApi.SourcePosition(domain, malformed, 0));
    }
    ClientApi.Bytes nullCopy =
        new ClientApi.Bytes() {
          public int size() {
            return 0;
          }

          public byte[] copy() {
            return null;
          }
        };
    assertThrows(
        ClientApi.ClientException.class,
        () -> new ConnectorApi.SourcePosition(domain, nullCopy, 0));
  }
}
