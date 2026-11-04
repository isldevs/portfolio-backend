package com.isldevs.portfolio.entity.media;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Replace this adapter for S3-compatible deployments; keep generated keys and visibility checks.
 */
public interface MediaStorage {
  void put(String key, byte[] content) throws IOException;

  Path path(String key) throws IOException;

  void delete(String key) throws IOException;
}
