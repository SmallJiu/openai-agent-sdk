package ai.acolite.agentsdk.core.contents;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.openai.models.responses.ResponseInputContent;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = TextContent.class, name = "text_input"),
  @JsonSubTypes.Type(value = ImageContent.class, name = "image_input"),
  @JsonSubTypes.Type(value = FileContent.class, name = "file_input")
})
public interface IAgentContent {
  ResponseInputContent toInputContent();
}
