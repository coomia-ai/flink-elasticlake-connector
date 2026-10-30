package io.elasticlake.consumer;

import static io.elasticlake.connector.api.v3.ConnectorApi.*;

import io.elasticlake.client.api.v2.ClientApi;
import io.elasticlake.connector.api.v3.ConnectorApi;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public final class LogicalApiConsumer {
  public static void main(String[] args) throws Exception {
    LocalContractRegression.verify();
    if (Runtime.version().feature() != 17) throw new AssertionError("Actual Java17 is required");
    String expectedSurface;
    try (var stream = LogicalApiConsumer.class.getResourceAsStream("/reference-abi.txt")) {
      if (stream == null) throw new AssertionError("Reference ABI fixture missing");
      expectedSurface = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
    if (!StrictAbi.surface(ConnectorApi.class).equals(expectedSurface)) {
      throw new AssertionError(
          "Ordered components/constructors/enums/closed unions differ from reference ABI");
    }
    if (!new ClientApi.StringValue("accepted-sdk-api").value().equals("accepted-sdk-api")) {
      throw new AssertionError("Accepted SDK API did not link");
    }
    if (ClientApi.class
        .getProtectionDomain()
        .getCodeSource()
        .getLocation()
        .equals(ConnectorApi.class.getProtectionDomain().getCodeSource().getLocation())) {
      throw new AssertionError("SDK source/classes must not be substituted into the review output");
    }
    RecoveryStore store = new AbiStore();
    try {
      store.readVerified(null, null);
      throw new AssertionError("Expected the test-only unsupported port");
    } catch (UnsupportedOperationException expected) {
      if (!expected.getMessage().equals("ABI consumer only")) throw expected;
    }
    if (!ConnectorApi.class
        .getProtectionDomain()
        .getCodeSource()
        .getLocation()
        .toString()
        .endsWith("-logical-api.jar")) {
      throw new AssertionError("Logical variant was not the class source");
    }
    try {
      new PageToken(null);
      throw new AssertionError("Null was accepted");
    } catch (ClientApi.ClientException expected) {
      if (expected.failure().code() != ClientApi.FailureCode.INVALID_INPUT) throw expected;
    }
    List<Class<?>> types = new ArrayList<>();
    types.add(ConnectorApi.class);
    types.addAll(Arrays.asList(ConnectorApi.class.getDeclaredClasses()));
    Set<String> abi = new TreeSet<>();
    int records = 0;
    int spis = 0;
    for (Class<?> type : types) {
      if (!Modifier.isPublic(type.getModifiers()))
        throw new AssertionError("Nonpublic logical type: " + type);
      abi.add(type.toGenericString());
      if (type.isInterface()) spis++;
      if (type.isRecord()) {
        records++;
        for (RecordComponent component : type.getRecordComponents()) {
          checkSurface(component.getGenericType().getTypeName());
          abi.add(
              type.getName()
                  + " component "
                  + component.getName()
                  + ":"
                  + component.getGenericType().getTypeName());
        }
      }
      for (Method method : type.getDeclaredMethods()) {
        if (Modifier.isPublic(method.getModifiers())) {
          checkSurface(method.toGenericString());
          abi.add(method.toGenericString());
        }
      }
    }
    Set<String> starts = new TreeSet<>();
    for (Class<?> start : ConnectorApi.RunStart.class.getPermittedSubclasses())
      starts.add(start.getSimpleName());
    if (!starts.equals(Set.of("BootstrapStart", "BeforeCheckpointRestart", "CheckpointRestart"))) {
      throw new AssertionError("RunStart is not the reviewed closed union: " + starts);
    }
    String abiSha =
        HexFormat.of()
            .formatHex(
                MessageDigest.getInstance("SHA-256")
                    .digest(String.join("\n", abi).getBytes(StandardCharsets.UTF_8)));
    if (!abiSha.equals("12a645479068ab21f9b3ef20aa799f1704338589ccb515a15cb20e5e8169ce03")
        || types.size() != 46
        || records != 35
        || spis != 8) {
      throw new AssertionError("Reference logical ABI differs: " + abiSha);
    }
    System.out.println(
        "CONNECTOR_LOGICAL_API_CONSUMER_OK java="
            + Runtime.version()
            + " publicTypes="
            + types.size()
            + " records="
            + records
            + " interfaces="
            + spis
            + " abiLines="
            + abi.size()
            + " abiSha256="
            + abiSha);
    System.out.println(
        "Logical API only; production RecoveryStore, runtime, codec and performance are not qualified");
  }

  private static final class AbiStore implements RecoveryStore {
    @Override
    public OwnershipToken openControlledRun(ControlledRunPermit permit, StoreBudget budget) {
      throw new UnsupportedOperationException("ABI consumer only");
    }

    @Override
    public BlobRef putImmutable(ObjectKind kind, ClientApi.Bytes bytes, StoreBudget budget) {
      throw new UnsupportedOperationException("ABI consumer only");
    }

    @Override
    public ClientApi.Bytes readVerified(BlobRef reference, StoreBudget budget) {
      throw new UnsupportedOperationException("ABI consumer only");
    }

    @Override
    public SealedRef createOrVerify(CheckpointPlan plan, OwnershipToken owner, StoreBudget budget) {
      throw new UnsupportedOperationException("ABI consumer only");
    }

    @Override
    public CheckpointPlan load(SealedRef sealed, StoreBudget budget) {
      throw new UnsupportedOperationException("ABI consumer only");
    }

    @Override
    public IntentRef recordFirstSend(
        SealedRef sealed,
        CommitAuthorization authorization,
        OwnershipToken owner,
        StoreBudget budget) {
      throw new UnsupportedOperationException("ABI consumer only");
    }

    @Override
    public Optional<IntentRef> resumeIntent(
        SealedRef sealed, OwnershipToken owner, StoreBudget budget) {
      throw new UnsupportedOperationException("ABI consumer only");
    }

    @Override
    public Optional<ProgressRef> progress(SealedRef sealed, StoreBudget budget) {
      throw new UnsupportedOperationException("ABI consumer only");
    }

    @Override
    public ProgressRef advance(
        Optional<ProgressRef> expected, Progress next, OwnershipToken owner, StoreBudget budget) {
      throw new UnsupportedOperationException("ABI consumer only");
    }

    @Override
    public BlobRef persistOutbox(OutboxRecord outbox, OwnershipToken owner, StoreBudget budget) {
      throw new UnsupportedOperationException("ABI consumer only");
    }

    @Override
    public RestoreDecision verifyRestore(ControlledRunPermit permit, StoreBudget budget) {
      throw new UnsupportedOperationException("ABI consumer only");
    }

    @Override
    public RecoveryPage listRecoveryCandidates(
        Lineage lineage, PageToken page, int limit, StoreBudget budget) {
      throw new UnsupportedOperationException("ABI consumer only");
    }

    @Override
    public BlobRef sealHandoff(
        ControlledRunPermit permit, OwnershipToken owner, StoreBudget budget) {
      throw new UnsupportedOperationException("ABI consumer only");
    }

    @Override
    public ClientApi.ShutdownReport shutdown(Duration timeout) {
      throw new UnsupportedOperationException("ABI consumer only");
    }
  }

  private static void checkSurface(String type) {
    for (String forbidden :
        List.of(
            "org.apache.flink",
            "io.elasticlake.server",
            "io.elasticlake.core",
            "io.elasticlake.client.api.v1",
            "io.elasticlake.connector.recovery",
            "software.amazon",
            "org.apache.paimon")) {
      if (type.contains(forbidden))
        throw new AssertionError("Forbidden logical ABI dependency: " + type);
    }
  }
}
