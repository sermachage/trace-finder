package com.tracefinder.processing;

import com.tracefinder.model.*;

import java.util.*;

public class LogAnalyzer {

    public AnalysisResult analyze(
            List<LogEntry> entries,
            Rulebook rulebook,
            List<MalformedLine> malformedLines
    ) {
        LinkedHashMap<String, Integer> activitySummary = buildActivitySummary(entries, rulebook);
        List<LogEntry> flaggedEntries = buildFlaggedEntries(entries, rulebook);
        LinkedHashMap<String, Integer> suspiciousByIp = buildSuspiciousByIp(entries);
        List<UnknownPatternLine> unknownPatterns = buildUnknownPatterns(entries, rulebook);

        return new AnalysisResult(
                activitySummary, flaggedEntries, suspiciousByIp, unknownPatterns, malformedLines);
    }

    private LinkedHashMap<String, Integer> buildActivitySummary(List<LogEntry> entries, Rulebook rulebook) {
        LinkedHashMap<String, Integer> summary = new LinkedHashMap<>();
        for (String level : rulebook.levelsInFileOrder()) {
            summary.put(level, 0);
        }
        for (LogEntry entry : entries) {
            if (rulebook.isKnownLevel(entry.level())) {
                summary.merge(entry.level(), 1, Integer::sum);
            }
        }
        return summary;
    }

    private List<LogEntry> buildFlaggedEntries(List<LogEntry> entries, Rulebook rulebook) {
        int minSeverity = Collections.min(rulebook.asMap().values());

        List<LogEntry> flagged = new ArrayList<>();
        for (LogEntry entry : entries) {
            if (rulebook.isKnownLevel(entry.level())
                    && rulebook.severityOf(entry.level()) > minSeverity) {
                flagged.add(entry);
            }
        }

        // highest severity first; chronological within the same severity
        flagged.sort(
                Comparator.<LogEntry>comparingInt(e -> rulebook.severityOf(e.level()))
                        .reversed()
                        .thenComparing(LogEntry::timestamp)
        );

        return flagged;
    }

    private LinkedHashMap<String, Integer> buildSuspiciousByIp(List<LogEntry> entries) {
        Map<String, Integer> counts = new HashMap<>();
        for (LogEntry entry : entries) {
            counts.merge(entry.ip(), 1, Integer::sum);
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort(
                Map.Entry.<String, Integer>comparingByValue()
                        .reversed()
                        .thenComparing(Map.Entry.comparingByKey())
        );

        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : sorted) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private List<UnknownPatternLine> buildUnknownPatterns(List<LogEntry> entries, Rulebook rulebook) {
        List<UnknownPatternLine> unknown = new ArrayList<>();
        for (LogEntry entry : entries) {
            if (!rulebook.isKnownLevel(entry.level())) {
                unknown.add(new UnknownPatternLine(entry.lineNumber(), entry.rawLine()));
            }
        }
        unknown.sort(Comparator.comparingInt(UnknownPatternLine::lineNumber));
        return unknown;
    }
}
