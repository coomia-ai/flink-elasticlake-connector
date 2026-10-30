package io.elasticlake.connector.recovery.s3.buildprobe;

import io.elasticlake.connector.api.v3.ApiProbe;

/** I-06 non-product provider fixture; deliberately has no main/control/runtime dependency. */
public final class ProviderProbe {
  private ProviderProbe() {}

  public static String marker() {
    return ApiProbe.marker();
  }
}
