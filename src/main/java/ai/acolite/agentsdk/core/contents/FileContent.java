package ai.acolite.agentsdk.core.contents;

import com.openai.models.responses.ResponseInputContent;
import com.openai.models.responses.ResponseInputFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Getter
@SuperBuilder
@Jacksonized
public class FileContent implements IAgentContent {
  public static FileContent ofUrl(String name, String url) {
    return FileContent.builder().name(name).data(url).type(Type.URL).build();
  }

  public static FileContent ofId(String name, String id) {
    return FileContent.builder().name(name).data(id).type(Type.ID).build();
  }

  public static FileContent ofFile(File file) throws IOException {
    String mimeType = Files.probeContentType(file.toPath());
    if (mimeType == null) mimeType = "text/plain";
    return ofFile(file.getName(), mimeType, Files.readAllBytes(file.toPath()));
  }

  public static FileContent ofFile(String name, String mimeType, byte[] bytes) {
    String data =
        "data:%s;base64,%s".formatted(mimeType, Base64.getEncoder().encodeToString(bytes));
    return FileContent.builder().name(name).data(data).type(Type.FILE).build();
  }

  private final Type type;
  private final String name;
  private final String data;

  @Override
  public ResponseInputContent toInputContent() {
    return ResponseInputContent.ofInputFile(this.getType().convert(this.getName(), this.getData()));
  }

  public enum Type {
    FILE((name, data) -> ResponseInputFile.builder().filename(name).fileData(data).build()),
    URL((name, data) -> ResponseInputFile.builder().filename(name).fileUrl(data).build()),
    ID((name, data) -> ResponseInputFile.builder().filename(name).fileId(data).build());
    private final Converter converter;

    Type(Converter converter) {
      this.converter = converter;
    }

    public ResponseInputFile convert(String name, String data) {
      return converter.convert(name, data);
    }

    private interface Converter {
      ResponseInputFile convert(String name, String data);
    }
  }
}
