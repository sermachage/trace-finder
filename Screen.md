# 1. What This Project Does

Tracefinder is a command-line Java application.

It:

1. Reads a rulebook containing log levels and severity scores.
2. Reads a log file.
3. Converts valid log lines into Java objects.
4. Finds suspicious, unknown, and malformed entries.
5. Creates a text report.

The overall flow is:

```text
rules.csv
   ↓
RulebookReader
   ↓
Rulebook object

logs.txt
   ↓
LogFileReader
   ↓
LogEntry objects
MalformedLine objects
   ↓
LogAnalyzer
   ↓
AnalysisResult
   ↓
ReportWriter
   ↓
output-report.txt
```

The project separates these jobs so that each class has one main responsibility.

---

# 2. Project Folder Structure

```text
trace-finder/
├── pom.xml
├── README.md
├── make_guide_pdf.py
├── trace-finder-project-guide.html
├── sample-data/
│   ├── logs.txt
│   └── rules.csv
└── src/
    ├── main/
    │   └── java/
    │       └── com/tracefinder/
    │           ├── Main.java
    │           ├── model/
    │           ├── reading/
    │           ├── processing/
    │           └── writing/
    └── test/
        └── java/
            └── com/tracefinder/
```

## Why `java`?

This is Maven's standard location for application code.

Maven automatically knows:

- Java files under `java` are production code.
- Java files under `java` are test code.
- Dependencies should be downloaded from `pom.xml`.

## Why `com/tracefinder`?

This is the folder structure for the Java package:

```java
package com.tracefinder;
```

Java packages organize classes and prevent naming conflicts.

The subpackages represent responsibilities:

- `model`: data objects
- `reading`: reading and parsing files
- `processing`: applying business rules
- `writing`: creating the output report

This is called **separation of concerns**.

---

# 3. The Starting Point: `Main.java`

The program begins in `Main.java:13`:

```java
public static void main(String[] args)
```

This is the special method Java looks for when starting an application.

The program expects three command-line arguments:

```text
1. Rulebook file
2. Log file
3. Output report file
```

For example:

```bash
java -jar target/tracefinder-1.0-SNAPSHOT.jar \
  sample-data/rules.csv \
  sample-data/logs.txt \
  sample-data/output-report.txt
```

`Main` does not perform the actual analysis. It coordinates other classes:

```java
Rulebook rulebook = readRulebookOrExit(rulebookPath);

LogFileReader.ParsedLogFile parsedLogFile =
        readLogFileOrExit(logFilePath);

AnalysisResult result = new LogAnalyzer()
        .analyze(
                parsedLogFile.entries(),
                rulebook,
                parsedLogFile.malformedLines()
        );

writeReportOrExit(result, outputPath);
```

In plain language:

1. Read the rules.
2. Read the logs.
3. Analyze the data.
4. Write the report.

`Main` is the application's **orchestrator**.

---

# 4. The Model Classes

The model classes represent information in the application.

They are located in:

```text
src/main/java/com/tracefinder/model/
```

## `LogEntry.java`

`LogEntry.java` represents one valid log line.

A log line such as:

```text
2024-03-15 02:14:08 | INFO | 192.168.1.45 | /login | success
```

becomes a `LogEntry` containing:

```text
lineNumber: 1
timestamp: 2024-03-15 02:14:08
level: INFO
ip: 192.168.1.45
resource: /login
action: success
rawLine: the original line
```

It is a Java **record**:

```java
public record LogEntry(
        int lineNumber,
        LocalDateTime timestamp,
        String level,
        String ip,
        String resource,
        String action,
        String rawLine
) {
}
```

A record automatically provides:

- A constructor
- Getter-like methods such as `entry.level()`
- `equals`
- `hashCode`
- `toString`

This is useful for simple data objects.

---

## `MalformedLine.java`

`MalformedLine.java` represents a line that could not be parsed.

Examples:

- Blank line
- Missing field
- Wrong separator
- Invalid timestamp

It stores:

```java
lineNumber
rawLine
```

For example, this line is malformed:

```text
2024-03-15 05:00:00 | WARN | 203.0.113.42 | /login
```

It only has four fields instead of five.

---

## `UnknownPatternLine.java`

`UnknownPatternLine.java` represents a line that is structurally valid but uses an unknown log level.

For example:

```text
2024-03-15 03:42:11 | CRIT | 203.0.113.42 | /admin | escalated
```

The format is valid, but `CRIT` is not in the rulebook.

This is different from a malformed line:

- Malformed: the line cannot be understood.
- Unknown pattern: the line can be understood, but its level is not recognized.

---

## `Rulebook.java`

`Rulebook.java` stores the known log levels and their severity scores.

The sample rulebook is:

```text
INFO  → 1
WARN  → 3
ERROR → 5
ALERT → 9
```

Internally it uses:

```java
LinkedHashMap<String, Integer>
```

This means:

```text
String  = log level
Integer = severity score
```

It uses `LinkedHashMap` instead of `HashMap` because it preserves the original order:

```text
INFO, WARN, ERROR, ALERT
```

Important methods include:

```java
isKnownLevel("WARN")
```

Checks whether a level exists.

```java
severityOf("ERROR")
```

Returns the score for a level.

```java
levelsInFileOrder()
```

Returns the levels in rulebook order.

---

## `AnalysisResult.java`

`AnalysisResult.java` is the container for everything produced by the analyzer.

It contains:

- Activity counts
- Flagged log entries
- Suspicious activity by IP
- Unknown patterns
- Malformed lines

This object is passed from `LogAnalyzer` to `ReportWriter`.

That gives the project a clean handoff:

```text
LogAnalyzer → AnalysisResult → ReportWriter
```

---

# 5. Reading the Rulebook

## `RulebookReader.java`

`RulebookReader.java` reads `rules.csv`.

It:

1. Reads all lines.
2. Skips the header.
3. Skips blank lines.
4. Splits each line at the comma.
5. Converts the severity text into an integer.
6. Builds a `Rulebook`.

This line:

```java
String[] parts = line.split(",", -1);
```

turns this:

```text
WARN,3
```

into:

```text
parts[0] = "WARN"
parts[1] = "3"
```

Then:

```java
score = Integer.parseInt(scoreText);
```

converts the text `"3"` into the number `3`.

## Custom exception

The class defines:

```java
public static class RulebookFormatException extends Exception
```

This is a **custom checked exception**.

It is used when the file exists but contains invalid content, such as:

```text
WARN,not-a-number
```

The exception travels back to `Main`, where it is caught and displayed to the user.

---

# 6. Reading the Log File

## `LogFileReader.java`

`LogFileReader.java` reads and parses `logs.txt`.

A valid line must contain exactly five fields:

```text
timestamp | level | ip | resource | action
```

The parser uses:

```java
String[] parts = rawLine.split(" \\| ", -1);
```

The separator is:

```text
space + | + space
```

For every line, it checks:

1. Is the line blank?
2. Does it have exactly five fields?
3. Is the timestamp valid?

The timestamp is parsed using:

```java
LocalDateTime.parse(timestampText, TIMESTAMP_FORMAT)
```

The expected format is:

```text
yyyy-MM-dd HH:mm:ss
```

If parsing succeeds, the method creates a `LogEntry`.

If parsing fails, the line becomes a `MalformedLine`.

## `ParsedLogFile`

The reader returns a nested record:

```java
public record ParsedLogFile(
        List<LogEntry> entries,
        List<MalformedLine> malformedLines
) {}
```

This lets one method return both:

```text
Valid entries
Malformed lines
```

The reader does not decide whether a level is known. That decision belongs to the processing layer.

---

# 7. The Main Business Logic

## `LogAnalyzer.java`

`LogAnalyzer.java` performs the actual analysis.

Its public method is:

```java
analyze(entries, rulebook, malformedLines)
```

It produces four main results.

## A. Activity summary

It counts known levels:

```text
INFO: 20
WARN: 10
ERROR: 5
ALERT: 4
```

It first adds every rulebook level with a count of zero. Then it counts matching entries.

This code is important:

```java
summary.merge(entry.level(), 1, Integer::sum);
```

It means:

- If the level is not present, add it with value `1`.
- If it already exists, increase its value by `1`.

Unknown levels such as `CRIT` are not included in this summary.

---

## B. Flagged entries

The analyzer finds the lowest severity:

```java
int minSeverity = Collections.min(rulebook.asMap().values());
```

With the sample data:

```text
INFO = 1
WARN = 3
ERROR = 5
ALERT = 9
```

The minimum is `1`.

Every known level with a severity higher than `1` is flagged:

```text
WARN
ERROR
ALERT
```

The entries are sorted by:

1. Highest severity first
2. Earliest timestamp first when severity is equal

This uses a Java **Comparator**:

```java
Comparator
    .comparingInt(...)
    .reversed()
    .thenComparing(...)
```

This is an example of **method chaining**.

---

## C. Suspicious activity by IP

The analyzer counts how many valid entries came from each IP address.

It first uses a `HashMap` because counting does not require order:

```java
Map<String, Integer> counts = new HashMap<>();
```

Then it sorts the results:

1. Highest count first
2. Alphabetical IP order for ties

Finally, it places the sorted results into a `LinkedHashMap`, because `LinkedHashMap` remembers insertion order.

This is a good example of choosing a collection based on the job:

- `HashMap`: fast lookup and counting
- `LinkedHashMap`: preserve a deliberate display order
- `List`: preserve a sequence of objects

---

## D. Unknown patterns

A valid entry is an unknown pattern when:

```java
!rulebook.isKnownLevel(entry.level())
```

For example:

```text
CRIT
DEBUG
TRACE
info
warn
FATAL
```

The comparison is case-sensitive.

Therefore:

```text
INFO
```

is known, but:

```text
info
```

is unknown.

Unknown lines are sorted by their original line number.

---

# 8. Writing the Report

## `ReportWriter.java`

`ReportWriter.java` receives the completed `AnalysisResult`.

It builds a large string using:

```java
StringBuilder sb = new StringBuilder();
```

The report contains sections such as:

```text
=== Log Analysis Report ===

--- Activity Summary ---

--- Flagged Entries ---

--- Suspicious Activity by IP ---

--- Unknown Patterns ---

--- Malformed Lines ---
```

The writer does not recalculate anything. It simply formats the results.

Finally:

```java
Files.writeString(outputPath, format(result));
```

saves the report to disk.

This is another example of separation of concerns:

- `LogAnalyzer` decides what the results are.
- `ReportWriter` decides how the results look.

---

# 9. Tests

The tests are under:

```text
src/test/java/
```

## `LogAnalyzerTest.java`

`LogAnalyzerTest.java` tests:

- Activity counting
- Rulebook order
- Flagged-entry sorting
- Severity sorting
- Timestamp sorting
- IP counting
- Unknown-pattern ordering

JUnit methods use annotations such as:

```java
@Test
```

Assertions include:

```java
assertEquals(expected, actual);
```

The test helpers avoid repeating long `LogEntry` constructor calls.

## `LogFileReaderTest.java`

`LogFileReaderTest.java` tests:

- Valid log lines
- Blank lines
- Missing fields
- Wrong delimiters
- Bad timestamps
- Unknown levels

It uses temporary files:

```java
Path temp = Files.createTempFile("log", ".txt");
```

There is currently no `ReportWriterTest.java`, so report formatting is not directly tested.

---

# 10. Maven and `pom.xml`

`pom.xml` is Maven's project configuration file.

It tells Maven:

- The project name and version
- Java version: Java 17
- The JUnit dependency
- How to run tests
- How to build a runnable JAR

The JUnit dependency has:

```xml
<scope>test</scope>
```

That means JUnit is needed for tests but is not part of the normal application.

The Shade Plugin creates a runnable JAR and sets:

```text
com.tracefinder.Main
```

as the entry point.

Useful commands:

```bash
mvn test
```

Runs the tests.

```bash
mvn clean package
```

Deletes old build output, compiles the project, runs tests, and creates the JAR.

---

# 11. Supporting Files

## `README.md`

Explains:

- What the program does
- The architecture
- Business rules
- How to build it
- How to run it
- How to test it

It is documentation, not running code.

## `rules.csv`

Provides example severity rules:

```text
INFO,1
WARN,3
ERROR,5
ALERT,9
```

## `logs.txt`

Provides example log entries, including:

- Valid entries
- Unknown levels
- Blank lines
- Missing fields
- Wrong delimiters
- Invalid timestamps

## `trace-finder-project-guide.html`

A browser-readable learning guide explaining the project.

## `make_guide_pdf.py`

A Python script that reads the HTML guide and creates a PDF version.

It is separate from the Java application. It helps document the project but is not used when Tracefinder analyzes logs.

---

# 12. Important Java Concepts Used

## Classes and objects

A class is a blueprint:

```java
class RulebookReader
```

An object is an actual instance:

```java
new RulebookReader()
```

## Packages

Packages group related classes:

```java
com.tracefinder.reading
com.tracefinder.processing
```

## Records

Records are compact data classes:

```java
public record MalformedLine(int lineNumber, String rawLine) {}
```

## Generics

Generics describe what a collection stores:

```java
List<LogEntry>
Map<String, Integer>
```

This means:

- `List<LogEntry>` stores `LogEntry` objects.
- `Map<String, Integer>` maps text to numbers.

## Exceptions

Exceptions represent problems:

```java
catch (IOException e)
```

The program catches file-reading and file-writing errors rather than crashing with an unexplained message.

## Interfaces

The code uses interfaces such as:

```java
List
Map
```

while choosing implementations such as:

```java
ArrayList
HashMap
LinkedHashMap
```

This lets the code describe what it needs while choosing how it should work internally.

## Lambdas

This is a lambda:

```java
e -> rulebook.severityOf(e.level())
```

It means:

> Take an entry called `e`, then return its severity.

## Method references

This is a method reference:

```java
LogEntry::timestamp
```

It is a shorter way of referring to:

```java
entry -> entry.timestamp()
```

## Immutability

Records are mainly used as fixed data values. Once a `LogEntry` is created, its fields are not normally changed.

## Encapsulation

Fields such as the rulebook map are private:

```java
private final LinkedHashMap<String, Integer> severityByLevel;
```

Other classes interact with the object through methods instead of directly changing its internal data.

---

# 13. Why the Project Is Organized This Way

Without this structure, one giant method would need to:

1. Open files.
2. Parse CSV.
3. Parse log lines.
4. Validate timestamps.
5. Count levels.
6. Sort entries.
7. Count IPs.
8. Format the report.
9. Handle errors.

That would be difficult to understand and test.

Instead:

```text
Main
  coordinates

Readers
  read and parse

Models
  represent data

Analyzer
  applies business rules

Writer
  produces output
```

Each part has a clear job. This makes the program easier to learn, modify, debug, and test.