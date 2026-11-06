package com.isldevs.portfolio.media.service;

import com.isldevs.portfolio.media.dto.MediaResponse;
import java.nio.file.Path;

public record Download(MediaResponse asset, Path path) {}
