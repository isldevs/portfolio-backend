package com.isldevs.portfolio.contact.service;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Small, bounded in-process protection for a single-instance VPS. */
@Component
public class ContactRateLimiter {

  private static final long WINDOW = Duration.ofHours(1).toMillis();
  private static final int LIMIT = 5;
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
  private final Clock clock;

  public ContactRateLimiter() {
    this(Clock.systemUTC());
  }

  ContactRateLimiter(Clock clock) {
    this.clock = clock;
  }

  public synchronized boolean allow(String address) {
    long now = clock.millis();
    // Limit attacker-controlled cardinality as well as request count.
    if (windows.size() > 1000)
      windows.entrySet().removeIf(e -> now - e.getValue().start() >= WINDOW);

    Window window = windows.get(address);
    if (window == null || now - window.start() >= WINDOW) {
      if (windows.size() >= 10000 && window == null) return false;
      windows.put(address, new Window(now, 1));
      return true;
    }

    if (window.count() >= LIMIT) return false;
    windows.put(address, new Window(window.start(), window.count() + 1));
    return true;
  }
}
