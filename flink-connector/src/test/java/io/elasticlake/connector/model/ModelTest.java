package io.elasticlake.connector.model;

import static io.elasticlake.connector.model.ModelFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import io.elasticlake.client.api.v2.ClientApi;
import io.elasticlake.connector.api.v3.ConnectorApi;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ModelTest {
  @Test
  void manualFrameIsBigEndianLengthPrefixedAndBudgeted() {
    var lineage = new ConnectorApi.Lineage("a", "u", DIGEST, DIGEST, DIGEST);
    var epoch = new ConnectorApi.EpochIdentity(lineage, 0, 1, DIGEST);
    String manual =
        "00000012636f6e6e6563746f722d65786163742d7632"
            + "0000000161"
            + "0000000000000000"
            + "0000000000000001"
            + "00".repeat(32);
    assertEquals(manual, HexFormat.of().formatHex(EpochFrames.canonical(epoch, 75)));
    assertThrows(ClientApi.ClientException.class, () -> EpochFrames.canonical(epoch, 74));
    assertThrows(ClientApi.ClientException.class, () -> EpochFrames.canonical(epoch, 0));
    for (String id : List.of("中", "é", "e\u0301", "\ud83d\ude00", "")) {
      var e =
          new ConnectorApi.EpochIdentity(
              new ConnectorApi.Lineage(id, "u", DIGEST, DIGEST, DIGEST),
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              DIGEST);
      assertArrayEquals(frameOracle(e), EpochFrames.canonical(e, 1024));
    }
    for (String id : List.of("\ud800", "\udc00")) {
      var malformed =
          new ConnectorApi.EpochIdentity(
              new ConnectorApi.Lineage(id, "u", DIGEST, DIGEST, DIGEST), 0, 0, DIGEST);
      assertThrows(ClientApi.ClientException.class, () -> EpochFrames.canonical(malformed, 1024));
    }
    assertFalse(
        Arrays.equals(
            EpochFrames.canonical(epoch(0, 1), 1024), EpochFrames.canonical(epoch(1, 1), 1024)));
  }

  @Test
  void providerContextMustContainExactOriginalFrame() {
    ConnectorApi.SourceCoverageProvider provider =
        new ConnectorApi.SourceCoverageProvider() {
          public ConnectorApi.SourceCoverage merge(
              long checkpoint,
              List<ConnectorApi.CheckpointFragment> fragments,
              ClientApi.PreparationLimits limits) {
            throw new UnsupportedOperationException("source coverage codec is unqualified");
          }

          public ClientApi.CheckpointContext exactContext(
              ConnectorApi.EpochIdentity epoch, ClientApi.TableBinding table) {
            return context(epoch);
          }
        };
    var expected = context(epoch(7, 1));
    assertEquals(expected, EpochFrames.exactContext(provider, epoch(7, 1), table(), 1024));
    ConnectorApi.SourceCoverageProvider stale =
        new ConnectorApi.SourceCoverageProvider() {
          public ConnectorApi.SourceCoverage merge(
              long checkpoint,
              List<ConnectorApi.CheckpointFragment> fragments,
              ClientApi.PreparationLimits limits) {
            throw new UnsupportedOperationException("unqualified");
          }

          public ClientApi.CheckpointContext exactContext(
              ConnectorApi.EpochIdentity epoch, ClientApi.TableBinding table) {
            return context(epoch(0, 1));
          }
        };
    assertThrows(
        ClientApi.ClientException.class,
        () -> EpochFrames.exactContext(stale, epoch(7, 1), table(), 1024));
  }

  @Test
  void pinnedPlanKeepsOriginalKeysCFirstAttemptPayloadAndMutationSource() {
    var plan = plan(epoch(7, 1));
    var id = new ConnectorApi.MutationIdentity("project", "source", "id");
    var position =
        new ConnectorApi.SourcePosition(
            new ConnectorApi.SourceDomain("s", "p", "cmp", 0), bytes(new byte[] {2}), 0);
    var mutation =
        new ConnectorApi.SinkMutation(
            id,
            position,
            table(),
            new ClientApi.Delete(
                new ClientApi.ScalarKey(new ClientApi.IntValue(java.math.BigInteger.ONE)),
                java.util.OptionalLong.empty()),
            Optional.of("event"));
    var mutable = new ArrayList<>(List.of(mutation));
    var pinned = new PlanIdentities.Pinned(plan, mutable);
    mutable.clear();
    assertSame(mutation, pinned.originalInputs().get(0));
    assertSame(plan, pinned.plan());
    var parts = PlanIdentities.originalParts(pinned, 1024);
    assertSame(plan.tables().get(0).parts().get(0), parts.get(0));
    assertEquals("elk1:original", parts.get(0).fact().key());
    assertEquals(17, parts.get(0).fact().firstAttemptMillis().orElseThrow());
    assertEquals("original-wire", parts.get(0).fact().context().checkpointWire());
    assertThrows(UnsupportedOperationException.class, parts::clear);
    var wrong =
        new ConnectorApi.CheckpointPlan(
            epoch(8, 1), plan.tables(), plan.coverage(), plan.contentDigest());
    assertThrows(
        ClientApi.ClientException.class,
        () -> PlanIdentities.originalParts(new PlanIdentities.Pinned(wrong, List.of()), 1024));
  }

  @Test
  void finalizedManagersOccupySlotsUntilCompleteRetirementView() {
    var one = epoch(0, 1);
    var two = epoch(0, 2);
    var slots =
        new PendingEpochs()
            .reserveBeforeSeal(one)
            .sealed(sealed(one))
            .finalized(one)
            .reserveBeforeSeal(two)
            .sealed(sealed(two));
    assertEquals(2, slots.managers().size());
    assertThrows(ClientApi.ClientException.class, () -> slots.reserveBeforeSeal(epoch(0, 3)));
    var point = new ConnectorApi.RestorePoint(3, DIGEST);
    for (var view :
        List.of(
            new PendingEpochs.RetirementView(LINEAGE, point, false, Set.of(), Set.of()),
            new PendingEpochs.RetirementView(LINEAGE, point, true, Set.of(one), Set.of()),
            new PendingEpochs.RetirementView(LINEAGE, point, true, Set.of(), Set.of(one)))) {
      assertThrows(ClientApi.ClientException.class, () -> slots.retire(one, view));
    }
    var retired =
        slots.retire(
            one, new PendingEpochs.RetirementView(LINEAGE, point, true, Set.of(two), Set.of(two)));
    assertEquals(2, slots.managers().size());
    assertEquals(1, retired.managers().size());
    assertEquals(2, retired.reserveBeforeSeal(epoch(0, 3)).managers().size());
    assertThrows(UnsupportedOperationException.class, () -> slots.managers().clear());
    var different = new ConnectorApi.Lineage("different", "uid", DIGEST, DIGEST, DIGEST);
    assertThrows(
        ClientApi.ClientException.class,
        () ->
            slots.retire(
                one, new PendingEpochs.RetirementView(different, point, true, Set.of(), Set.of())));
    assertThrows(ClientApi.ClientException.class, () -> new PendingEpochs().sealed(sealed(one)));
  }

  @Test
  void callbackAndDrainEnvelopesCannotBeRefreshedOrSpentAs150SecondCallbacks() {
    var one = epoch(0, 1);
    var two = epoch(0, 2);
    var drain =
        new CommitBudgets.Drain(List.of())
            .start(one)
            .charge(one, CommitBudgets.CALLBACK_NANOS, 4096)
            .start(two)
            .charge(two, CommitBudgets.CALLBACK_NANOS, 4096);
    assertEquals(CommitBudgets.DRAIN_NANOS, drain.elapsedNanos());
    assertEquals(8192, drain.wireAttempts());
    assertDoesNotThrow(
        () ->
            new CommitBudgets.Initialization(
                CommitBudgets.FRAMEWORK_NANOS, drain, CommitBudgets.CLEANUP_NANOS));
    assertThrows(
        ClientApi.ClientException.class,
        () ->
            new CommitBudgets.Initialization(
                CommitBudgets.FRAMEWORK_NANOS + 1, drain, CommitBudgets.CLEANUP_NANOS));
    assertThrows(ClientApi.ClientException.class, () -> drain.charge(one, 1, 0));
    assertThrows(ClientApi.ClientException.class, () -> drain.charge(two, 0, 1));
    assertThrows(ClientApi.ClientException.class, () -> drain.start(one));
    assertThrows(ClientApi.ClientException.class, () -> drain.start(epoch(0, 3)));
    assertThrows(
        ClientApi.ClientException.class,
        () -> new CommitBudgets.Callback(one, 150_000_000_000L, 0));
    var empty = new CommitBudgets.Drain(List.of()).start(one).start(two);
    assertThrows(ClientApi.ClientException.class, () -> empty.start(epoch(0, 3)));
    var active = new CommitBudgets.Callback(one, 1, 1);
    assertThrows(ClientApi.ClientException.class, () -> active.charge(Long.MAX_VALUE, 0));
    assertEquals(1, active.elapsedNanos());
    assertThrows(ClientApi.ClientException.class, () -> active.charge(-1, 0));
  }
}
