package com.isldevs.portfolio.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.isldevs.portfolio.entity.media.LocalMediaStorage;
import com.isldevs.portfolio.entity.media.MediaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalMediaStorageTest {
  @TempDir Path temporary;

  @Test
  void storesOnlyGeneratedKeysWithoutOverwritingExistingFiles() throws Exception {
    LocalMediaStorage storage = new LocalMediaStorage(temporary.resolve("media").toString());
    String key = UUID.randomUUID() + ".png";
    storage.put(key, new byte[] {1, 2, 3});
    assertThat(Files.readAllBytes(storage.path(key))).containsExactly(1, 2, 3);
    assertThatThrownBy(() -> storage.put(key, new byte[] {9})).isInstanceOf(IOException.class);
    assertThatThrownBy(() -> storage.path("../secret.txt")).isInstanceOf(IOException.class);
    storage.delete(key);
    assertThat(storage.path(key)).doesNotExist();
  }

  @Test
  void refusesSymlinkTargetsAndSanitizesDownloadNames() throws Exception {
    LocalMediaStorage storage = new LocalMediaStorage(temporary.resolve("media").toString());
    String key = UUID.randomUUID() + ".pdf";
    Path outside = Files.writeString(temporary.resolve("outside"), "private");
    Files.createSymbolicLink(temporary.resolve("media").resolve(key), outside);
    assertThatThrownBy(() -> storage.path(key)).isInstanceOf(IOException.class);
    assertThat(MediaService.safeName("C:\\Users\\person\\resume\r\n.pdf", "pdf"))
        .isEqualTo("resume__.pdf");
  }
}
