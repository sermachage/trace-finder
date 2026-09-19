package com.tracefinder.reading;

import com.tracefinder.model.LogEntry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LogFileReaderTest {

    @Test
    void parsesWellFormedLine() throws IOException {
        Path temp = Files.createTempFile("log", ".txt");
        Files.writeString(temp, "2024-03-15 02:14:08 | INFO | 192.168.1.45 | /login | success");

        LogFileReader.ParsedLogFile result = new LogFileReader().readAndParse(temp);

        assertEquals(1, result.entries().size());
        assertEquals(0, result.malformedLines().size());

        LogEntry entry = result.entries().get(0);
        assertEquals("INFO", entry.level());
        assertEquals("192.168.1.45", entry.ip());
        assertEquals("/login", entry.resource());
        assertEquals("success", entry.action());
    }

    @Test
    void flagsBlankLineAsMalformed() throws IOException {
        Path temp = Files.createTempFile("log", ".txt");
        Files.writeString(temp, "\n");

        LogFileReader.ParsedLogFile result = new LogFileReader().readAndParse(temp);

        assertEquals(0, result.entries().size());
        assertEquals(1, result.malformedLines().size());
        assertEquals("", result.malformedLines().get(0).rawLine());
    }

    @Test
    void flagsMissingFieldAsMalformed() throws IOException {
        Path temp = Files.createTempFile("log", ".txt");
        Files.writeString(temp, "2024-03-15 05:00:00 | WARN | 203.0.113.42 | /login");

        LogFileReader.ParsedLogFile result = new LogFileReader().readAndParse(temp);

        assertEquals(0, result.entries().size());
        assertEquals(1, result.malformedLines().size());
    }

    @Test
    void flagsWrongDelimiterAsMalformed() throws IOException {
        Path temp = Files.createTempFile("log", ".txt");
        Files.writeString(temp, "2024-03-15 05:05:00 / INFO / 192.168.1.45 / /home / accessed");

        LogFileReader.ParsedLogFile result = new LogFileReader().readAndParse(temp);

        assertEquals(1, result.malformedLines().size());
    }

    @Test
    void flagsBadTimestampAsMalformed() throws IOException {
        Path temp = Files.createTempFile("log", ".txt");
        Files.writeString(temp, "not-a-timestamp | INFO | 192.168.1.45 | /home | accessed");

        LogFileReader.ParsedLogFile result = new LogFileReader().readAndParse(temp);

        assertEquals(1, result.malformedLines().size());
    }

    @Test
    void keepsUnknownLevelAsValidEntry() throws IOException {
        Path temp = Files.createTempFile("log", ".txt");
        Files.writeString(temp, "2024-03-15 03:42:11 | CRIT | 203.0.113.42 | /admin | escalated");

        LogFileReader.ParsedLogFile result = new LogFileReader().readAndParse(temp);

        assertEquals(1, result.entries().size());
        assertEquals(0, result.malformedLines().size());
        assertEquals("CRIT", result.entries().get(0).level());
    }
}
