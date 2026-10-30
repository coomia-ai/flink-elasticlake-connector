package io.elasticlake.consumer;

import static io.elasticlake.connector.api.v3.ConnectorApi.*;

import io.elasticlake.client.api.v2.ClientApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class LocalContractRegression {
  private static ClientApi.Digest digest() {
    return new ClientApi.Digest("sha256-v1", "sha256-v1:" + "0".repeat(64));
  }

  private static ClientApi.Bytes bytes() {
    return new ClientApi.Bytes() {
      public int size() {
        return 0;
      }

      public byte[] copy() {
        return new byte[0];
      }
    };
  }

  static void verify() {
    var d = digest();
    var lineage = new Lineage("l", "u", d, d, d);
    var epoch = new EpochIdentity(lineage, 0, 1, d);
    var sealed = new SealedRef(epoch, new BlobRef(ObjectKind.SEALED, "key", 0, d));
    var position = new SourcePosition(new SourceDomain("s", "p", "c", 0), bytes(), 0);
    var output =
        new OutputMapping(0, "target", new MutationIdentity("p", "s", "m"), d, Optional.empty());
    var permit =
        new ControlledRunPermit(
            lineage, new BootstrapStart(new GenesisIdentity("g", bytes(), d)), "r", 0, d);
    var accepted = new ArrayList<String>();
    check(
        accepted,
        "FILTERED output",
        () ->
            new InputDisposition(position, Disposition.FILTERED, List.of(output), Optional.empty()),
        ClientApi.FailureCode.INVALID_INPUT);
    check(
        accepted,
        "DEDUP output",
        () ->
            new InputDisposition(
                position, Disposition.DEDUPLICATED, List.of(output), Optional.empty()),
        ClientApi.FailureCode.INVALID_INPUT);
    check(
        accepted,
        "DLQ missing identity",
        () -> new InputDisposition(position, Disposition.DLQ, List.of(), Optional.empty()),
        ClientApi.FailureCode.INVALID_INPUT);
    check(
        accepted,
        "DLQ mixed output",
        () -> new InputDisposition(position, Disposition.DLQ, List.of(output), Optional.of(d)),
        ClientApi.FailureCode.UNSUPPORTED_CAPABILITY);
    check(
        accepted,
        "ACKED missing ack",
        () -> new PartProgress(0, 0, PartStage.ACKED, Optional.empty()),
        ClientApi.FailureCode.INVALID_INPUT);
    check(
        accepted,
        "LANDED missing ack",
        () -> new PartProgress(0, 0, PartStage.LANDED, Optional.empty()),
        ClientApi.FailureCode.INVALID_INPUT);
    check(
        accepted,
        "report missing outbox",
        () -> new Progress(0, sealed, List.of(), List.of(), Optional.empty(), true),
        ClientApi.FailureCode.INVALID_INPUT);
    check(
        accepted,
        "restore missing reason",
        () -> new RestoreDecision(false, "", permit, List.of()),
        ClientApi.FailureCode.INVALID_INPUT);
    if (!accepted.isEmpty())
      throw new AssertionError(
          "LOCAL_CONTRACT_INVALID_ACCEPTED count=" + accepted.size() + " " + accepted);
    System.out.println("LOCAL_CONTRACT_REGRESSIONS_OK count=8");
  }

  private static void check(
      List<String> accepted, String name, Runnable action, ClientApi.FailureCode code) {
    try {
      action.run();
      accepted.add(name);
    } catch (ClientApi.ClientException expected) {
      if (expected.failure().code() != code)
        throw new AssertionError(name + " wrong failure code", expected);
    }
  }

  public static void main(String[] args) {
    verify();
  }
}
