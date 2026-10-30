package io.elasticlake.connector.api.v3;

import io.elasticlake.client.api.v2.ClientApi;

/** I-06 non-product fixture; no proposed Connector ABI is implemented here. */
public final class ApiProbe {
  private ApiProbe() {}

  public static String marker() {
    return new ClientApi.StringValue("I06_NON_PRODUCT_PROBE").value();
  }
}
