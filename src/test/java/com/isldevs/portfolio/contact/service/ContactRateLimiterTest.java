package com.isldevs.portfolio.contact.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ContactRateLimiterTest {
  @Test
  void limitsAnAddressWhileKeepingOtherVisitorsIndependent() {
    ContactRateLimiter limiter =
        new ContactRateLimiter(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    for (int i = 0; i < 5; i++) assertThat(limiter.allow("192.0.2.1")).isTrue();
    assertThat(limiter.allow("192.0.2.1")).isFalse();
    assertThat(limiter.allow("192.0.2.2")).isTrue();
  }

  @Test
  void permitsRequestsAgainAfterTheWindowExpires() {
    MutableClock clock = new MutableClock();
    ContactRateLimiter limiter = new ContactRateLimiter(clock);
    for (int i = 0; i < 5; i++) limiter.allow("client");
    assertThat(limiter.allow("client")).isFalse();
    clock.instant = clock.instant.plusSeconds(3600);
    assertThat(limiter.allow("client")).isTrue();
  }

  private static class MutableClock extends Clock {
    private Instant instant = Instant.parse("2026-01-01T00:00:00Z");

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }
}
