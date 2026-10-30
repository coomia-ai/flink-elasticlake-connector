package io.elasticlake.consumer;

import io.elasticlake.client.api.v2.ClientApi;
import io.elasticlake.connector.api.v3.ApiProbe;
import io.elasticlake.connector.buildprobe.MainProbe;
import io.elasticlake.connector.recovery.s3.buildprobe.ProviderProbe;

/** Isolated non-product installation check; only published artifacts are on its classpath. */
public final class FoundationConsumer {
  public static void main(String[] args) {
    if (Runtime.version().feature() != 17) throw new AssertionError("Expected actual Java17");
    String marker = new ClientApi.StringValue("I06_NON_PRODUCT_PROBE").value();
    if (!marker.equals(ApiProbe.marker())
        || !marker.equals(MainProbe.marker())
        || !marker.equals(ProviderProbe.marker()))
      throw new AssertionError("Inconsistent artifact API linkage");
    if (ApiProbe.class.getClassLoader() != MainProbe.class.getClassLoader()
        || ApiProbe.class.getClassLoader() != ProviderProbe.class.getClassLoader()) {
      throw new AssertionError("Installed logical API has different class identities");
    }
    System.out.println("NON_PRODUCT_FOUNDATION_CONSUMER_OK java=" + Runtime.version());
  }
}
