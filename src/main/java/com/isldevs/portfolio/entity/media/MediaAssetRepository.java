package com.isldevs.portfolio.entity.media;

import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface MediaAssetRepository extends JpaRepository<Media, UUID>, JpaSpecificationExecutor<Media> {}
