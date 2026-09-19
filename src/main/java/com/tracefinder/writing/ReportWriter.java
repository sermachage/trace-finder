package com.tracefinder.writing;

import com.tracefinder.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ReportWriter {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void write(AnalysisResult result, Path outputPath) throws IOException {
        Files.writeString(outputPath, format(result));
    }

    private String format(AnalysisResult result) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== Log Analysis Report ===\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(TIMESTAMP_FORMAT)).append("\n\n");

        sb.append("--- Activity Summary ---\n");
        for (Map.Entry<String, Integer> entry : result.activitySummary().entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        sb.append("\n");

        sb.append("--- Flagged Entries ---\n");
        for (LogEntry entry : result.flaggedEntries()) {
            sb.append(entry.rawLine()).append("\n");
        }
        sb.append("\n");

        sb.append("--- Suspicious Activity by IP ---\n");
        for (Map.Entry<String, Integer> entry : result.suspiciousByIp().entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(" entries\n");
        }
        sb.append("\n");

        sb.append("--- Unknown Patterns ---\n");
        for (UnknownPatternLine line : result.unknownPatterns()) {
            sb.append("Line ").append(line.lineNumber()).append(": ").append(line.rawLine()).append("\n");
        }
        sb.append("\n");

        sb.append("--- Malformed Lines ---\n");
        for (MalformedLine line : result.malformedLines()) {
            sb.append(formatMalformedLine(line)).append("\n");
        }

        return sb.toString();
    }

    private String formatMalformedLine(MalformedLine line) {
        if (line.rawLine().isBlank()) {
            return "Line " + line.lineNumber() + ":";
        }
        return "Line " + line.lineNumber() + ": " + line.rawLine();
    }
}
