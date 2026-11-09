package ai.acolite.agentsdk.core.contents;

import com.openai.models.responses.ResponseInputContent;
import com.openai.models.responses.ResponseInputText;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter
@SuperBuilder
@Jacksonized
public class TextContent implements IAgentContent {
  private final String content;

  @Override
  public ResponseInputContent toInputContent() {
    return ResponseInputContent.ofInputText(
        ResponseInputText.builder().text(this.getContent()).build());
  }
}
