package com.tracefinder.reading;

import com.tracefinder.model.Rulebook;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

public class RulebookReader {

    public static class RulebookFormatException extends Exception {
        public RulebookFormatException(String message) {
            super(message);
        }
    }

    public Rulebook read(Path rulebookPath) throws IOException, RulebookFormatException {
        List<String> lines = Files.readAllLines(rulebookPath);

        if (lines.isEmpty()) {
            throw new RulebookFormatException("Rulebook file is empty.");
        }

        LinkedHashMap<String, Integer> severityByLevel = new LinkedHashMap<>();

        // line 0 is the header: "level,severity_score" — skip it
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }

            String[] parts = line.split(",", -1);
            if (parts.length != 2) {
                throw new RulebookFormatException(
                        "Malformed rulebook row at line " + (i + 1) + ": " + line);
            }

            String level = parts[0].trim();
            String scoreText = parts[1].trim();

            int score;
            try {
                score = Integer.parseInt(scoreText);
            } catch (NumberFormatException e) {
                throw new RulebookFormatException(
                        "Invalid severity score at line " + (i + 1) + ": " + line);
            }

            severityByLevel.put(level, score);
        }

        if (severityByLevel.isEmpty()) {
            throw new RulebookFormatException("Rulebook contains no valid rules.");
        }

        return new Rulebook(severityByLevel);
    }
}
