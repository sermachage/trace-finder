# Tracefinder

A CLI tool that reads a log file and a rulebook CSV, analyzes entries
(matching, counting, grouping, flagging), and writes an analysis report.

## Architecture

- `model` — shared data classes (LogEntry, Rulebook, MalformedLine, UnknownPatternLine, AnalysisResult)
- `reading` — parses the rulebook CSV and the log file
- `processing` — matches, counts, groups, and flags entries
- `writing` — formats and writes the report
- `Main` — CLI argument handling and orchestration

## Usage

```
java -jar tracefinder.jar <rulebook.csv> <log-file> <output-report.txt>
```

## Business rules

- A log line is well-formed if it splits into exactly 5 fields on `" | "`
  (`timestamp | level | ip | resource | action`) and the timestamp matches
  `yyyy-MM-dd HH:mm:ss`. Otherwise it's malformed.
- A well-formed line whose level exactly matches (case-sensitive) a level in
  the rulebook is a known entry; otherwise it's an "unknown pattern".
- Any known entry with severity above the lowest severity in the rulebook
  (i.e. not the baseline level, typically INFO) is a "flagged entry".
- Flagged entries are grouped by severity descending, chronological within
  each group.
- "Suspicious Activity by IP" counts all well-formed entries (known and
  unknown level) per IP, sorted by count descending, ties broken by IP
  ascending.
- Unknown patterns and malformed lines are both listed in ascending line
  number order (blank lines count toward line numbering).

## Building and running

```
mvn clean package
java -jar target/tracefinder-1.0-SNAPSHOT.jar sample-data/rules.csv sample-data/logs.txt sample-data/output-report.txt
```

## Running tests

```
mvn test
```
