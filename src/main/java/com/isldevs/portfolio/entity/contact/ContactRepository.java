package com.isldevs.portfolio.entity.contact;

import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface ContactRepository extends JpaRepository<Contact, UUID>, JpaSpecificationExecutor<Contact> {
  long countByStatus(Status status);
}
