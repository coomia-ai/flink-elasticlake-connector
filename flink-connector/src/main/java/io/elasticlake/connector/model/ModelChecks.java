package io.elasticlake.connector.model;

import io.elasticlake.client.api.v2.ClientApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class ModelChecks {
  private ModelChecks() {}

  static void require(boolean valid, ClientApi.FailureCode code, String path, String reason) {
    if (!valid) throw failure(code, path, reason, null);
  }

  static <T> T required(T value, String path) {
    require(value != null, ClientApi.FailureCode.INVALID_INPUT, path, "null is prohibited");
    return value;
  }

  static void nonnegative(long value, String path) {
    require(value >= 0, ClientApi.FailureCode.INVALID_INPUT, path, "negative value");
  }

  static long add(long left, long right, String path) {
    try {
      return Math.addExact(left, right);
    } catch (ArithmeticException overflow) {
      throw failure(ClientApi.FailureCode.CAPACITY, path, "numeric overflow", overflow);
    }
  }

  static <T> List<T> list(List<T> values, String path) {
    required(values, path);
    var copy = new ArrayList<T>(values.size());
    int index = 0;
    for (T value : values) copy.add(required(value, path + "[" + index++ + "]"));
    return List.copyOf(copy);
  }

  static <T> Set<T> set(Set<T> values, String path) {
    required(values, path);
    for (T value : values) required(value, path + "[]");
    return Set.copyOf(values);
  }

  private static ClientApi.ClientException failure(
      ClientApi.FailureCode code, String path, String reason, Throwable cause) {
    return new ClientApi.ClientException(
        new ClientApi.Failure(
            code, new ClientApi.OptionalIntStatus(false, 0), reason, path, Optional.empty(), false),
        cause);
  }
}
