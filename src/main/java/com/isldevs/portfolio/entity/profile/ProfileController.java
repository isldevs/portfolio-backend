package com.isldevs.portfolio.entity.profile;

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
  public Map<String, Object> get() {
    return service.get();
  }

  @PutMapping("/admin/profile")
  public Map<String, Object> update(@RequestBody Map<String, Object> input) {
    return service.update(input);
  }
}
