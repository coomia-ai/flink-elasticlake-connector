package io.elasticlake.connector.buildprobe;

import io.elasticlake.connector.api.v3.ApiProbe;

/** I-06 non-product main fixture depends on the single logical API output. */
public final class MainProbe {
  private MainProbe() {}

  public static String marker() {
    return ApiProbe.marker();
  }
}
