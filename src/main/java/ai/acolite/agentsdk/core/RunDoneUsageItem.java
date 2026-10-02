package ai.acolite.agentsdk.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * RunDoneUsageItem
 *
 * <p>Represents the usage of tokens in a run.
 */
@Getter
@SuperBuilder
@Jacksonized
public class RunDoneUsageItem extends RunItemBase {
  private final Double inputTokens;
  private final Double outputTokens;
  private final Double totalTokens;

  @lombok.Builder.Default List<Map<String, Double>> inputTokensDetails = Collections.emptyList();
  @lombok.Builder.Default List<Map<String, Double>> outputTokensDetails = Collections.emptyList();
}
