package ai.acolite.agentsdk.core.contents;

import com.openai.models.responses.ResponseInputContent;
import com.openai.models.responses.ResponseInputImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter
@SuperBuilder
@Jacksonized
public class ImageContent implements IAgentContent {
  public static ImageContent ofUrlOrBase64(String data, ResponseInputImage.Detail.Known detail) {
    return ImageContent.builder().image(data).type(Type.IMAGE).detail(detail).build();
  }

  public static ImageContent ofId(String id, ResponseInputImage.Detail.Known detail) {
    return ImageContent.builder().image(id).type(Type.ID).detail(detail).build();
  }

  public static ImageContent ofFile(InputStream stream, ResponseInputImage.Detail.Known detail)
      throws IOException {
    return ofFile("image/png", stream.readAllBytes(), detail);
  }

  public static ImageContent ofFile(File file, ResponseInputImage.Detail.Known detail)
      throws IOException {
    Path path = file.toPath();
    String mimeType = Files.probeContentType(path);
    if (mimeType == null) mimeType = "image/png";
    return ofFile(mimeType, Files.readAllBytes(path), detail);
  }

  public static ImageContent ofFile(
      String mimeType, InputStream stream, ResponseInputImage.Detail.Known detail)
      throws IOException {
    return ofFile(mimeType, stream.readAllBytes(), detail);
  }

  public static ImageContent ofFile(
      String mimeType, byte[] bytes, ResponseInputImage.Detail.Known detail) {
    String data =
        "data:%s;base64,%s".formatted(mimeType, Base64.getEncoder().encodeToString(bytes));
    return ImageContent.builder().image(data).type(Type.IMAGE).detail(detail).build();
  }

  private final Type type;
  private final String image;

  @Builder.Default
  private final ResponseInputImage.Detail.Known detail = ResponseInputImage.Detail.AUTO.known();

  @Override
  public ResponseInputContent toInputContent() {
    ResponseInputImage.Detail detail =
        this.getDetail() == null
            ? ResponseInputImage.Detail.AUTO
            : ResponseInputImage.Detail.of(this.getDetail().name().toLowerCase(Locale.ROOT));
    return ResponseInputContent.ofInputImage(this.getType().convert(this.getImage(), detail));
  }

  public enum Type {
    IMAGE((data, detail) -> ResponseInputImage.builder().imageUrl(data).detail(detail).build()),
    ID((data, detail) -> ResponseInputImage.builder().fileId(data).detail(detail).build());
    private final Converter converter;

    Type(Converter converter) {
      this.converter = converter;
    }

    public ResponseInputImage convert(String data, ResponseInputImage.Detail detail) {
      return converter.convert(data, detail);
    }

    private interface Converter {
      ResponseInputImage convert(String data, ResponseInputImage.Detail detail);
    }
  }
}
