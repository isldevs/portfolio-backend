package com.isldevs.portfolio.media.repository;

import com.isldevs.portfolio.media.entity.Media;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface MediaAssetRepository
    extends JpaRepository<Media, UUID>, JpaSpecificationExecutor<Media> {}
