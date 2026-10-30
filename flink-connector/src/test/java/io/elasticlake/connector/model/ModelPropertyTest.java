package io.elasticlake.connector.model;

import static io.elasticlake.connector.model.ModelFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import io.elasticlake.client.api.v2.ClientApi;
import io.elasticlake.connector.api.v3.ConnectorApi;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;
import net.jqwik.api.*;
import net.jqwik.api.constraints.LongRange;

class ModelPropertyTest {
  @Provide
  Arbitrary<List<Integer>> commands() {
    return Arbitraries.integers().between(0, 7).list().ofMinSize(1).ofMaxSize(100);
  }

  @Provide
  Arbitrary<String> identifiers() {
    return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(0).ofMaxLength(200);
  }

  @Provide
  Arbitrary<byte[]> coverages() {
    byte[] ones = new byte[32];
    Arrays.fill(ones, (byte) 0xff);
    return Arbitraries.oneOf(
        Arbitraries.just(new byte[32]),
        Arbitraries.just(ones),
        Arbitraries.bytes().array(byte[].class).ofSize(32));
  }

  @Property(tries = 500, seed = "20261028161")
  void checkpointFrameMatchesSeparateStreamEncoder(
      @ForAll("identifiers") String id,
      @ForAll @LongRange(min = 0) long generation,
      @ForAll @LongRange(min = 0) long checkpoint,
      @ForAll("coverages") byte[] coverage) {
    var e =
        new ConnectorApi.EpochIdentity(
            new ConnectorApi.Lineage(id, "uid", DIGEST, DIGEST, DIGEST),
            generation,
            checkpoint,
            new ClientApi.Digest(
                "sha256-v1", "sha256-v1:" + java.util.HexFormat.of().formatHex(coverage)));
    assertArrayEquals(frameOracle(e), EpochFrames.canonical(e, 4096));
    byte[] frame = EpochFrames.canonical(e, 4096);
    assertArrayEquals(coverage, Arrays.copyOfRange(frame, frame.length - 32, frame.length));
    byte[] otherCoverage = coverage.clone();
    otherCoverage[0] ^= 1;
    var other =
        new ConnectorApi.EpochIdentity(
            e.lineage(),
            generation,
            checkpoint,
            new ClientApi.Digest(
                "sha256-v1", "sha256-v1:" + java.util.HexFormat.of().formatHex(otherCoverage)));
    assertFalse(Arrays.equals(frame, EpochFrames.canonical(other, 4096)));
    frame[0] ^= 1;
    assertArrayEquals(frameOracle(e), EpochFrames.canonical(e, 4096));
  }

  @Property(tries = 500, seed = "20261028162")
  void generationTransitionsMatchScalarJournalOracle(@ForAll("commands") List<Integer> commands) {
    var state = bootstrap();
    long generation = 0;
    long revision = 0;
    boolean intent = false;
    ConnectorApi.RestorePoint completed = null;
    String run = "r0";
    var last = state.lastPermit();
    int step = 0;
    for (int command : commands) {
      step++;
      var before = state;
      if (command == 0) {
        var replay = GenerationModel.open(Optional.of(state), OptionalLong.empty(), last, FACTS);
        assertTrue(replay.replayed());
        assertSame(state, replay.registration());
      } else if (command == 1 || command == 4 || command == 7) {
        var next =
            permit(
                new ConnectorApi.BeforeCheckpointRestart(GENESIS, run, DIGEST),
                "run-" + step,
                generation + (command == 7 ? 2 : 1));
        var expected = command == 4 ? OptionalLong.empty() : OptionalLong.of(revision);
        if (command == 1 && !intent && completed == null) {
          byte[] priorFrame = EpochFrames.canonical(epoch(generation, 1), 1024);
          state = GenerationModel.open(Optional.of(state), expected, next, FACTS).registration();
          generation++;
          revision++;
          run = next.runId();
          last = next;
          assertFalse(Arrays.equals(priorFrame, EpochFrames.canonical(epoch(generation, 1), 1024)));
        } else
          assertThrows(
              ClientApi.ClientException.class,
              () -> GenerationModel.open(Optional.of(before), expected, next, FACTS));
      } else if (command == 2) {
        state = GenerationModel.observeIntent(state);
        if (!intent) revision++;
        intent = true;
      } else if (command == 3) {
        completed = new ConnectorApi.RestorePoint(step, DIGEST);
        state = GenerationModel.observeCompleted(state, completed);
        revision++;
      } else if (command == 5) {
        var point = completed == null ? new ConnectorApi.RestorePoint(step, DIGEST) : completed;
        var next =
            permit(
                new ConnectorApi.CheckpointRestart(point, run, DIGEST),
                "restore-" + step,
                generation);
        long expected = revision;
        if (completed == null)
          assertThrows(
              ClientApi.ClientException.class,
              () ->
                  GenerationModel.open(
                      Optional.of(before), OptionalLong.of(expected), next, FACTS));
        else {
          state =
              GenerationModel.open(Optional.of(state), OptionalLong.of(revision), next, FACTS)
                  .registration();
          revision++;
          run = next.runId();
          last = next;
        }
      } else {
        var same = last;
        assertThrows(
            ClientApi.ClientException.class,
            () ->
                GenerationModel.open(
                    Optional.of(before),
                    OptionalLong.empty(),
                    same,
                    new GenerationModel.Facts(false, true, true, true)));
      }
      assertEquals(generation, state.generation());
      assertEquals(revision, state.revision());
      assertEquals(run, state.runId());
      assertEquals(intent, state.hasIntent());
      assertEquals(Optional.ofNullable(completed), state.completed());
      assertEquals(last, state.lastPermit());
      assertEquals(GENESIS, state.genesis());
    }
  }

  @Property(tries = 500, seed = "20261028163")
  void slotsMatchNaiveInventoryListAcrossSealFinalizeAndRetirement(
      @ForAll("commands") List<Integer> commands) {
    var actual = new PendingEpochs();
    var active = new ArrayList<Integer>();
    var sealed = new HashSet<Integer>();
    var finalized = new HashSet<Integer>();
    int step = 0;
    for (int command : commands) {
      int id = ++step % 4 + 1;
      var e = epoch(0, id);
      var prior = actual;
      int action = command % 6;
      boolean expected =
          switch (action) {
            case 0 -> active.contains(id) || active.size() < 2;
            case 1 -> active.contains(id);
            case 2 -> active.contains(id) && sealed.contains(id);
            case 3 -> active.contains(id);
            default -> false;
          };
      if (expected) {
        switch (action) {
          case 0 -> {
            actual = actual.reserveBeforeSeal(e);
            if (!active.contains(id)) active.add(id);
          }
          case 1 -> {
            actual = actual.sealed(ModelFixtures.sealed(e));
            sealed.add(id);
          }
          case 2 -> {
            actual = actual.finalized(e);
            finalized.add(id);
          }
          case 3 -> {
            actual =
                actual.retire(
                    e,
                    new PendingEpochs.RetirementView(
                        LINEAGE,
                        new ConnectorApi.RestorePoint(100, DIGEST),
                        true,
                        Set.of(),
                        Set.of()));
            active.remove(Integer.valueOf(id));
            sealed.remove(id);
            finalized.remove(id);
          }
          default -> throw new AssertionError(action);
        }
      } else {
        assertThrows(
            ClientApi.ClientException.class,
            () -> {
              switch (action) {
                case 0 -> prior.reserveBeforeSeal(e);
                case 1 -> prior.sealed(ModelFixtures.sealed(e));
                case 2 -> prior.finalized(e);
                default ->
                    prior.retire(
                        e,
                        new PendingEpochs.RetirementView(
                            LINEAGE,
                            new ConnectorApi.RestorePoint(100, DIGEST),
                            action != 4,
                            action == 5 ? Set.of(e) : Set.of(),
                            Set.of()));
              }
            });
      }
      assertEquals(
          active.stream().map(i -> epoch(0, i)).collect(Collectors.toSet()),
          actual.managers().keySet());
      for (int present : active) {
        var manager = actual.managers().get(epoch(0, present));
        assertEquals(sealed.contains(present), manager.sealed().isPresent());
        assertEquals(finalized.contains(present), manager.finalized());
      }
    }
  }

  record Charge(long nanos, long wires) {}

  @Provide
  Arbitrary<List<Charge>> charges() {
    return Combinators.combine(
            Arbitraries.longs().between(0, 35_000_000_000L), Arbitraries.longs().between(0, 2000))
        .as(Charge::new)
        .list()
        .ofMinSize(1)
        .ofMaxSize(40);
  }

  @Property(tries = 500, seed = "20261028164")
  void callbackChargesMatchUnboundedArithmeticOracle(@ForAll("charges") List<Charge> charges) {
    var budget = new CommitBudgets.Callback(epoch(0, 1), 0, 0);
    BigInteger nanos = BigInteger.ZERO;
    BigInteger wires = BigInteger.ZERO;
    for (var charge : charges) {
      var time = nanos.add(BigInteger.valueOf(charge.nanos()));
      var attempts = wires.add(BigInteger.valueOf(charge.wires()));
      if (time.compareTo(BigInteger.valueOf(90_000_000_000L)) > 0
          || attempts.compareTo(BigInteger.valueOf(4096)) > 0) {
        var unchanged = budget;
        assertThrows(
            ClientApi.ClientException.class,
            () -> unchanged.charge(charge.nanos(), charge.wires()));
        assertEquals(nanos.longValueExact(), budget.elapsedNanos());
        assertEquals(wires.longValueExact(), budget.wireAttempts());
        break;
      }
      budget = budget.charge(charge.nanos(), charge.wires());
      nanos = time;
      wires = attempts;
      assertEquals(nanos.longValueExact(), budget.elapsedNanos());
      assertEquals(wires.longValueExact(), budget.wireAttempts());
    }
  }
}
