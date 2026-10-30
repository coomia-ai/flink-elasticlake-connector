package io.elasticlake.connector.model;

import static io.elasticlake.client.api.v2.ClientApi.FailureCode.*;

import io.elasticlake.connector.api.v3.ConnectorApi;
import java.util.ArrayList;
import java.util.List;

/** Pure charging model, with externally supplied elapsed time; no clock or IO exit authority. */
public final class CommitBudgets {
  public static final long CALLBACK_NANOS = 90_000_000_000L;
  public static final int CALLBACK_WIRES = 4096;
  public static final long DRAIN_NANOS = 180_000_000_000L;
  public static final int DRAIN_WIRES = 8192;
  public static final long INITIALIZATION_NANOS = 240_000_000_000L;
  public static final long FRAMEWORK_NANOS = 50_000_000_000L;
  public static final long CLEANUP_NANOS = 10_000_000_000L;

  private CommitBudgets() {}

  public record Callback(ConnectorApi.EpochIdentity epoch, long elapsedNanos, long wireAttempts) {
    public Callback {
      ModelChecks.required(epoch, "CommitBudgets.epoch");
      ModelChecks.nonnegative(elapsedNanos, "CommitBudgets.elapsedNanos");
      ModelChecks.nonnegative(wireAttempts, "CommitBudgets.wireAttempts");
      ModelChecks.require(
          elapsedNanos <= CALLBACK_NANOS,
          DEADLINE,
          "CommitBudgets.callback",
          "90 second callback limit exceeded");
      ModelChecks.require(
          wireAttempts <= CALLBACK_WIRES,
          CAPACITY,
          "CommitBudgets.callback",
          "4096 wire callback limit exceeded");
    }

    public Callback charge(long elapsedDelta, long wireDelta) {
      ModelChecks.nonnegative(elapsedDelta, "CommitBudgets.elapsedDelta");
      ModelChecks.nonnegative(wireDelta, "CommitBudgets.wireDelta");
      return new Callback(
          epoch,
          ModelChecks.add(elapsedNanos, elapsedDelta, "CommitBudgets.elapsedNanos"),
          ModelChecks.add(wireAttempts, wireDelta, "CommitBudgets.wireAttempts"));
    }
  }

  public record Drain(List<Callback> callbacks) {
    public Drain {
      callbacks = ModelChecks.list(callbacks, "CommitBudgets.callbacks");
      ModelChecks.require(
          callbacks.size() <= 2,
          CAPACITY,
          "CommitBudgets.managers",
          "third callback manager rejected");
      ModelChecks.require(
          callbacks.stream().map(Callback::epoch).distinct().count() == callbacks.size(),
          CONFLICT,
          "CommitBudgets.managers",
          "callback budget cannot be refreshed for the same manager");
      long elapsed = 0;
      long attempts = 0;
      for (var callback : callbacks) {
        elapsed = ModelChecks.add(elapsed, callback.elapsedNanos(), "CommitBudgets.drainElapsed");
        attempts =
            ModelChecks.add(attempts, callback.wireAttempts(), "CommitBudgets.drainAttempts");
      }
      ModelChecks.require(
          elapsed <= DRAIN_NANOS,
          DEADLINE,
          "CommitBudgets.drain",
          "180 second drain limit exceeded");
      ModelChecks.require(
          attempts <= DRAIN_WIRES,
          CAPACITY,
          "CommitBudgets.drain",
          "8192 wire drain limit exceeded");
    }

    public Drain start(ConnectorApi.EpochIdentity epoch) {
      var next = new ArrayList<>(callbacks);
      next.add(new Callback(epoch, 0, 0));
      return new Drain(next);
    }

    public Drain charge(ConnectorApi.EpochIdentity epoch, long elapsedDelta, long wireDelta) {
      var next = new ArrayList<>(callbacks);
      int index = -1;
      for (int i = 0; i < next.size(); i++) if (next.get(i).epoch().equals(epoch)) index = i;
      ModelChecks.require(
          index >= 0, CONFLICT, "CommitBudgets.epoch", "manager has not entered this drain");
      next.set(index, next.get(index).charge(elapsedDelta, wireDelta));
      return new Drain(next);
    }

    public long elapsedNanos() {
      return callbacks.stream().mapToLong(Callback::elapsedNanos).sum();
    }

    public long wireAttempts() {
      return callbacks.stream().mapToLong(Callback::wireAttempts).sum();
    }
  }

  public record Initialization(long frameworkNanos, Drain drain, long cleanupNanos) {
    public Initialization {
      ModelChecks.nonnegative(frameworkNanos, "CommitBudgets.frameworkNanos");
      ModelChecks.nonnegative(cleanupNanos, "CommitBudgets.cleanupNanos");
      ModelChecks.required(drain, "CommitBudgets.drain");
      ModelChecks.require(
          frameworkNanos <= FRAMEWORK_NANOS
              && cleanupNanos <= CLEANUP_NANOS
              && ModelChecks.add(
                      ModelChecks.add(
                          frameworkNanos, drain.elapsedNanos(), "CommitBudgets.initialization"),
                      cleanupNanos,
                      "CommitBudgets.initialization")
                  <= INITIALIZATION_NANOS,
          DEADLINE,
          "CommitBudgets.initialization",
          "240 second initialization envelope exceeded");
    }
  }
}
