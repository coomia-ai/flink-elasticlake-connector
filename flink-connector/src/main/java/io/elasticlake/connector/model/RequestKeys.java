package io.elasticlake.connector.model;

import static io.elasticlake.client.api.v2.ClientApi.FailureCode.CAPACITY;
import static io.elasticlake.client.api.v2.ClientApi.FailureCode.CONFLICT;
import static io.elasticlake.client.api.v2.ClientApi.FailureCode.INVALID_INPUT;

import io.elasticlake.client.api.v2.ClientApi;
import io.elasticlake.connector.api.v3.ConnectorApi;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * Accepted key-frame-v1 framing only; callers retain full lineage/table authority and original
 * keys.
 */
public final class RequestKeys {
  private static final byte[] DOMAIN =
      "elasticlake-connector-request-key".getBytes(StandardCharsets.US_ASCII);

  private RequestKeys() {}

  /** Pure framing; this does not admit a codec, checkpoint, table or sealed plan. */
  public static byte[] canonicalFrame(
      String lineage,
      String tableIdentity,
      ClientApi.Bytes checkpoint,
      int slot,
      int maxFrameBytes) {
    ModelChecks.required(lineage, "RequestKeys.lineage");
    ModelChecks.required(tableIdentity, "RequestKeys.tableIdentity");
    ModelChecks.required(checkpoint, "RequestKeys.checkpoint");
    ModelChecks.nonnegative(slot, "RequestKeys.slot");
    ModelChecks.require(
        maxFrameBytes > 0, INVALID_INPUT, "RequestKeys.budget", "nonpositive budget");
    long lineageLength = utf8Length(lineage, "RequestKeys.lineage");
    long tableLength = utf8Length(tableIdentity, "RequestKeys.tableIdentity");
    int checkpointLength = checkpoint.size();
    ModelChecks.nonnegative(checkpointLength, "RequestKeys.checkpoint.size");
    long length = ModelChecks.add(55L, lineageLength, "RequestKeys.length");
    length = ModelChecks.add(length, tableLength, "RequestKeys.length");
    length = ModelChecks.add(length, checkpointLength, "RequestKeys.length");
    ModelChecks.require(
        length <= maxFrameBytes, CAPACITY, "RequestKeys.budget", "frame exceeds budget");
    byte[] canonical = ModelChecks.required(checkpoint.copy(), "RequestKeys.checkpoint.copy");
    ModelChecks.require(
        canonical.length == checkpointLength,
        INVALID_INPUT,
        "RequestKeys.checkpoint",
        "size/copy mismatch");
    byte[] lineageBytes = lineage.getBytes(StandardCharsets.UTF_8);
    byte[] tableBytes = tableIdentity.getBytes(StandardCharsets.UTF_8);
    return ByteBuffer.allocate((int) length)
        .putInt(DOMAIN.length)
        .put(DOMAIN)
        .putShort((short) 1)
        .putInt(lineageBytes.length)
        .put(lineageBytes)
        .putInt(tableBytes.length)
        .put(tableBytes)
        .putInt(canonical.length)
        .put(canonical)
        .putInt(slot)
        .array();
  }

  /** Used before sealing a new part. Recovery reads the persisted key instead of calling this. */
  public static String newPartKey(
      ConnectorApi.EpochIdentity epoch,
      ClientApi.TableBinding table,
      ClientApi.CheckpointContext context,
      String originalOpaqueIdentity,
      int slot,
      int partCount,
      int maxFrameBytes) {
    ModelChecks.required(table, "RequestKeys.table");
    ModelChecks.required(context, "RequestKeys.context");
    ModelChecks.required(epoch, "RequestKeys.epoch");
    ModelChecks.required(originalOpaqueIdentity, "RequestKeys.originalOpaqueIdentity");
    ModelChecks.require(
        originalOpaqueIdentity.equals(table.opaqueIdentity()),
        CONFLICT,
        "RequestKeys.originalOpaqueIdentity",
        "original opaque identity differs from SDK binding");
    ModelChecks.require(
        partCount > 0 && partCount <= 1_000_000 && slot >= 0 && slot < partCount,
        INVALID_INPUT,
        "RequestKeys.slot/count",
        "invalid original part coordinates");
    ModelChecks.require(
        context.coverage() == ClientApi.Coverage.EXACT
            && context.schemaRevision() == table.schemaRevision()
            && context.versionDomain().equals(table.versionDomain()),
        CONFLICT,
        "RequestKeys.context",
        "context differs from epoch/table");
    byte[] frame =
        canonicalFrame(
            epoch.lineage().id(),
            originalOpaqueIdentity,
            context.checkpointCanonical(),
            slot,
            maxFrameBytes);
    byte[] expected = EpochFrames.canonical(epoch, maxFrameBytes);
    ModelChecks.require(
        context.checkpointCanonical().size() == expected.length
            && Arrays.equals(expected, context.checkpointCanonical().copy()),
        CONFLICT,
        "RequestKeys.checkpoint",
        "canonical checkpoint differs from original epoch");
    try {
      return "elk1:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(frame));
    } catch (NoSuchAlgorithmException unavailable) {
      throw new IllegalStateException("RequestKeys requires JDK SHA-256", unavailable);
    }
  }

  private static long utf8Length(String text, String path) {
    long length = 0;
    for (int index = 0; index < text.length(); index++) {
      char value = text.charAt(index);
      if (Character.isHighSurrogate(value)) {
        ModelChecks.require(
            index + 1 < text.length() && Character.isLowSurrogate(text.charAt(index + 1)),
            INVALID_INPUT,
            path,
            "unpaired surrogate");
        index++;
        length += 4;
      } else {
        ModelChecks.require(
            !Character.isLowSurrogate(value), INVALID_INPUT, path, "unpaired surrogate");
        length += value < 128 ? 1 : value < 2048 ? 2 : 3;
      }
    }
    return length;
  }
}
