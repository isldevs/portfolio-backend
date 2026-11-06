package com.isldevs.portfolio.profile.repository;

import com.isldevs.portfolio.profile.entity.Profile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {}
