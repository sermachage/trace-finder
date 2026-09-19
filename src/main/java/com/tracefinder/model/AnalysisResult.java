package com.tracefinder.model;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Everything the Processing stage produces, ready for Writing to format.
 */
public class AnalysisResult {

    private final LinkedHashMap<String, Integer> activitySummary;   // level -> count, rulebook order
    private final List<LogEntry> flaggedEntries;                    // non-INFO known entries
    private final LinkedHashMap<String, Integer> suspiciousByIp;    // ip -> count, sorted desc
    private final List<UnknownPatternLine> unknownPatterns;         // line-number order
    private final List<MalformedLine> malformedLines;               // line-number order

    public AnalysisResult(
            LinkedHashMap<String, Integer> activitySummary,
            List<LogEntry> flaggedEntries,
            LinkedHashMap<String, Integer> suspiciousByIp,
            List<UnknownPatternLine> unknownPatterns,
            List<MalformedLine> malformedLines
    ) {
        this.activitySummary = activitySummary;
        this.flaggedEntries = flaggedEntries;
        this.suspiciousByIp = suspiciousByIp;
        this.unknownPatterns = unknownPatterns;
        this.malformedLines = malformedLines;
    }

    public LinkedHashMap<String, Integer> activitySummary() { return activitySummary; }
    public List<LogEntry> flaggedEntries() { return flaggedEntries; }
    public LinkedHashMap<String, Integer> suspiciousByIp() { return suspiciousByIp; }
    public List<UnknownPatternLine> unknownPatterns() { return unknownPatterns; }
    public List<MalformedLine> malformedLines() { return malformedLines; }
}
