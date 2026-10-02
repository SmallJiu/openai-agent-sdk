package ai.acolite.agentsdk.core.runner;

import ai.acolite.agentsdk.core.*;
import ai.acolite.agentsdk.openai.ConversionUtils;
import ai.acolite.agentsdk.openai.SerializationUtils;
import com.openai.core.*;
import com.openai.models.responses.*;
import java.util.*;

/**
 * ResponseParser
 *
 * <p>Utility for parsing ModelResponse output into RunItem objects. Separates parsing logic from
 * execution flow.
 */
public class ResponseParser {

  /**
   * Parse ModelResponse output into RunItem objects
   *
   * @param response The model response to parse
   * @return List of RunItem objects extracted from the response
   */
  public static List<RunItem> parseResponseItems(ModelResponse response) {
    List<RunItem> items = new ArrayList<>();

    if (response.getOutput() == null || response.getOutput().isEmpty()) {
      return items;
    }

    for (Object outputItem : response.getOutput()) {
      RunItem item = parseOutputItem(outputItem);
      if (item != null) {
        items.add(item);
      }
    }
    if (response.getUsage() != null) {
      items.add(
          RunDoneUsageItem.builder()
              .inputTokens(response.getUsage().getInputTokens())
              .outputTokens(response.getUsage().getOutputTokens())
              .totalTokens(response.getUsage().getTotalTokens())
              .inputTokensDetails(response.getUsage().getInputTokensDetails())
              .outputTokensDetails(response.getUsage().getOutputTokensDetails())
              .build());
    }
    return items;
  }

  /**
   * Parse a single output item into a RunItem
   *
   * @param outputItem The output item from the model response
   * @return Parsed RunItem or null if type not recognized
   */
  private static RunItem parseOutputItem(Object outputItem) {
    if (outputItem == null) {
      return null;
    }

    if (outputItem instanceof RunItem) {
      return (RunItem) outputItem;
    }

    if (outputItem instanceof String) {
      return RunMessageOutputItem.builder().content(outputItem).role("assistant").build();
    }

    // Handle ResponseOutputMessage - store the original message object for conversation history.
    if (outputItem instanceof ResponseOutputMessage message) {
      return RunMessageOutputItem.builder()
          .content(message.content().getFirst().asOutputText().text())
          .role("assistant")
          .build();
    }

    if (outputItem instanceof Map) {
      return RunMessageOutputItem.builder()
          .content(ConversionUtils.getOutputItemMapContentMessage((Map<String, Object>) outputItem))
          .role("assistant")
          .build();
    }

    if (outputItem instanceof ResponseReasoningItem reasoning) {
      String reasoningText = extractReasoningText(reasoning);
      return RunReasoningItem.builder().content(reasoningText).build();
    }

    if (outputItem instanceof ResponseFunctionToolCall functionCall) {
      Object parameters = parseToolArguments(functionCall.arguments());

      return RunToolCallItem.builder()
          .id(functionCall.callId())
          .name(functionCall.name())
          .parameters(parameters)
          .additionalProperties(functionCall._additionalProperties())
          .build();
    }

    // Handle hosted tool calls
    if (outputItem instanceof ResponseFunctionWebSearch webSearchCall) {
      return RunToolCallItem.builder()
          .id(webSearchCall.id())
          .name("web_search")
          .parameters(webSearchCall.action())
          .additionalProperties(webSearchCall._additionalProperties())
          .build();
    }

    if (outputItem instanceof ResponseOutputItem.ImageGenerationCall imageGenCall) {
      return RunToolCallItem.builder()
          .id(imageGenCall.id())
          .name("image_generation")
          .parameters(null)
          .additionalProperties(imageGenCall._additionalProperties())
          .build();
    }

    // TODO: Add handlers for other hosted tool types when class names are confirmed
    // For now, if it's an unknown type (likely another hosted tool), convert to message
    return RunMessageOutputItem.builder().content(outputItem).role("assistant").build();
  }

  /** Parse tool call arguments from JSON string */
  private static Object parseToolArguments(String argumentsJson) {
    if (argumentsJson == null || argumentsJson.isEmpty()) {
      return null;
    }

    return SerializationUtils.deserializeFromJson(argumentsJson);
  }

  /**
   * Extract the final output from a list of RunItems Content can be a String (for text responses)
   * or a typed object (for structured outputs).
   *
   * @param items List of RunItems from conversation
   * @return The final output, or null if none found
   */
  public static Object extractFinalOutput(List<RunItem> items) {
    if (items == null || items.isEmpty()) {
      return null;
    }

    for (int i = items.size() - 1; i >= 0; i--) {
      RunItem item = items.get(i);
      if (item instanceof RunMessageOutputItem messageItem) {
        Object content = messageItem.getContent();

        if (content instanceof ResponseOutputMessage message) {
          return message.content().stream()
              .flatMap(c -> c.outputText().stream())
              .map(ResponseOutputText::text)
              .findFirst()
              .orElse("");
        }

        return content;
      }
    }

    return null;
  }

  private static final JsonValue.Visitor<Double> TO_DOUBLE =
      new JsonValue.Visitor<>() {
        private static Double parseDoubleOrNull(String s) {
          try {
            return Double.parseDouble(s.trim());
          } catch (Exception e) {
            return null;
          }
        }

        @Override
        public Double visitNull() {
          return null;
        }

        @Override
        public Double visitMissing() {
          return null;
        }

        @Override
        public Double visitBoolean(boolean v) {
          return null;
        }

        @Override
        public Double visitNumber(Number v) {
          return v.doubleValue();
        }

        @Override
        public Double visitString(String v) {
          return parseDoubleOrNull(v);
        }

        @Override
        public Double visitArray(List<? extends JsonValue> values) {
          return null;
        }

        @Override
        public Double visitObject(Map<String, ? extends JsonValue> values) {
          return null;
        }

        @Override
        public Double visitDefault() {
          return null;
        }
      };

  private static Double safeDouble(java.util.function.Supplier<Long> s) {
    try {
      return (double) s.get();
    } catch (Exception e) {
      return null;
    }
  }

  private static Double jsonNumber(JsonValue value) {
    if (value == null) return null;
    try {
      return value.accept(TO_DOUBLE);
    } catch (Exception e) {
      return null;
    }
  }

  public static Usage extractUsage(com.openai.models.responses.ResponseUsage apiUsage) {
    if (apiUsage == null) {
      return Usage.empty();
    }
    Usage.UsageBuilder usage =
        Usage.builder()
            .inputTokens((double) apiUsage.inputTokens())
            .outputTokens((double) apiUsage.outputTokens())
            .totalTokens((double) apiUsage.totalTokens());

    Map<String, Double> inputTokensDetails = new HashMap<>();
    Double cached = safeDouble(() -> apiUsage.inputTokensDetails().cachedTokens());
    if (cached != null) inputTokensDetails.put("cached_tokens", cached);
    apiUsage
        .inputTokensDetails()
        ._additionalProperties()
        .forEach(
            (key, value) -> {
              Double val = jsonNumber(value);
              if (val != null) inputTokensDetails.put(key, val);
            });

    Map<String, Double> outTokensDetails = new HashMap<>();
    Double reasoning = safeDouble(() -> apiUsage.outputTokensDetails().reasoningTokens());
    if (reasoning != null) outTokensDetails.put("reasoning_tokens", reasoning);
    apiUsage
        .outputTokensDetails()
        ._additionalProperties()
        .forEach(
            (key, value) -> {
              Double val = jsonNumber(value);
              if (val != null) outTokensDetails.put(key, val);
            });

    if (!inputTokensDetails.isEmpty()) usage.inputTokensDetails(List.of(inputTokensDetails));
    if (!outTokensDetails.isEmpty()) usage.outputTokensDetails(List.of(outTokensDetails));

    return usage.build();
  }

  private static final String[] REASONING_KEYS = {"reasoning_content", "reasoning", "thinking"};

  public static String extractReasoningText(ResponseReasoningItem reasoning) {
    StringBuilder sb = new StringBuilder();

    List<ResponseReasoningItem.Summary> summary = reasoning.summary();
    for (ResponseReasoningItem.Summary part : summary) {
      appendLine(sb, part == null ? null : part.text());
    }

    reasoning
        .content()
        .ifPresent(
            parts -> {
              for (ResponseReasoningItem.Content part : parts) {
                appendLine(sb, part == null ? null : part.text());
              }
            });

    if (sb.isEmpty()) {
      Map<String, JsonValue> extra = reasoning._additionalProperties();
      for (String key : REASONING_KEYS) {
        String text = asText(extra.get(key));
        if (!text.isEmpty()) return text;
      }
      for (JsonValue part : asJsonList(extra.get("parts"))) {
        appendLine(sb, asText(asJsonMap(part).get("text")));
      }
    }

    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  private static List<JsonValue> asJsonList(JsonValue value) {
    if (value == null) return Collections.emptyList();
    Object list = value.asArray().orElse(null);
    return list instanceof List<?> ? (List<JsonValue>) list : Collections.emptyList();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, JsonValue> asJsonMap(JsonValue value) {
    if (value == null) return Collections.emptyMap();
    Object map = value.asObject().orElse(null);
    return map instanceof Map<?, ?> ? (Map<String, JsonValue>) map : Collections.emptyMap();
  }

  private static String asText(JsonValue value) {
    if (value == null || value.isNull() || value.isMissing()) return "";
    try {
      return value.asStringOrThrow();
    } catch (RuntimeException e) {
      return "";
    }
  }

  private static void appendLine(StringBuilder sb, String text) {
    if (text == null || text.isEmpty()) return;
    if (!sb.isEmpty()) sb.append('\n');
    sb.append(text);
  }
}
