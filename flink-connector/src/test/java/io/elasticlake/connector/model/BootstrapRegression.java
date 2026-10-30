package io.elasticlake.connector.model;

import io.elasticlake.client.api.v2.ClientApi;
import io.elasticlake.connector.api.v3.ConnectorApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

/** Standalone before/after regression against the original and revised model JARs. */
public final class BootstrapRegression {
  private BootstrapRegression() {}

  public static void verify() throws ReflectiveOperationException {
    var digest = new ClientApi.Digest("sha256-v1", "sha256-v1:" + "0".repeat(64));
    var lineage = new ConnectorApi.Lineage("l", "u", digest, digest, digest);
    ClientApi.Bytes bytes =
        new ClientApi.Bytes() {
          public int size() {
            return 0;
          }

          public byte[] copy() {
            return new byte[0];
          }
        };
    var genesis = new ConnectorApi.GenesisIdentity("original", bytes, digest);
    var other = new ConnectorApi.GenesisIdentity("different", bytes, digest);
    var bootstrap =
        new ConnectorApi.ControlledRunPermit(
            lineage, new ConnectorApi.BootstrapStart(genesis), "r0", 0, digest);
    var before =
        new ConnectorApi.ControlledRunPermit(
            lineage,
            new ConnectorApi.BeforeCheckpointRestart(genesis, "r0", digest),
            "r1",
            1,
            digest);
    GenerationModel.Facts facts;
    try {
      facts =
          GenerationModel.Facts.class
              .getConstructor(boolean.class, boolean.class, boolean.class, boolean.class)
              .newInstance(true, true, true, false);
    } catch (NoSuchMethodException originalModel) {
      facts =
          GenerationModel.Facts.class
              .getConstructor(boolean.class, boolean.class, boolean.class)
              .newInstance(true, true, true);
    }
    var orphanFacts = facts;
    var accepted = new ArrayList<String>();
    expectRejected(
        accepted,
        "orphan inventory at bootstrap",
        () -> GenerationModel.open(Optional.empty(), OptionalLong.empty(), bootstrap, orphanFacts));
    expectRejected(
        accepted,
        "bootstrap genesis mismatch",
        () ->
            new GenerationModel.Registration(
                lineage, other, "r0", 0, 0, Optional.empty(), false, bootstrap));
    expectRejected(
        accepted,
        "before-checkpoint genesis mismatch",
        () ->
            new GenerationModel.Registration(
                lineage, other, "r1", 1, 0, Optional.empty(), false, before));
    var point = new ConnectorApi.RestorePoint(7, digest);
    var checkpoint =
        new ConnectorApi.ControlledRunPermit(
            lineage,
            new ConnectorApi.CheckpointRestart(point, "prior", digest),
            "checkpoint",
            0,
            digest);
    var otherDigest = new ClientApi.Digest("sha256-v1", "sha256-v1:" + "1".repeat(64));
    expectRejected(
        accepted,
        "checkpoint start without completion",
        () ->
            new GenerationModel.Registration(
                lineage, genesis, "checkpoint", 0, 0, Optional.empty(), false, checkpoint));
    expectRejected(
        accepted,
        "checkpoint start ahead of completion",
        () ->
            new GenerationModel.Registration(
                lineage,
                genesis,
                "checkpoint",
                0,
                0,
                Optional.of(new ConnectorApi.RestorePoint(6, digest)),
                false,
                checkpoint));
    expectRejected(
        accepted,
        "same checkpoint with different state",
        () ->
            new GenerationModel.Registration(
                lineage,
                genesis,
                "checkpoint",
                0,
                0,
                Optional.of(new ConnectorApi.RestorePoint(7, otherDigest)),
                false,
                checkpoint));
    new GenerationModel.Registration(
        lineage, genesis, "checkpoint", 0, 0, Optional.of(point), false, checkpoint);
    new GenerationModel.Registration(
        lineage,
        genesis,
        "checkpoint",
        0,
        0,
        Optional.of(new ConnectorApi.RestorePoint(8, otherDigest)),
        false,
        checkpoint);
    var wrongBootstrap =
        new ConnectorApi.ControlledRunPermit(
            lineage, new ConnectorApi.BootstrapStart(genesis), "r0", 7, digest);
    expectRejected(
        accepted,
        "bootstrap generation is not zero",
        () ->
            new GenerationModel.Registration(
                lineage, genesis, "r0", 7, 0, Optional.empty(), false, wrongBootstrap));
    var wrongBefore =
        new ConnectorApi.ControlledRunPermit(
            lineage,
            new ConnectorApi.BeforeCheckpointRestart(genesis, "prior", digest),
            "r1",
            0,
            digest);
    expectRejected(
        accepted,
        "pre-checkpoint generation is zero",
        () ->
            new GenerationModel.Registration(
                lineage, genesis, "r1", 0, 0, Optional.empty(), false, wrongBefore));
    var selfBefore =
        new ConnectorApi.ControlledRunPermit(
            lineage,
            new ConnectorApi.BeforeCheckpointRestart(genesis, "r1", digest),
            "r1",
            1,
            digest);
    expectRejected(
        accepted,
        "pre-checkpoint prior run equals new run",
        () ->
            new GenerationModel.Registration(
                lineage, genesis, "r1", 1, 0, Optional.empty(), false, selfBefore));
    var selfCheckpoint =
        new ConnectorApi.ControlledRunPermit(
            lineage,
            new ConnectorApi.CheckpointRestart(point, "checkpoint", digest),
            "checkpoint",
            0,
            digest);
    expectRejected(
        accepted,
        "checkpoint prior run equals new run",
        () ->
            new GenerationModel.Registration(
                lineage, genesis, "checkpoint", 0, 0, Optional.of(point), false, selfCheckpoint));
    if (!accepted.isEmpty())
      throw new AssertionError(
          "BOOTSTRAP_REGRESSION_INVALID_ACCEPTED count=" + accepted.size() + " " + accepted);
    System.out.println("BOOTSTRAP_REGRESSIONS_OK count=10 java=" + Runtime.version());
  }

  private static void expectRejected(List<String> accepted, String name, Runnable action) {
    try {
      action.run();
      accepted.add(name);
    } catch (ClientApi.ClientException expected) {
      if (expected.failure().code() != ClientApi.FailureCode.CONFLICT) throw expected;
    }
  }

  public static void main(String[] args) throws ReflectiveOperationException {
    verify();
  }
}
