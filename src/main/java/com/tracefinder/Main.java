package com.tracefinder;

import com.tracefinder.model.AnalysisResult;
import com.tracefinder.model.Rulebook;
import com.tracefinder.processing.LogAnalyzer;
import com.tracefinder.reading.LogFileReader;
import com.tracefinder.reading.RulebookReader;
import com.tracefinder.writing.ReportWriter;

import java.io.IOException;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: tracefinder <rulebook.csv> <log-file> <output-report.txt>");
            System.exit(1);
            return;
        }

        Path rulebookPath = Path.of(args[0]);
        Path logFilePath = Path.of(args[1]);
        Path outputPath = Path.of(args[2]);

        Rulebook rulebook = readRulebookOrExit(rulebookPath);
        LogFileReader.ParsedLogFile parsedLogFile = readLogFileOrExit(logFilePath);

        AnalysisResult result = new LogAnalyzer()
                .analyze(parsedLogFile.entries(), rulebook, parsedLogFile.malformedLines());

        writeReportOrExit(result, outputPath);

        System.out.println("Report written to " + outputPath);
    }

    private static Rulebook readRulebookOrExit(Path rulebookPath) {
        try {
            return new RulebookReader().read(rulebookPath);
        } catch (IOException e) {
            System.err.println("Could not read rulebook file: " + e.getMessage());
            System.exit(2);
        } catch (RulebookReader.RulebookFormatException e) {
            System.err.println("Rulebook problem: " + e.getMessage());
            System.exit(2);
        }
        throw new IllegalStateException("unreachable"); // System.exit already terminated the JVM
    }

    private static LogFileReader.ParsedLogFile readLogFileOrExit(Path logFilePath) {
        try {
            return new LogFileReader().readAndParse(logFilePath);
        } catch (IOException e) {
            System.err.println("Could not read log file: " + e.getMessage());
            System.exit(3);
        }
        throw new IllegalStateException("unreachable");
    }

    private static void writeReportOrExit(AnalysisResult result, Path outputPath) {
        try {
            new ReportWriter().write(result, outputPath);
        } catch (IOException e) {
            System.err.println("Could not write report: " + e.getMessage());
            System.exit(4);
        }
    }
}
