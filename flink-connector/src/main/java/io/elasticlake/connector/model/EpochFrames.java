package io.elasticlake.connector.model;

import static io.elasticlake.client.api.v2.ClientApi.FailureCode.*;

import io.elasticlake.client.api.v2.ClientApi;
import io.elasticlake.connector.api.v3.ConnectorApi;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;

/** SPEC-26 §3.1 candidate frame only; this does not register a source codec or wire profile. */
public final class EpochFrames {
  private static final byte[] DOMAIN = "connector-exact-v2".getBytes(StandardCharsets.UTF_8);

  private EpochFrames() {}

  public static byte[] canonical(ConnectorApi.EpochIdentity epoch, int maxFrameBytes) {
    ModelChecks.required(epoch, "EpochFrames.epoch");
    ModelChecks.require(
        maxFrameBytes > 0, INVALID_INPUT, "EpochFrames.maxFrameBytes", "budget must be positive");
    String id = epoch.lineage().id();
    long utf8Length = 0;
    for (int index = 0; index < id.length(); index++) {
      char c = id.charAt(index);
      if (Character.isHighSurrogate(c)) {
        ModelChecks.require(
            index + 1 < id.length() && Character.isLowSurrogate(id.charAt(index + 1)),
            INVALID_INPUT,
            "EpochFrames.lineage.id",
            "unpaired surrogate");
        index++;
        utf8Length += 4;
      } else {
        ModelChecks.require(
            !Character.isLowSurrogate(c),
            INVALID_INPUT,
            "EpochFrames.lineage.id",
            "unpaired surrogate");
        utf8Length += c < 128 ? 1 : c < 2048 ? 2 : 3;
      }
    }
    long length = 4L + DOMAIN.length + 4L + utf8Length + 8L + 8L + 32L;
    ModelChecks.require(
        length <= maxFrameBytes,
        CAPACITY,
        "EpochFrames.maxFrameBytes",
        "canonical frame exceeds budget");
    byte[] lineage = id.getBytes(StandardCharsets.UTF_8);
    String digest = epoch.coverage().value();
    byte[] coverage = HexFormat.of().parseHex(digest.substring(digest.indexOf(':') + 1));
    var buffer = ByteBuffer.allocate((int) length);
    buffer
        .putInt(DOMAIN.length)
        .put(DOMAIN)
        .putInt(lineage.length)
        .put(lineage)
        .putLong(epoch.planningGeneration())
        .putLong(epoch.originCheckpointId())
        .put(coverage);
    return buffer.array();
  }

  public static ClientApi.CheckpointContext exactContext(
      ConnectorApi.SourceCoverageProvider provider,
      ConnectorApi.EpochIdentity epoch,
      ClientApi.TableBinding table,
      int maxFrameBytes) {
    ModelChecks.required(provider, "EpochFrames.provider");
    ModelChecks.required(table, "EpochFrames.table");
    byte[] frame = canonical(epoch, maxFrameBytes);
    var context = ModelChecks.required(provider.exactContext(epoch, table), "EpochFrames.context");
    ModelChecks.require(
        context.coverage() == ClientApi.Coverage.EXACT
            && Arrays.equals(frame, context.checkpointCanonical().copy())
            && context.schemaRevision() == table.schemaRevision()
            && context.versionDomain().equals(table.versionDomain()),
        CONFLICT,
        "EpochFrames.context",
        "provider context differs from exact epoch/table");
    return context;
  }
}
