package io.codekvast.s3_uploader;

import java.io.File;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@EqualsAndHashCode(of = {"length", "lastModified"})
public class FileStatus {
  long length;
  long lastModified;

  public static FileStatus of(File file) {
    return FileStatus.builder().length(file.length()).lastModified(file.lastModified()).build();
  }
}
