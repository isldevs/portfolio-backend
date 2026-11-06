package com.isldevs.portfolio.contact.repository;

import com.isldevs.portfolio.contact.entity.Contact;
import com.isldevs.portfolio.contact.entity.Status;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface ContactRepository
    extends JpaRepository<Contact, UUID>, JpaSpecificationExecutor<Contact> {
  long countByStatus(Status status);
}
