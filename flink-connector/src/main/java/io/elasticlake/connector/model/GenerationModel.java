package io.elasticlake.connector.model;

import static io.elasticlake.client.api.v2.ClientApi.FailureCode.*;

import io.elasticlake.connector.api.v3.ConnectorApi;
import java.util.Optional;
import java.util.OptionalLong;

/** Pure journal transition model. Simulation facts are not permit, stop or CAS authority. */
public final class GenerationModel {
  private GenerationModel() {}

  /** Empty means no prior registration, plan, intent, progress or outbox in the lineage scope. */
  public record Facts(
      boolean completeInventory,
      boolean priorRunStopped,
      boolean sourceRetained,
      boolean bootstrapInventoryEmpty) {}

  public record Registration(
      ConnectorApi.Lineage lineage,
      ConnectorApi.GenesisIdentity genesis,
      String runId,
      long generation,
      long revision,
      Optional<ConnectorApi.RestorePoint> completed,
      boolean hasIntent,
      ConnectorApi.ControlledRunPermit lastPermit) {
    public Registration {
      ModelChecks.required(lineage, "GenerationModel.lineage");
      ModelChecks.required(genesis, "GenerationModel.genesis");
      ModelChecks.required(runId, "GenerationModel.runId");
      ModelChecks.nonnegative(generation, "GenerationModel.generation");
      ModelChecks.nonnegative(revision, "GenerationModel.revision");
      ModelChecks.required(completed, "GenerationModel.completed");
      ModelChecks.required(lastPermit, "GenerationModel.lastPermit");
      ModelChecks.require(
          lastPermit.lineage().equals(lineage)
              && lastPermit.runId().equals(runId)
              && lastPermit.planningGeneration() == generation,
          CONFLICT,
          "GenerationModel.lastPermit",
          "registration binding differs");
      ConnectorApi.GenesisIdentity permitGenesis = null;
      if (lastPermit.start() instanceof ConnectorApi.BootstrapStart start) {
        permitGenesis = start.genesis();
        ModelChecks.require(
            generation == 0,
            CONFLICT,
            "GenerationModel.generation",
            "bootstrap generation must be zero");
      } else if (lastPermit.start() instanceof ConnectorApi.BeforeCheckpointRestart start) {
        permitGenesis = start.genesis();
        ModelChecks.require(
            generation > 0 && !start.priorRunId().equals(runId),
            CONFLICT,
            "GenerationModel.generation",
            "pre-checkpoint restart needs a new run and positive generation");
      }
      ModelChecks.require(
          permitGenesis == null || permitGenesis.equals(genesis),
          CONFLICT,
          "GenerationModel.genesis",
          "registration and permit source start differ");
      if (lastPermit.start() instanceof ConnectorApi.CheckpointRestart start) {
        ModelChecks.require(
            !start.priorRunId().equals(runId)
                && completed.isPresent()
                && completed.get().completedCheckpointId() >= start.point().completedCheckpointId()
                && (completed.get().completedCheckpointId() != start.point().completedCheckpointId()
                    || completed.get().equals(start.point())),
            CONFLICT,
            "GenerationModel.completed",
            "registration contradicts completed start point");
      }
    }
  }

  public record Result(Registration registration, boolean replayed) {
    public Result {
      ModelChecks.required(registration, "GenerationModel.result");
    }
  }

  public static Result open(
      Optional<Registration> current,
      OptionalLong expectedRevision,
      ConnectorApi.ControlledRunPermit permit,
      Facts facts) {
    ModelChecks.required(current, "GenerationModel.current");
    ModelChecks.required(expectedRevision, "GenerationModel.expectedRevision");
    ModelChecks.required(permit, "GenerationModel.permit");
    ModelChecks.required(facts, "GenerationModel.facts");
    ModelChecks.require(
        facts.completeInventory(),
        CONFLICT,
        "GenerationModel.inventory",
        "inventory is incomplete or unreadable");
    if (current.isEmpty()) {
      ModelChecks.require(
          facts.bootstrapInventoryEmpty()
              && expectedRevision.isEmpty()
              && permit.start() instanceof ConnectorApi.BootstrapStart
              && permit.planningGeneration() == 0,
          CONFLICT,
          "GenerationModel.bootstrap",
          "bootstrap requires empty inventory, create-only genesis and generation zero");
      var start = (ConnectorApi.BootstrapStart) permit.start();
      return new Result(
          new Registration(
              permit.lineage(),
              start.genesis(),
              permit.runId(),
              0,
              0,
              Optional.empty(),
              false,
              permit),
          false);
    }
    var prior = current.get();
    ModelChecks.require(
        prior.lineage().equals(permit.lineage()),
        CONFLICT,
        "GenerationModel.lineage",
        "lineage/UID/transform/table/profile differs");
    if (prior.lastPermit().equals(permit)) return new Result(prior, true);
    ModelChecks.require(
        !prior.runId().equals(permit.runId()),
        CONFLICT,
        "GenerationModel.runId",
        "same run has a different permit");
    ModelChecks.require(
        expectedRevision.isPresent() && expectedRevision.getAsLong() == prior.revision(),
        CONFLICT,
        "GenerationModel.expectedRevision",
        "exact CAS revision differs");
    ModelChecks.require(
        facts.priorRunStopped(), CONFLICT, "GenerationModel.stopped", "prior run exit is unproven");
    long generation;
    if (permit.start() instanceof ConnectorApi.BeforeCheckpointRestart before) {
      ModelChecks.require(
          before.priorRunId().equals(prior.runId()) && before.genesis().equals(prior.genesis()),
          CONFLICT,
          "GenerationModel.genesis",
          "prior run or original source start differs");
      ModelChecks.require(
          !prior.hasIntent() && prior.completed().isEmpty() && facts.sourceRetained(),
          CONFLICT,
          "GenerationModel.beforeCheckpoint",
          "intent/completion exists or source replay is unproven");
      generation = ModelChecks.add(prior.generation(), 1, "GenerationModel.generation");
    } else if (permit.start() instanceof ConnectorApi.CheckpointRestart restart) {
      ModelChecks.require(
          restart.priorRunId().equals(prior.runId())
              && prior.completed().isPresent()
              && restart.point().equals(prior.completed().get())
              && facts.sourceRetained(),
          CONFLICT,
          "GenerationModel.restorePoint",
          "original completed point/source retention differs");
      generation = prior.generation();
    } else {
      ModelChecks.require(false, CONFLICT, "GenerationModel.bootstrap", "genesis already exists");
      throw new AssertionError("unreachable");
    }
    ModelChecks.require(
        permit.planningGeneration() == generation,
        CONFLICT,
        "GenerationModel.permitGeneration",
        "authoritative generation differs");
    return new Result(
        new Registration(
            prior.lineage(),
            prior.genesis(),
            permit.runId(),
            generation,
            ModelChecks.add(prior.revision(), 1, "GenerationModel.revision"),
            prior.completed(),
            prior.hasIntent(),
            permit),
        false);
  }

  public static Registration observeIntent(Registration prior) {
    ModelChecks.required(prior, "GenerationModel.prior");
    if (prior.hasIntent()) return prior;
    return new Registration(
        prior.lineage(),
        prior.genesis(),
        prior.runId(),
        prior.generation(),
        ModelChecks.add(prior.revision(), 1, "GenerationModel.revision"),
        prior.completed(),
        true,
        prior.lastPermit());
  }

  public static Registration observeCompleted(Registration prior, ConnectorApi.RestorePoint point) {
    ModelChecks.required(prior, "GenerationModel.prior");
    ModelChecks.required(point, "GenerationModel.point");
    if (prior.completed().isPresent()) {
      var existing = prior.completed().get();
      ModelChecks.require(
          point.completedCheckpointId() >= existing.completedCheckpointId(),
          CONFLICT,
          "GenerationModel.point",
          "completed point regressed");
      if (point.completedCheckpointId() == existing.completedCheckpointId()) {
        ModelChecks.require(
            point.equals(existing),
            CONFLICT,
            "GenerationModel.point",
            "same checkpoint has different state identity");
        return prior;
      }
    }
    return new Registration(
        prior.lineage(),
        prior.genesis(),
        prior.runId(),
        prior.generation(),
        ModelChecks.add(prior.revision(), 1, "GenerationModel.revision"),
        Optional.of(point),
        prior.hasIntent(),
        prior.lastPermit());
  }
}
