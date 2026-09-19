package com.tracefinder.model;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Everything the Processing stage produces, ready for Writing to format.
 *
 * @param activitySummary level -> count, rulebook order
 * @param flaggedEntries  non-INFO known entries
 * @param suspiciousByIp  ip -> count, sorted desc
 * @param unknownPatterns line-number order
 * @param malformedLines  line-number order
 */
public record AnalysisResult(LinkedHashMap<String, Integer> activitySummary, List<LogEntry> flaggedEntries,
                             LinkedHashMap<String, Integer> suspiciousByIp, List<UnknownPatternLine> unknownPatterns,
                             List<MalformedLine> malformedLines) {

}
