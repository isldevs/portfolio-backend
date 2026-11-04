package com.isldevs.portfolio.entity.media;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalMediaStorage implements MediaStorage {
  private final Path root;

  public LocalMediaStorage(
      @Value("${MEDIA_ROOT:${portfolio.media.directory:${MEDIA_DIRECTORY:./uploads}}}") String root)
      throws IOException {
    Path configured = Path.of(root).toAbsolutePath().normalize();
    if (configured.getFileSystem().supportedFileAttributeViews().contains("posix"))
      Files.createDirectories(
          configured,
          PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
    else Files.createDirectories(configured);
    this.root = configured.toRealPath();
  }

  @Override
  public void put(String key, byte[] content) throws IOException {
    Path target = path(key);
    if (target.getFileSystem().supportedFileAttributeViews().contains("posix")) {
      Files.createFile(
          target,
          PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
      try {
        Files.write(target, content, StandardOpenOption.WRITE);
      } catch (IOException e) {
        Files.deleteIfExists(target);
        throw e;
      }
    } else Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
  }

  @Override
  public Path path(String key) throws IOException {
    if (key == null || !key.matches("[a-f0-9-]{36}\\.(png|jpg|gif|webp|pdf)"))
      throw new IOException("Invalid media key");
    Path path = root.resolve(key).normalize();
    if (!path.getParent().equals(root) || Files.isSymbolicLink(path))
      throw new IOException("Invalid media path");
    if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)
        && !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
      throw new IOException("Media path is not a regular file");
    return path;
  }

  @Override
  public void delete(String key) throws IOException {
    Files.deleteIfExists(path(key));
  }
}
