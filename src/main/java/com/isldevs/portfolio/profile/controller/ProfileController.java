package com.isldevs.portfolio.profile.controller;

import com.isldevs.portfolio.profile.dto.ProfileResponse;
import com.isldevs.portfolio.profile.service.ProfileService;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ProfileController {
  private final ProfileService service;

  public ProfileController(ProfileService service) {
    this.service = service;
  }

  @GetMapping({"/public/profile", "/admin/profile"})
  public ProfileResponse get() {
    return service.get();
  }

  @PutMapping("/admin/profile")
  public ProfileResponse update(@RequestBody Map<String, Object> input) {
    return service.update(input);
  }
}
