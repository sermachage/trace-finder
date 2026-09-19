package com.tracefinder.processing;

import com.tracefinder.model.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogAnalyzerTest {

    private Rulebook testRulebook() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        map.put("INFO", 1);
        map.put("WARN", 3);
        map.put("ERROR", 5);
        map.put("ALERT", 9);
        return new Rulebook(map);
    }

    @Test
    void activitySummaryCountsKnownLevelsInRulebookOrder() {
        List<LogEntry> entries = List.of(
                entry(1, "INFO"), entry(2, "WARN"), entry(3, "WARN"), entry(4, "CRIT")
        );

        AnalysisResult result = new LogAnalyzer().analyze(entries, testRulebook(), List.of());

        assertEquals(List.of("INFO", "WARN", "ERROR", "ALERT"),
                List.copyOf(result.activitySummary().keySet()));
        assertEquals(1, result.activitySummary().get("INFO"));
        assertEquals(2, result.activitySummary().get("WARN"));
        assertEquals(0, result.activitySummary().get("ERROR"));
    }

    @Test
    void flaggedEntriesExcludeInfoAndSortBySeverityThenTime() {
        List<LogEntry> entries = List.of(
                entry(1, "INFO"),
                entryAt(2, "WARN", LocalDateTime.of(2024, 3, 15, 10, 0)),
                entryAt(3, "ALERT", LocalDateTime.of(2024, 3, 15, 9, 0)),
                entryAt(4, "WARN", LocalDateTime.of(2024, 3, 15, 8, 0))
        );

        AnalysisResult result = new LogAnalyzer().analyze(entries, testRulebook(), List.of());

        List<LogEntry> flagged = result.flaggedEntries();
        assertEquals(3, flagged.size());
        assertEquals("ALERT", flagged.get(0).level());   // highest severity first
        assertEquals("WARN", flagged.get(1).level());    // then chronological among WARNs
        assertEquals(8, flagged.get(1).timestamp().getHour());
        assertEquals(10, flagged.get(2).timestamp().getHour());
    }

    @Test
    void suspiciousByIpSortsByCountDescThenIpAsc() {
        List<LogEntry> entries = List.of(
                entryWithIp(1, "INFO", "10.0.0.1"),
                entryWithIp(2, "INFO", "10.0.0.1"),
                entryWithIp(3, "INFO", "10.0.0.2"),
                entryWithIp(4, "INFO", "10.0.0.2")
        );

        AnalysisResult result = new LogAnalyzer().analyze(entries, testRulebook(), List.of());

        List<String> ipsInOrder = List.copyOf(result.suspiciousByIp().keySet());
        assertEquals("10.0.0.1", ipsInOrder.get(0)); // tie broken alphabetically
        assertEquals(2, result.suspiciousByIp().get("10.0.0.1"));
    }

    @Test
    void unknownPatternsCollectedInLineOrder() {
        List<LogEntry> entries = List.of(entry(5, "CRIT"), entry(2, "TRACE"), entry(1, "INFO"));

        AnalysisResult result = new LogAnalyzer().analyze(entries, testRulebook(), List.of());

        assertEquals(2, result.unknownPatterns().size());
        assertEquals(2, result.unknownPatterns().get(0).lineNumber());
        assertEquals(5, result.unknownPatterns().get(1).lineNumber());
    }

    private LogEntry entry(int lineNumber, String level) {
        return entryAt(lineNumber, level, LocalDateTime.of(2024, 3, 15, 0, 0));
    }

    private LogEntry entryAt(int lineNumber, String level, LocalDateTime timestamp) {
        return new LogEntry(lineNumber, timestamp, level, "1.2.3.4", "/x", "y", "raw" + lineNumber);
    }

    private LogEntry entryWithIp(int lineNumber, String level, String ip) {
        return new LogEntry(lineNumber, LocalDateTime.of(2024, 3, 15, 0, 0), level, ip, "/x", "y", "raw" + lineNumber);
    }
}
