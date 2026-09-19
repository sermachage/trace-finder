package com.tracefinder.model;

import java.time.LocalDateTime;

/**
 * A single successfully-parsed log line.
 */
public record LogEntry(
        int lineNumber,
        LocalDateTime timestamp,
        String level,
        String ip,
        String resource,
        String action,
        String rawLine
) {
}
