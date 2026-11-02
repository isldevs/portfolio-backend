package com.isldevs.portfolio.auth;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {
  Optional<AdminUser> findByUsername(String username);
}
