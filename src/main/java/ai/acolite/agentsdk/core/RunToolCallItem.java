package ai.acolite.agentsdk.core;

import com.openai.core.JsonValue;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * RunToolCallItem
 *
 * <p>Represents a tool invocation request from the model.
 *
 * <p>Ported from TypeScript OpenAI Agents SDK Source: <a
 * href="https://github.com/openai/openai-agents-js/blob/main/packages/agents-core/src/items.ts">items.ts</a>
 */
@Getter
@SuperBuilder
@Jacksonized
public class RunToolCallItem extends RunItemBase {
  private final String id;
  private final String name;
  private final Object parameters;

  @lombok.Builder.Default
  private Map<String, JsonValue> additionalProperties = java.util.Collections.emptyMap();

  private static final String[]
      REASONING_KEYS =
          {
            "reasoning_content",
            "reasoning",
            "thinking",
            "reasoning_details",
            "reasoning_text",
            "thought",
            "thoughts",
            "analysis",
            "cot",
            "chain_of_thought",
            "scratchpad",
            "internal_thought",
            "rationale",
            "explanation"
          },
      REASONING_KEYS_INNER =
          {"text", "content", "summary", "thought", "reasoning", "reasoning_content", "delta"};

  public String getReasoning() {
    if (additionalProperties == null || additionalProperties.isEmpty()) {
      return null;
    }
    for (String key : REASONING_KEYS) {
      String text = flatten(additionalProperties.get(key));
      if (text != null && !text.isEmpty()) {
        return text;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static String flatten(JsonValue value) {
    if (value == null || value.isNull() || value.isMissing()) {
      return null;
    }

    Object scalar = value.asString().orElse(null);
    if (scalar == null) {
      scalar = value.asNumber().orElse(null);
    }
    if (scalar == null) {
      scalar = value.asBoolean().orElse(null);
    }
    if (scalar != null) {
      return String.valueOf(scalar);
    }

    Object array = value.asArray().orElse(null);
    if (array instanceof List<?> list) {
      StringBuilder sb = new StringBuilder();
      for (Object part : list) {
        appendLine(sb, part instanceof JsonValue json ? flatten(json) : null);
      }
      return sb.isEmpty() ? null : sb.toString();
    }

    Object object = value.asObject().orElse(null);
    if (object instanceof Map<?, ?> map) {
      for (String key : REASONING_KEYS_INNER) {
        String text = flatten((JsonValue) map.get(key));
        if (text != null && !text.isEmpty()) {
          return text;
        }
      }
    }
    return null;
  }

  private static void appendLine(StringBuilder sb, String text) {
    if (text == null || text.isEmpty()) {
      return;
    }
    if (!sb.isEmpty()) {
      sb.append('\n');
    }
    sb.append(text);
  }
}
