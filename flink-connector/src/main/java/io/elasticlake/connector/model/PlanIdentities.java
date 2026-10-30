package io.elasticlake.connector.model;

import static io.elasticlake.client.api.v2.ClientApi.FailureCode.CONFLICT;

import io.elasticlake.connector.api.v3.ConnectorApi;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Keeps original C/key/first-attempt/payload references; recovery never derives replacement keys.
 */
public final class PlanIdentities {
  private PlanIdentities() {}

  public record Pinned(
      ConnectorApi.CheckpointPlan plan, List<ConnectorApi.SinkMutation> originalInputs) {
    public Pinned {
      ModelChecks.required(plan, "PlanIdentities.plan");
      originalInputs = ModelChecks.list(originalInputs, "PlanIdentities.originalInputs");
    }
  }

  public static List<ConnectorApi.PartPlan> originalParts(Pinned pinned, int maxFrameBytes) {
    ModelChecks.required(pinned, "PlanIdentities.pinned");
    var plan = pinned.plan();
    byte[] canonical = EpochFrames.canonical(plan.epoch(), maxFrameBytes);
    ModelChecks.require(
        plan.epoch().coverage().equals(plan.coverage().canonicalDigest()),
        CONFLICT,
        "PlanIdentities.coverage",
        "plan coverage differs from epoch");
    var parts = new ArrayList<ConnectorApi.PartPlan>();
    for (var table : plan.tables()) {
      ModelChecks.require(
          table.seal().context().coverage() == io.elasticlake.client.api.v2.ClientApi.Coverage.EXACT
              && Arrays.equals(canonical, table.seal().context().checkpointCanonical().copy()),
          CONFLICT,
          "PlanIdentities.context",
          "sealed context differs from original epoch");
      ModelChecks.require(
          table
              .seal()
              .orderedParts()
              .equals(table.parts().stream().map(ConnectorApi.PartPlan::fact).toList()),
          CONFLICT,
          "PlanIdentities.parts",
          "sealed parts differ from originals");
      for (var part : table.parts()) {
        ModelChecks.require(
            part.fact().table().equals(table.seal().table())
                && part.fact().context().equals(table.seal().context()),
            CONFLICT,
            "PlanIdentities.part",
            "original table/context mismatch");
        parts.add(part);
      }
    }
    return List.copyOf(parts);
  }
}
