package com.isldevs.portfolio.content.controller;

import com.isldevs.portfolio.audit.dto.AuditResponse;
import com.isldevs.portfolio.content.dto.DashboardResponse;
import com.isldevs.portfolio.content.service.DashboardService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
public class DashboardController {
  private final DashboardService service;

  public DashboardController(DashboardService service) {
    this.service = service;
  }

  @GetMapping("/dashboard")
  public DashboardResponse dashboard() {
    return service.dashboard();
  }

  @GetMapping("/audit")
  public List<AuditResponse> audit() {
    return service.audit();
  }
}
