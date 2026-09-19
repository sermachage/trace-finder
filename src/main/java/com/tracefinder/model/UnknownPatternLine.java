package com.tracefinder.model;

/**
 * A line that parsed fine (5 valid fields + valid timestamp) but whose level
 * doesn't exactly match anything in the rulebook.
 */
public record UnknownPatternLine(int lineNumber, String rawLine) {
}
