package com.isldevs.portfolio.media;

import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface MediaAssetRepository
    extends JpaRepository<MediaAsset, UUID>, JpaSpecificationExecutor<MediaAsset> {}
