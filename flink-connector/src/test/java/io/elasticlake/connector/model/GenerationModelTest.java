package io.elasticlake.connector.model;

import static io.elasticlake.connector.model.ModelFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import io.elasticlake.client.api.v2.ClientApi;
import io.elasticlake.connector.api.v3.ConnectorApi;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class GenerationModelTest {
  @Test
  void rejectsCompleteButNonemptyBootstrapInventoryAndContradictoryGenesis()
      throws ReflectiveOperationException {
    BootstrapRegression.verify();
    var kinds = java.util.List.of("registration", "plan", "intent", "progress", "outbox");
    var bootstrap = permit(new ConnectorApi.BootstrapStart(GENESIS), "bootstrap", 0);
    for (int mask = 0; mask < 32; mask++) {
      var inventory = new java.util.ArrayList<String>();
      for (int bit = 0; bit < kinds.size(); bit++)
        if ((mask & (1 << bit)) != 0) inventory.add(kinds.get(bit));
      var facts = new GenerationModel.Facts(true, false, false, inventory.isEmpty());
      if (inventory.isEmpty())
        assertDoesNotThrow(
            () -> GenerationModel.open(Optional.empty(), OptionalLong.empty(), bootstrap, facts));
      else
        assertThrows(
            ClientApi.ClientException.class,
            () -> GenerationModel.open(Optional.empty(), OptionalLong.empty(), bootstrap, facts));
    }
  }

  @Test
  void bootstrapHasNoSyntheticRestorePointOrStoppedPriorRun() {
    var state =
        GenerationModel.open(
                Optional.empty(),
                OptionalLong.empty(),
                permit(new ConnectorApi.BootstrapStart(GENESIS), "r0", 0),
                new GenerationModel.Facts(true, false, false, true))
            .registration();
    assertEquals(0, state.generation());
    assertTrue(state.completed().isEmpty());
    assertFalse(state.hasIntent());
    assertEquals(GENESIS, state.genesis());
  }

  @Test
  void exactPermitResponseLossConvergesWithoutAnotherIncrement() {
    var old = bootstrap();
    var permit = permit(new ConnectorApi.BeforeCheckpointRestart(GENESIS, "r0", DIGEST), "r1", 1);
    var next =
        GenerationModel.open(Optional.of(old), OptionalLong.of(0), permit, FACTS).registration();
    assertEquals(1, next.generation());
    var replay = GenerationModel.open(Optional.of(next), OptionalLong.of(0), permit, FACTS);
    assertTrue(replay.replayed());
    assertSame(next, replay.registration());
    var competing =
        permit(new ConnectorApi.BeforeCheckpointRestart(GENESIS, "r0", DIGEST), "r2", 1);
    assertThrows(
        ClientApi.ClientException.class,
        () -> GenerationModel.open(Optional.of(next), OptionalLong.of(0), competing, FACTS));
    assertEquals(0, old.generation());
  }

  @Test
  void restoreG7RetainsEpochFrameAndGeneration() {
    var state = bootstrap();
    for (int generation = 1; generation <= 7; generation++) {
      var p =
          permit(
              new ConnectorApi.BeforeCheckpointRestart(GENESIS, state.runId(), DIGEST),
              "r" + generation,
              generation);
      state =
          GenerationModel.open(Optional.of(state), OptionalLong.of(state.revision()), p, FACTS)
              .registration();
    }
    var point = new ConnectorApi.RestorePoint(1, DIGEST);
    state = GenerationModel.observeCompleted(state, point);
    byte[] frame = EpochFrames.canonical(epoch(state.generation(), 1), 1024);
    var restart =
        permit(new ConnectorApi.CheckpointRestart(point, state.runId(), DIGEST), "restored", 7);
    var restored =
        GenerationModel.open(Optional.of(state), OptionalLong.of(state.revision()), restart, FACTS)
            .registration();
    assertEquals(7, restored.generation());
    assertArrayEquals(frame, EpochFrames.canonical(epoch(restored.generation(), 1), 1024));
  }

  @Test
  void intentCompletionUnreadableInventoryAndUnprovenExitRejectGenerationChange() {
    var clean = bootstrap();
    var next = permit(new ConnectorApi.BeforeCheckpointRestart(GENESIS, "r0", DIGEST), "r1", 1);
    for (var state :
        java.util.List.of(
            GenerationModel.observeIntent(clean),
            GenerationModel.observeCompleted(clean, new ConnectorApi.RestorePoint(1, DIGEST)))) {
      assertThrows(
          ClientApi.ClientException.class,
          () ->
              GenerationModel.open(
                  Optional.of(state), OptionalLong.of(state.revision()), next, FACTS));
    }
    for (var facts :
        java.util.List.of(
            new GenerationModel.Facts(false, true, true, true),
            new GenerationModel.Facts(true, false, true, true),
            new GenerationModel.Facts(true, true, false, true))) {
      assertThrows(
          ClientApi.ClientException.class,
          () -> GenerationModel.open(Optional.of(clean), OptionalLong.of(0), next, facts));
    }
    assertThrows(
        ClientApi.ClientException.class,
        () ->
            GenerationModel.open(
                Optional.empty(),
                OptionalLong.empty(),
                clean.lastPermit(),
                new GenerationModel.Facts(false, true, true, true)));
  }

  @Test
  void overflowRevisionAndGenerationRejectWithoutWrapping() {
    var base = bootstrap();
    var maxPermit =
        permit(
            new ConnectorApi.BeforeCheckpointRestart(GENESIS, "prior", DIGEST),
            "max",
            Long.MAX_VALUE);
    var max =
        new GenerationModel.Registration(
            LINEAGE, GENESIS, "max", Long.MAX_VALUE, 0, Optional.empty(), false, maxPermit);
    var change =
        permit(
            new ConnectorApi.BeforeCheckpointRestart(GENESIS, "max", DIGEST),
            "next",
            Long.MAX_VALUE);
    assertEquals(
        ClientApi.FailureCode.CAPACITY,
        assertThrows(
                ClientApi.ClientException.class,
                () -> GenerationModel.open(Optional.of(max), OptionalLong.of(0), change, FACTS))
            .failure()
            .code());
    var revisionMax =
        new GenerationModel.Registration(
            LINEAGE, GENESIS, "r0", 0, Long.MAX_VALUE, Optional.empty(), false, base.lastPermit());
    assertThrows(ClientApi.ClientException.class, () -> GenerationModel.observeIntent(revisionMax));
    assertFalse(revisionMax.hasIntent());
  }

  @Test
  void stalePointDifferentStateAndSameRunDifferentPermitReject() {
    var state =
        GenerationModel.observeCompleted(bootstrap(), new ConnectorApi.RestorePoint(7, DIGEST));
    assertThrows(
        ClientApi.ClientException.class,
        () -> GenerationModel.observeCompleted(state, new ConnectorApi.RestorePoint(6, DIGEST)));
    var other = new ClientApi.Digest("sha256-v1", "sha256-v1:" + "1".repeat(64));
    assertThrows(
        ClientApi.ClientException.class,
        () -> GenerationModel.observeCompleted(state, new ConnectorApi.RestorePoint(7, other)));
    var sameRun =
        new ConnectorApi.ControlledRunPermit(
            LINEAGE, new ConnectorApi.BootstrapStart(GENESIS), "r0", 0, other);
    assertThrows(
        ClientApi.ClientException.class,
        () ->
            GenerationModel.open(
                Optional.of(state), OptionalLong.of(state.revision()), sameRun, FACTS));
    var olderRestore =
        permit(
            new ConnectorApi.CheckpointRestart(
                new ConnectorApi.RestorePoint(6, DIGEST), "r0", DIGEST),
            "next",
            0);
    assertThrows(
        ClientApi.ClientException.class,
        () ->
            GenerationModel.open(
                Optional.of(state), OptionalLong.of(state.revision()), olderRestore, FACTS));
  }
}
