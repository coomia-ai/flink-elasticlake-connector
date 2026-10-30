package io.elasticlake.connector.api.v3;

import io.elasticlake.client.api.v2.ClientApi;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

final class ConnectorValues {
  private ConnectorValues() {}

  static void check(boolean valid, String path, String reason) {
    if (!valid) {
      throw new ClientApi.ClientException(
          new ClientApi.Failure(
              ClientApi.FailureCode.INVALID_INPUT,
              new ClientApi.OptionalIntStatus(false, 0),
              reason,
              path,
              Optional.empty(),
              false),
          null);
    }
  }

  static <T> T required(T value, String path) {
    check(value != null, path, "null is prohibited");
    return value;
  }

  static void unsupported(boolean condition, String path, String reason) {
    if (condition) {
      throw new ClientApi.ClientException(
          new ClientApi.Failure(
              ClientApi.FailureCode.UNSUPPORTED_CAPABILITY,
              new ClientApi.OptionalIntStatus(false, 0),
              reason,
              path,
              Optional.empty(),
              false),
          null);
    }
  }

  static void nonnegative(long value, String path) {
    check(value >= 0, path, "negative value");
  }

  static <T> List<T> list(List<T> values, String path) {
    required(values, path);
    var result = new ArrayList<T>(values.size());
    int index = 0;
    for (T value : values) {
      result.add(required(value, path + "[" + index++ + "]"));
    }
    return List.copyOf(result);
  }

  static ClientApi.Bytes bytes(ClientApi.Bytes value, String path) {
    required(value, path);
    int size = value.size();
    check(size >= 0, path, "negative byte size");
    byte[] copy = required(value.copy(), path + ".copy").clone();
    check(copy.length == size && value.size() == size, path, "byte size/copy differs");
    return new ByteSnapshot(copy);
  }

  private static final class ByteSnapshot implements ClientApi.Bytes {
    private final byte[] value;

    private ByteSnapshot(byte[] value) {
      this.value = value;
    }

    @Override
    public int size() {
      return value.length;
    }

    @Override
    public byte[] copy() {
      return value.clone();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof ByteSnapshot snapshot && Arrays.equals(value, snapshot.value);
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(value);
    }
  }
}
