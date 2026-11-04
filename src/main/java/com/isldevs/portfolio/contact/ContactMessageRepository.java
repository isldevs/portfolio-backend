package com.isldevs.portfolio.contact;

import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface ContactMessageRepository
    extends JpaRepository<ContactMessage, UUID>, JpaSpecificationExecutor<ContactMessage> {
  long countByStatus(ContactService.Status status);
}
