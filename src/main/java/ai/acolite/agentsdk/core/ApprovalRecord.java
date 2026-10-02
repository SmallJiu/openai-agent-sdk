package ai.acolite.agentsdk.core;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;

/**
 * ApprovalRecord
 *
 * <p>Records approval/rejection status for tools.
 *
 * <p>Source:
 * https://github.com/openai/openai-agents-js/blob/main/packages/agents-core/src/runContext.ts
 */
public class ApprovalRecord {
  private Object approved; // boolean | string[]
  private Object rejected; // boolean | string[]
  @Setter @Getter private String alwaysRejectReason;
  @Getter private Map<String, String> rejectedReason;

  public ApprovalRecord() {
    this.approved = false;
    this.rejected = false;
  }

  public Object getApproved() {
    return approved;
  }

  public void setApproved(boolean approved) {
    this.approved = approved;
  }

  public void setApproved(List<String> approvedIds) {
    this.approved = approvedIds;
  }

  public Object getRejected() {
    return rejected;
  }

  public void setRejected(boolean rejected) {
    this.rejected = rejected;
  }

  public void setRejected(List<String> rejectedIds) {
    this.rejected = rejectedIds;
  }

  public void addRejectedReason(String toolId, String reason) {
    if (this.rejectedReason == null) this.rejectedReason = new ConcurrentHashMap<>();
    this.rejectedReason.put(toolId, reason);
  }

  public String getRejectedReason(String toolId) {
    if (this.rejectedReason == null) return null;
    return this.rejectedReason.get(toolId);
  }
}
