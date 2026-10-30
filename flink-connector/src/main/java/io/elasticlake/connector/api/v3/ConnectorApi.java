package io.elasticlake.connector.api.v3;

import io.elasticlake.client.api.v2.ClientApi;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Flink-independent logical SPI; no runtime topology or S3 implementation. */
public interface ConnectorApi {
  record SourceDomain(String system, String partition, String comparator, long registryRevision) {
    public SourceDomain {
      ConnectorValues.required(system, "SourceDomain.system");
      ConnectorValues.required(partition, "SourceDomain.partition");
      ConnectorValues.required(comparator, "SourceDomain.comparator");
      ConnectorValues.nonnegative(registryRevision, "SourceDomain.registryRevision");
    }
  }

  record SourceRange(
      SourceDomain domain,
      ClientApi.Bytes start,
      ClientApi.Bytes end,
      ClientApi.Digest processedSetDigest,
      long readCount,
      long keptCount,
      long filteredCount,
      long deduplicatedCount,
      long dlqCount) {
    public SourceRange {
      ConnectorValues.required(domain, "SourceRange.domain");
      start = ConnectorValues.bytes(start, "SourceRange.start");
      end = ConnectorValues.bytes(end, "SourceRange.end");
      ConnectorValues.required(processedSetDigest, "SourceRange.processedSetDigest");
      ConnectorValues.nonnegative(readCount, "SourceRange.readCount");
      ConnectorValues.nonnegative(keptCount, "SourceRange.keptCount");
      ConnectorValues.nonnegative(filteredCount, "SourceRange.filteredCount");
      ConnectorValues.nonnegative(deduplicatedCount, "SourceRange.deduplicatedCount");
      ConnectorValues.nonnegative(dlqCount, "SourceRange.dlqCount");
    }
  }

  record SourceCoverage(List<SourceRange> ranges, ClientApi.Digest canonicalDigest) {
    public SourceCoverage {
      ranges = ConnectorValues.list(ranges, "SourceCoverage.ranges");
      ConnectorValues.required(canonicalDigest, "SourceCoverage.canonicalDigest");
    }
  }

  enum Disposition {
    KEPT,
    FILTERED,
    DEDUPLICATED,
    DLQ
  }

  record OutputMapping(
      long ordinal,
      String opaqueTarget,
      MutationIdentity mutation,
      ClientApi.Digest operationDigest,
      Optional<String> eventId) {
    public OutputMapping {
      ConnectorValues.nonnegative(ordinal, "OutputMapping.ordinal");
      ConnectorValues.required(opaqueTarget, "OutputMapping.opaqueTarget");
      ConnectorValues.required(mutation, "OutputMapping.mutation");
      ConnectorValues.required(operationDigest, "OutputMapping.operationDigest");
      ConnectorValues.required(eventId, "OutputMapping.eventId");
    }
  }

  record InputDisposition(
      SourcePosition position,
      Disposition disposition,
      List<OutputMapping> outputs,
      Optional<ClientApi.Digest> dlqIdentity) {
    public InputDisposition {
      ConnectorValues.required(position, "InputDisposition.position");
      ConnectorValues.required(disposition, "InputDisposition.disposition");
      outputs = ConnectorValues.list(outputs, "InputDisposition.outputs");
      ConnectorValues.required(dlqIdentity, "InputDisposition.dlqIdentity");
      ConnectorValues.unsupported(
          disposition == Disposition.DLQ && !outputs.isEmpty(),
          "InputDisposition.outputs",
          "mixed business output and DLQ is unsupported");
      ConnectorValues.check(
          disposition == Disposition.KEPT || outputs.isEmpty(),
          "InputDisposition.outputs",
          "non-kept input has business output");
      ConnectorValues.check(
          (disposition == Disposition.DLQ) == dlqIdentity.isPresent(),
          "InputDisposition.dlqIdentity",
          "DLQ identity presence differs from disposition");
    }
  }

  record Lineage(
      String id,
      String operatorUid,
      ClientApi.Digest transform,
      ClientApi.Digest tableBindings,
      ClientApi.Digest profile) {
    public Lineage {
      ConnectorValues.required(id, "Lineage.id");
      ConnectorValues.required(operatorUid, "Lineage.operatorUid");
      ConnectorValues.required(transform, "Lineage.transform");
      ConnectorValues.required(tableBindings, "Lineage.tableBindings");
      ConnectorValues.required(profile, "Lineage.profile");
    }
  }

  record EpochIdentity(
      Lineage lineage,
      long planningGeneration,
      long originCheckpointId,
      ClientApi.Digest coverage) {
    public EpochIdentity {
      ConnectorValues.required(lineage, "EpochIdentity.lineage");
      ConnectorValues.nonnegative(planningGeneration, "EpochIdentity.planningGeneration");
      ConnectorValues.nonnegative(originCheckpointId, "EpochIdentity.originCheckpointId");
      ConnectorValues.required(coverage, "EpochIdentity.coverage");
    }
  }

  record MutationIdentity(String project, String sourceDomain, String id) {
    public MutationIdentity {
      ConnectorValues.required(project, "MutationIdentity.project");
      ConnectorValues.required(sourceDomain, "MutationIdentity.sourceDomain");
      ConnectorValues.required(id, "MutationIdentity.id");
    }
  }

  record SourcePosition(SourceDomain domain, ClientApi.Bytes position, long outputOrdinal) {
    public SourcePosition {
      ConnectorValues.required(domain, "SourcePosition.domain");
      position = ConnectorValues.bytes(position, "SourcePosition.position");
      ConnectorValues.nonnegative(outputOrdinal, "SourcePosition.outputOrdinal");
    }
  }

  record SinkMutation(
      MutationIdentity id,
      SourcePosition position,
      ClientApi.TableBinding target,
      ClientApi.Mutation operation,
      Optional<String> eventId) {
    public SinkMutation {
      ConnectorValues.required(id, "SinkMutation.id");
      ConnectorValues.required(position, "SinkMutation.position");
      ConnectorValues.required(target, "SinkMutation.target");
      ConnectorValues.required(operation, "SinkMutation.operation");
      ConnectorValues.required(eventId, "SinkMutation.eventId");
    }
  }

  enum ObjectKind {
    SPOOL,
    ROOT_FREE_PART,
    WRITE_PAYLOAD,
    SEALED,
    OUTBOX,
    HANDOFF
  }

  record BlobRef(ObjectKind kind, String immutableKey, long length, ClientApi.Digest sha256) {
    public BlobRef {
      ConnectorValues.required(kind, "BlobRef.kind");
      ConnectorValues.required(immutableKey, "BlobRef.immutableKey");
      ConnectorValues.nonnegative(length, "BlobRef.length");
      ConnectorValues.required(sha256, "BlobRef.sha256");
    }
  }

  record WriterFragment(
      long localFragmentSequence,
      List<BlobRef> chunks,
      SourceCoverage coverage,
      ClientApi.Digest contentDigest) {
    public WriterFragment {
      ConnectorValues.nonnegative(localFragmentSequence, "WriterFragment.localFragmentSequence");
      chunks = ConnectorValues.list(chunks, "WriterFragment.chunks");
      ConnectorValues.required(coverage, "WriterFragment.coverage");
      ConnectorValues.required(contentDigest, "WriterFragment.contentDigest");
    }
  }

  record CheckpointFragment(
      long originCheckpointId, int originWriterId, int expectedWriters, WriterFragment fragment) {
    public CheckpointFragment {
      ConnectorValues.nonnegative(originCheckpointId, "CheckpointFragment.originCheckpointId");
      ConnectorValues.nonnegative(originWriterId, "CheckpointFragment.originWriterId");
      ConnectorValues.nonnegative(expectedWriters, "CheckpointFragment.expectedWriters");
      ConnectorValues.required(fragment, "CheckpointFragment.fragment");
      ConnectorValues.check(
          expectedWriters > 0 && originWriterId < expectedWriters,
          "CheckpointFragment.originWriterId",
          "writer outside expected writers");
    }
  }

  record PartPlan(
      ClientApi.RootFreeFact fact,
      BlobRef preparedPart,
      BlobRef writePayload,
      ClientApi.Digest requestDigest) {
    public PartPlan {
      ConnectorValues.required(fact, "PartPlan.fact");
      ConnectorValues.required(preparedPart, "PartPlan.preparedPart");
      ConnectorValues.required(writePayload, "PartPlan.writePayload");
      ConnectorValues.required(requestDigest, "PartPlan.requestDigest");
    }
  }

  record TablePlan(ClientApi.TableSeal seal, List<PartPlan> parts) {
    public TablePlan {
      ConnectorValues.required(seal, "TablePlan.seal");
      parts = ConnectorValues.list(parts, "TablePlan.parts");
    }
  }

  record CheckpointPlan(
      EpochIdentity epoch,
      List<TablePlan> tables,
      SourceCoverage coverage,
      ClientApi.Digest contentDigest) {
    public CheckpointPlan {
      ConnectorValues.required(epoch, "CheckpointPlan.epoch");
      tables = ConnectorValues.list(tables, "CheckpointPlan.tables");
      ConnectorValues.required(coverage, "CheckpointPlan.coverage");
      ConnectorValues.required(contentDigest, "CheckpointPlan.contentDigest");
    }
  }

  record SealedRef(EpochIdentity epoch, BlobRef manifest) {
    public SealedRef {
      ConnectorValues.required(epoch, "SealedRef.epoch");
      ConnectorValues.required(manifest, "SealedRef.manifest");
    }
  }

  record RestorePoint(long completedCheckpointId, ClientApi.Digest stateIdentity) {
    public RestorePoint {
      ConnectorValues.nonnegative(completedCheckpointId, "RestorePoint.completedCheckpointId");
      ConnectorValues.required(stateIdentity, "RestorePoint.stateIdentity");
    }
  }

  record GenesisIdentity(
      String registrationId,
      ClientApi.Bytes canonicalSourceStart,
      ClientApi.Digest sourceStartDigest) {
    public GenesisIdentity {
      ConnectorValues.required(registrationId, "GenesisIdentity.registrationId");
      canonicalSourceStart =
          ConnectorValues.bytes(canonicalSourceStart, "GenesisIdentity.canonicalSourceStart");
      ConnectorValues.required(sourceStartDigest, "GenesisIdentity.sourceStartDigest");
    }
  }

  sealed interface RunStart permits BootstrapStart, BeforeCheckpointRestart, CheckpointRestart {}

  record BootstrapStart(GenesisIdentity genesis) implements RunStart {
    public BootstrapStart {
      ConnectorValues.required(genesis, "BootstrapStart.genesis");
    }
  }

  record BeforeCheckpointRestart(
      GenesisIdentity genesis, String priorRunId, ClientApi.Digest stoppedPriorRunProof)
      implements RunStart {
    public BeforeCheckpointRestart {
      ConnectorValues.required(genesis, "BeforeCheckpointRestart.genesis");
      ConnectorValues.required(priorRunId, "BeforeCheckpointRestart.priorRunId");
      ConnectorValues.required(
          stoppedPriorRunProof, "BeforeCheckpointRestart.stoppedPriorRunProof");
    }
  }

  record CheckpointRestart(
      RestorePoint point, String priorRunId, ClientApi.Digest stoppedPriorRunProof)
      implements RunStart {
    public CheckpointRestart {
      ConnectorValues.required(point, "CheckpointRestart.point");
      ConnectorValues.required(priorRunId, "CheckpointRestart.priorRunId");
      ConnectorValues.required(stoppedPriorRunProof, "CheckpointRestart.stoppedPriorRunProof");
    }
  }

  record RunRequest(Lineage lineage, RunStart start, String requestedRunId) {
    public RunRequest {
      ConnectorValues.required(lineage, "RunRequest.lineage");
      ConnectorValues.required(start, "RunRequest.start");
      ConnectorValues.required(requestedRunId, "RunRequest.requestedRunId");
    }
  }

  record ControlledRunPermit(
      Lineage lineage,
      RunStart start,
      String runId,
      long planningGeneration,
      ClientApi.Digest authorizerEvidence) {
    public ControlledRunPermit {
      ConnectorValues.required(lineage, "ControlledRunPermit.lineage");
      ConnectorValues.required(start, "ControlledRunPermit.start");
      ConnectorValues.required(runId, "ControlledRunPermit.runId");
      ConnectorValues.nonnegative(planningGeneration, "ControlledRunPermit.planningGeneration");
      ConnectorValues.required(authorizerEvidence, "ControlledRunPermit.authorizerEvidence");
    }
  }

  record OwnershipToken(String lineageId, String runId, long generation, String opaqueGrant) {
    public OwnershipToken {
      ConnectorValues.required(lineageId, "OwnershipToken.lineageId");
      ConnectorValues.required(runId, "OwnershipToken.runId");
      ConnectorValues.nonnegative(generation, "OwnershipToken.generation");
      ConnectorValues.required(opaqueGrant, "OwnershipToken.opaqueGrant");
    }
  }

  record CommitAuthorization(
      EpochIdentity originalEpoch,
      OwnershipToken owner,
      ClientApi.Digest manifestSha256,
      String invocationId,
      ClientApi.Bytes opaqueProof) {
    public CommitAuthorization {
      ConnectorValues.required(originalEpoch, "CommitAuthorization.originalEpoch");
      ConnectorValues.required(owner, "CommitAuthorization.owner");
      ConnectorValues.required(manifestSha256, "CommitAuthorization.manifestSha256");
      ConnectorValues.required(invocationId, "CommitAuthorization.invocationId");
      opaqueProof = ConnectorValues.bytes(opaqueProof, "CommitAuthorization.opaqueProof");
    }
  }

  interface CommitAuthorizationVerifier {
    void verify(
        SealedRef sealed,
        CommitAuthorization authorization,
        OwnershipToken owner,
        StoreBudget budget);
  }

  enum PartStage {
    INTENT,
    UNKNOWN,
    ACKED,
    LANDED
  }

  record PartProgress(int tableId, int slot, PartStage stage, Optional<ClientApi.WriteAck> ack) {
    public PartProgress {
      ConnectorValues.nonnegative(tableId, "PartProgress.tableId");
      ConnectorValues.nonnegative(slot, "PartProgress.slot");
      ConnectorValues.required(stage, "PartProgress.stage");
      ConnectorValues.required(ack, "PartProgress.ack");
      ConnectorValues.check(
          (stage != PartStage.ACKED && stage != PartStage.LANDED) || ack.isPresent(),
          "PartProgress.ack",
          "acknowledged stage requires original ack");
    }
  }

  record Progress(
      long revision,
      SealedRef sealed,
      List<PartProgress> parts,
      List<ClientApi.DeliveryReceipt> receipts,
      Optional<BlobRef> durableOutbox,
      boolean reportAcknowledged) {
    public Progress {
      ConnectorValues.nonnegative(revision, "Progress.revision");
      ConnectorValues.required(sealed, "Progress.sealed");
      parts = ConnectorValues.list(parts, "Progress.parts");
      receipts = ConnectorValues.list(receipts, "Progress.receipts");
      ConnectorValues.required(durableOutbox, "Progress.durableOutbox");
      ConnectorValues.check(
          !reportAcknowledged || durableOutbox.isPresent(),
          "Progress.durableOutbox",
          "acknowledged report requires durable outbox");
    }
  }

  record ProgressRef(String opaqueVersion, Progress value) {
    public ProgressRef {
      ConnectorValues.required(opaqueVersion, "ProgressRef.opaqueVersion");
      ConnectorValues.required(value, "ProgressRef.value");
    }
  }

  record IntentRef(SealedRef sealed, OwnershipToken owner, long originCheckpointId) {
    public IntentRef {
      ConnectorValues.required(sealed, "IntentRef.sealed");
      ConnectorValues.required(owner, "IntentRef.owner");
      ConnectorValues.nonnegative(originCheckpointId, "IntentRef.originCheckpointId");
    }
  }

  record StoreBudget(ClientApi.CallBudget call, long maxReadBytes, int maxObjects) {
    public StoreBudget {
      ConnectorValues.required(call, "StoreBudget.call");
      ConnectorValues.nonnegative(maxReadBytes, "StoreBudget.maxReadBytes");
      ConnectorValues.nonnegative(maxObjects, "StoreBudget.maxObjects");
    }
  }

  record PageToken(String opaque) {
    public PageToken {
      ConnectorValues.required(opaque, "PageToken.opaque");
    }
  }

  record RecoveryPage(List<SealedRef> plans, Optional<PageToken> next, long bytesRead) {
    public RecoveryPage {
      plans = ConnectorValues.list(plans, "RecoveryPage.plans");
      ConnectorValues.required(next, "RecoveryPage.next");
      ConnectorValues.nonnegative(bytesRead, "RecoveryPage.bytesRead");
    }
  }

  record RestoreDecision(
      boolean admitted, String reason, ControlledRunPermit permit, List<SealedRef> pending) {
    public RestoreDecision {
      ConnectorValues.required(reason, "RestoreDecision.reason");
      ConnectorValues.check(
          admitted || !reason.isBlank(), "RestoreDecision.reason", "rejection requires a reason");
      ConnectorValues.required(permit, "RestoreDecision.permit");
      pending = ConnectorValues.list(pending, "RestoreDecision.pending");
    }
  }

  record OutboxRecord(
      EpochIdentity epoch,
      List<ClientApi.DeliveryReceipt> receipts,
      ClientApi.Digest reportIdentity,
      ClientApi.Bytes encodedReport) {
    public OutboxRecord {
      ConnectorValues.required(epoch, "OutboxRecord.epoch");
      receipts = ConnectorValues.list(receipts, "OutboxRecord.receipts");
      ConnectorValues.required(reportIdentity, "OutboxRecord.reportIdentity");
      encodedReport = ConnectorValues.bytes(encodedReport, "OutboxRecord.encodedReport");
    }
  }

  interface RecoveryStore {
    OwnershipToken openControlledRun(ControlledRunPermit permit, StoreBudget budget);

    BlobRef putImmutable(ObjectKind kind, ClientApi.Bytes bytes, StoreBudget budget);

    ClientApi.Bytes readVerified(BlobRef reference, StoreBudget budget);

    SealedRef createOrVerify(CheckpointPlan plan, OwnershipToken owner, StoreBudget budget);

    CheckpointPlan load(SealedRef sealed, StoreBudget budget);

    IntentRef recordFirstSend(
        SealedRef sealed,
        CommitAuthorization authorization,
        OwnershipToken owner,
        StoreBudget budget);

    Optional<IntentRef> resumeIntent(SealedRef sealed, OwnershipToken owner, StoreBudget budget);

    Optional<ProgressRef> progress(SealedRef sealed, StoreBudget budget);

    ProgressRef advance(
        Optional<ProgressRef> expected, Progress next, OwnershipToken owner, StoreBudget budget);

    BlobRef persistOutbox(OutboxRecord outbox, OwnershipToken owner, StoreBudget budget);

    RestoreDecision verifyRestore(ControlledRunPermit permit, StoreBudget budget);

    RecoveryPage listRecoveryCandidates(
        Lineage lineage, PageToken page, int limit, StoreBudget budget);

    BlobRef sealHandoff(ControlledRunPermit permit, OwnershipToken owner, StoreBudget budget);

    ClientApi.ShutdownReport shutdown(Duration timeout);
  }

  interface SourceCoverageProvider {
    SourceCoverage merge(
        long originCheckpointId,
        List<CheckpointFragment> fragments,
        ClientApi.PreparationLimits limits);

    ClientApi.CheckpointContext exactContext(EpochIdentity epoch, ClientApi.TableBinding table);
  }

  interface PlanAssembler {
    CheckpointPlan assemble(
        EpochIdentity epoch,
        List<CheckpointFragment> fragments,
        List<ClientApi.TableBinding> targets,
        ClientApi.RequestPlanner planner,
        RecoveryStore store,
        OwnershipToken owner,
        StoreBudget budget);
  }

  interface ReportPublisher {
    CompletionStage<Void> publish(OutboxRecord report, ClientApi.CallBudget budget);
  }

  interface RunAuthorizer {
    ControlledRunPermit authorize(RunRequest request);
  }
}
