package com.tracefinder.reading;

import com.tracefinder.model.LogEntry;
import com.tracefinder.model.MalformedLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class LogFileReader {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Result of reading + parsing the whole log file. */
    public record ParsedLogFile(List<LogEntry> entries, List<MalformedLine> malformedLines) {}

    public ParsedLogFile readAndParse(Path logFilePath) throws IOException {
        List<String> rawLines = Files.readAllLines(logFilePath);

        List<LogEntry> entries = new ArrayList<>();
        List<MalformedLine> malformedLines = new ArrayList<>();

        for (int i = 0; i < rawLines.size(); i++) {
            int lineNumber = i + 1; // 1-indexed, blank lines included
            String rawLine = rawLines.get(i);

            LogEntry entry = tryParseLine(lineNumber, rawLine);
            if (entry != null) {
                entries.add(entry);
            } else {
                malformedLines.add(new MalformedLine(lineNumber, rawLine));
            }
        }

        return new ParsedLogFile(entries, malformedLines);
    }

    private LogEntry tryParseLine(int lineNumber, String rawLine) {
        if (rawLine.isBlank()) {
            return null;
        }

        String[] parts = rawLine.split(" \\| ", -1);
        if (parts.length != 5) {
            return null; // wrong delimiter or missing field(s)
        }

        String timestampText = parts[0];
        String level = parts[1];
        String ip = parts[2];
        String resource = parts[3];
        String action = parts[4];

        LocalDateTime timestamp;
        try {
            timestamp = LocalDateTime.parse(timestampText, TIMESTAMP_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }

        return new LogEntry(lineNumber, timestamp, level, ip, resource, action, rawLine);
    }
}
