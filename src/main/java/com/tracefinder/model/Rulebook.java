package com.tracefinder.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps each known log level to its severity score, preserving the order
 * levels appeared in the rulebook CSV (INFO, WARN, ERROR, ALERT, ...).
 */
public class Rulebook {

    private final LinkedHashMap<String, Integer> severityByLevel;

    public Rulebook(LinkedHashMap<String, Integer> severityByLevel) {
        this.severityByLevel = severityByLevel;
    }

    public boolean isKnownLevel(String level) {
        return severityByLevel.containsKey(level);
    }

    public int severityOf(String level) {
        return severityByLevel.get(level);
    }

    /** Levels in the order they appeared in the CSV (ascending severity). */
    public Iterable<String> levelsInFileOrder() {
        return severityByLevel.keySet();
    }

    public Map<String, Integer> asMap() {
        return severityByLevel;
    }
}
