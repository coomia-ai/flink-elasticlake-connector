package io.elasticlake.connector.api.v3;

import static io.elasticlake.connector.api.v3.ConnectorApi.*;
import static org.junit.jupiter.api.Assertions.*;

import io.elasticlake.client.api.v2.ClientApi;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class ConnectorApiTest {
  static final String HEX = "0".repeat(64);

  static List<Class<?>> records() {
    var result =
        Arrays.stream(ConnectorApi.class.getDeclaredClasses()).filter(Class::isRecord).toList();
    assertEquals(35, result.size());
    return result;
  }

  static ClientApi.Digest digest(String algorithm) {
    return new ClientApi.Digest(algorithm, algorithm + ":" + HEX);
  }

  static ClientApi.Bytes bytes(byte[] backing) {
    return new ClientApi.Bytes() {
      public int size() {
        return backing.length;
      }

      public byte[] copy() {
        return backing;
      }
    };
  }

  static Object value(Class<?> type) {
    if (type == String.class) return "opaque";
    if (type == long.class) return 0L;
    if (type == int.class) return 0;
    if (type == boolean.class) return false;
    if (type == Optional.class) return Optional.empty();
    if (type == List.class) return new ArrayList<>();
    if (type.isEnum()) return type.getEnumConstants()[0];
    if (type == ClientApi.Bytes.class) return bytes(new byte[] {1, 2, 3});
    if (type == ClientApi.Digest.class) return digest("sha256-v1");
    if (type == RunStart.class)
      return new BootstrapStart((GenesisIdentity) value(GenesisIdentity.class));
    if (type == ClientApi.Mutation.class) {
      return new ClientApi.Delete(
          new ClientApi.ScalarKey(new ClientApi.IntValue(java.math.BigInteger.ONE)),
          OptionalLong.empty());
    }
    if (type == ClientApi.TableBinding.class) {
      var id = new ClientApi.Column("id", ClientApi.LogicalType.INT64, false);
      var schema =
          new ClientApi.SchemaDescriptor(
              "sha256:" + HEX, List.of(id), List.of(id), Optional.empty(), Optional.empty());
      var codec = new ClientApi.PkCodecBinding("test", "test", schema.fingerprint(), List.of(id));
      return new ClientApi.TableBinding(
          "ns", "table", "table-id", 0, 0, schema, Optional.empty(), codec, "profile");
    }
    if (type == ClientApi.RootFreeFact.class || type == ClientApi.TableSeal.class) {
      var table = (ClientApi.TableBinding) value(ClientApi.TableBinding.class);
      var context =
          new ClientApi.CheckpointContext(
              "connector-exact-v2",
              bytes(new byte[] {1}),
              "wire",
              0,
              "cmp",
              ClientApi.Coverage.EXACT,
              0,
              Optional.empty());
      if (type == ClientApi.TableSeal.class)
        return new ClientApi.TableSeal(table, context, List.of(), digest("sha256-v2"));
      return new ClientApi.RootFreeFact(
          table,
          context,
          0,
          1,
          "elk1:test",
          OptionalLong.empty(),
          digest("payload-sha256-v1"),
          digest("sha256-v1"),
          digest("part-sha256-v1"));
    }
    if (type == ClientApi.CallBudget.class) {
      return new ClientApi.CallBudget() {
        public long deadlineNanos() {
          return 1;
        }

        public boolean tryAcquireWireAttempt() {
          return false;
        }

        public ClientApi.Cancellation cancellation() {
          throw new UnsupportedOperationException("unused test port");
        }
      };
    }
    if (type.isRecord() && type.getEnclosingClass() == ConnectorApi.class)
      return construct(type, arguments(type));
    throw new AssertionError("Missing specimen for " + type);
  }

  static Object[] arguments(Class<?> type) {
    var components = type.getRecordComponents();
    var args = new Object[components.length];
    for (int index = 0; index < args.length; index++) {
      args[index] =
          components[index].getName().equals("expectedWriters")
              ? 1
              : value(components[index].getType());
    }
    return args;
  }

  static Object construct(Class<?> type, Object[] args) {
    try {
      return type.getConstructor(
              Arrays.stream(type.getRecordComponents())
                  .map(RecordComponent::getType)
                  .toArray(Class<?>[]::new))
          .newInstance(args);
    } catch (InvocationTargetException failure) {
      if (failure.getCause() instanceof RuntimeException runtime) throw runtime;
      throw new AssertionError(failure.getCause());
    } catch (ReflectiveOperationException failure) {
      throw new AssertionError(failure);
    }
  }

  @TestFactory
  Stream<DynamicTest> constructorsRejectEveryNullAndNegativeComponent() {
    var tests = new ArrayList<DynamicTest>();
    for (Class<?> type : records()) {
      tests.add(
          DynamicTest.dynamicTest(
              type.getSimpleName() + " valid",
              () -> assertNotNull(construct(type, arguments(type)))));
      var components = type.getRecordComponents();
      for (int index = 0; index < components.length; index++) {
        var component = components[index];
        if (component.getType() == boolean.class) continue;
        int position = index;
        tests.add(
            DynamicTest.dynamicTest(
                type.getSimpleName() + "." + component.getName() + " invalid",
                () -> {
                  var args = arguments(type);
                  if (component.getType() == long.class) args[position] = Long.valueOf(-1);
                  else if (component.getType() == int.class) args[position] = Integer.valueOf(-1);
                  else args[position] = null;
                  var error =
                      assertThrows(ClientApi.ClientException.class, () -> construct(type, args));
                  assertEquals(ClientApi.FailureCode.INVALID_INPUT, error.failure().code());
                  assertTrue(
                      error
                          .failure()
                          .path()
                          .startsWith(type.getSimpleName() + "." + component.getName()));
                }));
      }
    }
    return tests.stream();
  }

  @TestFactory
  Stream<DynamicTest> everyListIsClosedAndRejectsNullElements() {
    var tests = new ArrayList<DynamicTest>();
    for (Class<?> type : records()) {
      var components = type.getRecordComponents();
      for (int index = 0; index < components.length; index++) {
        var component = components[index];
        if (component.getType() != List.class) continue;
        int position = index;
        tests.add(
            DynamicTest.dynamicTest(
                type.getSimpleName() + "." + component.getName(),
                () -> {
                  var args = arguments(type);
                  var mutable = new ArrayList<>();
                  args[position] = mutable;
                  var result = construct(type, args);
                  mutable.add("changed after construction");
                  var exposed = (List<?>) component.getAccessor().invoke(result);
                  assertTrue(exposed.isEmpty());
                  assertThrows(UnsupportedOperationException.class, exposed::clear);
                  args[position] = Arrays.asList((Object) null);
                  assertThrows(ClientApi.ClientException.class, () -> construct(type, args));
                }));
      }
    }
    return tests.stream();
  }

  @Test
  void opaqueValuesAreNotReinterpretedAndPortsKeepAuthority() {
    assertEquals(17, Runtime.version().feature());
    assertNotEquals(
        ClientApi.class.getProtectionDomain().getCodeSource().getLocation(),
        ConnectorApi.class.getProtectionDomain().getCodeSource().getLocation());
    assertEquals("", new PageToken("").opaque());
    assertEquals(" opaque\n", new PageToken(" opaque\n").opaque());
    var port = (ClientApi.CallBudget) value(ClientApi.CallBudget.class);
    assertSame(port, new StoreBudget(port, 0, 0).call());
    assertThrows(
        ClientApi.ClientException.class,
        () -> new CheckpointFragment(0, 1, 1, (WriterFragment) value(WriterFragment.class)));
    assertThrows(
        ClientApi.ClientException.class,
        () -> new CheckpointFragment(0, 0, 0, (WriterFragment) value(WriterFragment.class)));
  }

  @Test
  void dispositionClosedBranches() {
    var position = (SourcePosition) value(SourcePosition.class);
    var output = (OutputMapping) value(OutputMapping.class);
    for (Disposition disposition : Disposition.values()) {
      for (boolean outputs : List.of(false, true)) {
        for (boolean dlq : List.of(false, true)) {
          Runnable create =
              () ->
                  new InputDisposition(
                      position,
                      disposition,
                      outputs ? List.of(output) : List.of(),
                      dlq ? Optional.of(digest("sha256-v1")) : Optional.empty());
          boolean valid =
              (disposition == Disposition.KEPT || !outputs)
                  && ((disposition == Disposition.DLQ) == dlq);
          if (valid) assertDoesNotThrow(create::run);
          else assertThrows(ClientApi.ClientException.class, create::run);
        }
      }
    }
  }

  @Test
  void acknowledgedLocalStateRequiresItsEvidence() {
    var ack =
        new ClientApi.WriteAck(
            "table",
            0,
            java.util.Map.of(0, 0L),
            java.util.Map.of(0, 0L),
            0,
            new ClientApi.SeqRange(1, 0),
            "elk1:test",
            digest("sha256-v1"),
            false,
            0,
            0,
            Optional.empty(),
            0,
            Optional.empty());
    for (PartStage stage : PartStage.values()) {
      assertDoesNotThrow(() -> new PartProgress(0, 0, stage, Optional.of(ack)));
      if (stage == PartStage.ACKED || stage == PartStage.LANDED) {
        assertThrows(
            ClientApi.ClientException.class, () -> new PartProgress(0, 0, stage, Optional.empty()));
      } else assertDoesNotThrow(() -> new PartProgress(0, 0, stage, Optional.empty()));
    }
    var sealed = (SealedRef) value(SealedRef.class);
    for (boolean acknowledged : List.of(false, true)) {
      for (boolean outbox : List.of(false, true)) {
        Runnable create =
            () ->
                new Progress(
                    0,
                    sealed,
                    List.of(),
                    List.of(),
                    outbox ? Optional.of((BlobRef) value(BlobRef.class)) : Optional.empty(),
                    acknowledged);
        if (acknowledged && !outbox) assertThrows(ClientApi.ClientException.class, create::run);
        else assertDoesNotThrow(create::run);
      }
    }
  }

  @Test
  void restoreRejectionRequiresAReasonOnlyWhenRejected() {
    var permit = (ControlledRunPermit) value(ControlledRunPermit.class);
    for (String reason : List.of("", " \t", "unproven old run exit")) {
      assertDoesNotThrow(() -> new RestoreDecision(true, reason, permit, List.of()));
      if (reason.isBlank())
        assertThrows(
            ClientApi.ClientException.class,
            () -> new RestoreDecision(false, reason, permit, List.of()));
      else assertDoesNotThrow(() -> new RestoreDecision(false, reason, permit, List.of()));
    }
  }
}
