package com.isldevs.portfolio.auth.repository;

import com.isldevs.portfolio.auth.entity.User;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByUsername(String username);
}
