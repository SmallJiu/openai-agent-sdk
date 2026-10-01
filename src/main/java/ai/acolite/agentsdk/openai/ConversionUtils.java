package ai.acolite.agentsdk.openai;

import ai.acolite.agentsdk.core.*;
import com.openai.models.responses.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ConversionUtils
 *
 * <p>Static utility methods for converting between SDK types and OpenAI API types.
 */
public class ConversionUtils {

  private ConversionUtils() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Convert conversation items to OpenAI ResponseInputItem format.
   *
   * <p>Handles: - String messages → text input (USER role) - RunToolCallItem → function call input
   * - RunToolCallOutputItem → function call output - RunMessageOutputItem → skipped (assistant
   * messages are outputs)
   *
   * @param items List of conversation items (String, RunItem, etc.)
   * @return List of ResponseInputItem objects for the API
   */
  public static List<ResponseInputItem> convertToResponseInputItems(List<Object> items) {
    List<ResponseInputItem> inputItems = new ArrayList<>();

    for (Object item : items) {
      if (item instanceof RunMessageInputItem inputItem) {
        inputItems.add(
            ResponseInputItem.ofMessage(
                ResponseInputItem.Message.builder()
                    .addInputTextContent(inputItem.getContent())
                    .role(ResponseInputItem.Message.Role.USER)
                    .build()));
      } else if (item instanceof String text) {
        inputItems.add(
            ResponseInputItem.ofMessage(
                ResponseInputItem.Message.builder()
                    .addInputTextContent(text)
                    .role(ResponseInputItem.Message.Role.USER)
                    .build()));
      } else if (item instanceof RunReasoningItem reasoning) {
        inputItems.add(
            ResponseInputItem.ofReasoning(
                ResponseReasoningItem.builder()
                    .id("reasoning-" + reasoning.hashCode())
                    .addSummary(
                        ResponseReasoningItem.Summary.builder()
                            .text(reasoning.getContent())
                            .build())
                    .build()));
      } else if (item instanceof RunToolCallItem toolCall) {
        ResponseFunctionToolCall functionCall =
            ResponseFunctionToolCall.builder()
                .callId(toolCall.getId())
                .name(toolCall.getName())
                .arguments(SerializationUtils.serializeToJson(toolCall.getParameters()))
                .additionalProperties(toolCall.getAdditionalProperties())
                .build();
        inputItems.add(ResponseInputItem.ofFunctionCall(functionCall));
      } else if (item instanceof RunToolCallOutputItem toolOutput) {
        ResponseInputItem.FunctionCallOutput output =
            ResponseInputItem.FunctionCallOutput.builder()
                .callId(toolOutput.getToolCallId())
                .outputAsJson(toolOutput.getResult())
                .build();
        inputItems.add(ResponseInputItem.ofFunctionCallOutput(output));
      } else if (item instanceof RunMessageOutputItem messageOutput) {
        Object content = messageOutput.getContent();
        if (content instanceof ResponseOutputMessage responseMessage) {
          inputItems.add(ResponseInputItem.ofResponseOutputMessage(responseMessage));
        } else if (content instanceof String text) {
          inputItems.add(convertToResponseInputItem(text));
        } else if (content instanceof Map) {
          String messageText = getOutputItemMapContentMessage((Map<String, Object>) content);
          if (messageText != null) {
            inputItems.add(convertToResponseInputItem(messageText));
          }
        }
      }
    }
    return inputItems;
  }

  public static ResponseInputItem convertToResponseInputItem(String text) {
    return ResponseInputItem.ofResponseOutputMessage(
        ResponseOutputMessage.builder()
            .id("message-" + System.currentTimeMillis())
            .status(ResponseOutputMessage.Status.COMPLETED)
            .addContent(
                ResponseOutputMessage.Content.ofOutputText(
                    ResponseOutputText.builder().text(text).annotations(List.of()).build()))
            .build());
  }

  public static String getOutputItemMapContentMessage(RunMessageOutputItem outputItem) {
    Object content = outputItem.getContent();
    if (content instanceof Map) {
      return getOutputItemMapContentMessage((Map<String, Object>) content);
    }
    return null;
  }

  public static String getOutputItemMapContentMessage(Map<String, Object> messageOutput) {
    if (messageOutput.containsKey("content")) {
      List<Map<String, Object>> contentList =
          (List<Map<String, Object>>) messageOutput.get("content");
      if (!contentList.isEmpty()) {
        Map<String, Object> firstContent = contentList.get(0);
        if (firstContent.containsKey("text")) {
          return (String) firstContent.get("text");
        }
      }
    }
    return null;
  }
}
