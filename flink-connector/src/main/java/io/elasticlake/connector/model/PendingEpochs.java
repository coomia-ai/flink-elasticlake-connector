package io.elasticlake.connector.model;

import static io.elasticlake.client.api.v2.ClientApi.FailureCode.*;

import io.elasticlake.connector.api.v3.ConnectorApi;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** SPEC-26 §4.1 simulation; a retirement view is not real Flink or journal evidence. */
public final class PendingEpochs {
  public record Manager(
      ConnectorApi.EpochIdentity epoch,
      Optional<ConnectorApi.SealedRef> sealed,
      boolean finalized) {
    public Manager {
      ModelChecks.required(epoch, "PendingEpochs.epoch");
      ModelChecks.required(sealed, "PendingEpochs.sealed");
      ModelChecks.require(
          sealed.isEmpty() || sealed.get().epoch().equals(epoch),
          CONFLICT,
          "PendingEpochs.sealed",
          "epoch differs");
      ModelChecks.require(
          !finalized || sealed.isPresent(),
          CONFLICT,
          "PendingEpochs.finalized",
          "unsealed manager cannot be finalized");
    }
  }

  public record RetirementView(
      ConnectorApi.Lineage lineage,
      ConnectorApi.RestorePoint completedPoint,
      boolean completeInventory,
      Set<ConnectorApi.EpochIdentity> restorableManagers,
      Set<ConnectorApi.EpochIdentity> pendingJournal) {
    public RetirementView {
      ModelChecks.required(completedPoint, "PendingEpochs.completedPoint");
      ModelChecks.required(lineage, "PendingEpochs.lineage");
      restorableManagers = ModelChecks.set(restorableManagers, "PendingEpochs.restorableManagers");
      pendingJournal = ModelChecks.set(pendingJournal, "PendingEpochs.pendingJournal");
      ModelChecks.require(
          restorableManagers.stream().allMatch(e -> e.lineage().equals(lineage))
              && pendingJournal.stream().allMatch(e -> e.lineage().equals(lineage)),
          CONFLICT,
          "PendingEpochs.lineage",
          "inventory contains a different lineage binding");
    }
  }

  private final Map<ConnectorApi.EpochIdentity, Manager> managers;

  public PendingEpochs() {
    managers = Map.of();
  }

  private PendingEpochs(Map<ConnectorApi.EpochIdentity, Manager> managers) {
    this.managers = Map.copyOf(managers);
  }

  public Map<ConnectorApi.EpochIdentity, Manager> managers() {
    return managers;
  }

  public PendingEpochs reserveBeforeSeal(ConnectorApi.EpochIdentity epoch) {
    ModelChecks.required(epoch, "PendingEpochs.epoch");
    if (managers.containsKey(epoch)) return this;
    ModelChecks.require(
        managers.size() < 2,
        CAPACITY,
        "PendingEpochs.slots",
        "third epoch rejected before sealing");
    return replace(new Manager(epoch, Optional.empty(), false));
  }

  public PendingEpochs sealed(ConnectorApi.SealedRef sealed) {
    ModelChecks.required(sealed, "PendingEpochs.sealed");
    var prior = manager(sealed.epoch());
    ModelChecks.require(
        prior.sealed().isEmpty() || prior.sealed().get().equals(sealed),
        CONFLICT,
        "PendingEpochs.sealed",
        "original manifest differs");
    return replace(new Manager(prior.epoch(), Optional.of(sealed), prior.finalized()));
  }

  public PendingEpochs finalized(ConnectorApi.EpochIdentity epoch) {
    var prior = manager(epoch);
    return replace(new Manager(epoch, prior.sealed(), true));
  }

  public PendingEpochs retire(ConnectorApi.EpochIdentity epoch, RetirementView view) {
    manager(epoch);
    ModelChecks.required(view, "PendingEpochs.retirementView");
    ModelChecks.require(
        view.lineage().equals(epoch.lineage())
            && view.completeInventory()
            && view.completedPoint().completedCheckpointId() >= epoch.originCheckpointId()
            && !view.restorableManagers().contains(epoch)
            && !view.pendingJournal().contains(epoch),
        CONFLICT,
        "PendingEpochs.retirement",
        "epoch may still require a journal or framework callback");
    var next = new HashMap<>(managers);
    next.remove(epoch);
    return new PendingEpochs(next);
  }

  private Manager manager(ConnectorApi.EpochIdentity epoch) {
    ModelChecks.required(epoch, "PendingEpochs.epoch");
    ModelChecks.require(
        managers.containsKey(epoch), CONFLICT, "PendingEpochs.epoch", "epoch was not reserved");
    return managers.get(epoch);
  }

  private PendingEpochs replace(Manager manager) {
    var next = new HashMap<>(managers);
    next.put(manager.epoch(), manager);
    return new PendingEpochs(next);
  }
}
