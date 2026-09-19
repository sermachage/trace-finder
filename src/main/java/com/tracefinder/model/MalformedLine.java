package com.tracefinder.model;

/**
 * A line that could not be parsed at all (wrong field count, bad timestamp, blank, etc.)
 */
public record MalformedLine(int lineNumber, String rawLine) {
}
