package io.elasticlake.connector.model;

import io.elasticlake.client.api.v2.ClientApi;
import io.elasticlake.connector.api.v3.ConnectorApi;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

final class ModelFixtures {
  static final ClientApi.Digest DIGEST = digest("sha256-v1");
  static final ConnectorApi.Lineage LINEAGE =
      new ConnectorApi.Lineage("lineage", "uid", DIGEST, DIGEST, DIGEST);
  static final ConnectorApi.GenesisIdentity GENESIS =
      new ConnectorApi.GenesisIdentity("registration", bytes(new byte[] {1}), DIGEST);
  static final GenerationModel.Facts FACTS = new GenerationModel.Facts(true, true, true, true);

  static ClientApi.Digest digest(String algorithm) {
    return new ClientApi.Digest(algorithm, algorithm + ":" + "0".repeat(64));
  }

  static ClientApi.Bytes bytes(byte[] value) {
    return new ClientApi.Bytes() {
      public int size() {
        return value.length;
      }

      public byte[] copy() {
        return value;
      }
    };
  }

  static ConnectorApi.EpochIdentity epoch(long generation, long checkpoint) {
    return new ConnectorApi.EpochIdentity(LINEAGE, generation, checkpoint, DIGEST);
  }

  static ConnectorApi.ControlledRunPermit permit(
      ConnectorApi.RunStart start, String run, long generation) {
    return new ConnectorApi.ControlledRunPermit(LINEAGE, start, run, generation, DIGEST);
  }

  static GenerationModel.Registration bootstrap() {
    return GenerationModel.open(
            Optional.empty(),
            OptionalLong.empty(),
            permit(new ConnectorApi.BootstrapStart(GENESIS), "r0", 0),
            FACTS)
        .registration();
  }

  static ConnectorApi.SealedRef sealed(ConnectorApi.EpochIdentity epoch) {
    return new ConnectorApi.SealedRef(
        epoch,
        new ConnectorApi.BlobRef(
            ConnectorApi.ObjectKind.SEALED, "manifest-" + epoch.originCheckpointId(), 0, DIGEST));
  }

  static byte[] frameOracle(ConnectorApi.EpochIdentity epoch) {
    try {
      var output = new ByteArrayOutputStream();
      var data = new DataOutputStream(output);
      for (String text : List.of("connector-exact-v2", epoch.lineage().id())) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        data.writeInt(bytes.length);
        data.write(bytes);
      }
      data.writeLong(epoch.planningGeneration());
      data.writeLong(epoch.originCheckpointId());
      data.write(HexFormat.of().parseHex(epoch.coverage().value().split(":")[1]));
      data.flush();
      return output.toByteArray();
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  static ClientApi.TableBinding table() {
    var id = new ClientApi.Column("id", ClientApi.LogicalType.INT64, false);
    var schema =
        new ClientApi.SchemaDescriptor(
            "sha256:" + "0".repeat(64),
            List.of(id),
            List.of(id),
            Optional.empty(),
            Optional.empty());
    var codec = new ClientApi.PkCodecBinding("test", "test", schema.fingerprint(), List.of(id));
    return new ClientApi.TableBinding(
        "ns", "table", "table-id", 0, 0, schema, Optional.empty(), codec, "profile");
  }

  static ClientApi.CheckpointContext context(ConnectorApi.EpochIdentity epoch) {
    return new ClientApi.CheckpointContext(
        "model-domain",
        bytes(frameOracle(epoch)),
        "original-wire",
        0,
        "model-comparator",
        ClientApi.Coverage.EXACT,
        0,
        Optional.empty());
  }

  static ConnectorApi.CheckpointPlan plan(ConnectorApi.EpochIdentity epoch) {
    var fact =
        new ClientApi.RootFreeFact(
            table(),
            context(epoch),
            0,
            1,
            "elk1:original",
            OptionalLong.of(17),
            digest("payload-sha256-v1"),
            DIGEST,
            digest("part-sha256-v1"));
    var seal = new ClientApi.TableSeal(table(), context(epoch), List.of(fact), digest("sha256-v2"));
    var prepared =
        new ConnectorApi.BlobRef(ConnectorApi.ObjectKind.ROOT_FREE_PART, "prepared", 1, DIGEST);
    var payload =
        new ConnectorApi.BlobRef(ConnectorApi.ObjectKind.WRITE_PAYLOAD, "payload", 1, DIGEST);
    var part = new ConnectorApi.PartPlan(fact, prepared, payload, DIGEST);
    return new ConnectorApi.CheckpointPlan(
        epoch,
        List.of(new ConnectorApi.TablePlan(seal, List.of(part))),
        new ConnectorApi.SourceCoverage(List.of(), DIGEST),
        DIGEST);
  }
}
