package com.isldevs.portfolio.auth;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class Attempt {

    public final int count;
    public final Instant until;

    public Attempt(int count, Instant until) {
        this.count = count;
        this.until = until;
    }

    public int count() {
        return count;
    }

    public Instant until() {
        return until;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (Attempt) obj;
        return this.count == that.count &&
                Objects.equals(this.until, that.until);
    }

    @Override
    public int hashCode() {
        return Objects.hash(count, until);
    }

    @Override
    public String toString() {
        return "Attempt[" +
                "count=" + count + ", " +
                "until=" + until + ']';
    }

    public static final Map<String, Attempt> attempts =
            Collections.synchronizedMap(
                    new LinkedHashMap<>() {
                        @Override
                        protected boolean removeEldestEntry(Map.Entry<String, Attempt> e) {
                            return size() > 10000;
                        }
                    });
}
