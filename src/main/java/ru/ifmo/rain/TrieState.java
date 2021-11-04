package ru.ifmo.rain;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TrieState {
    public final String prefix;
    public final Map<Character, TrieState> next = new HashMap<>();
    public boolean terminal = false;

    TrieState(String prefix) {
        this.prefix = prefix;
    }

    void lowerClosure(Set<TrieState> result, char to) {
        if (next.containsKey(to)) result.add(next.get(to));
        next.entrySet().stream().filter(e -> Character.isLowerCase(e.getKey()))
                .forEach(s -> s.getValue().lowerClosure(result, to));
    }

    void wildcardClosure(Set<TrieState> result, char to) {
        if (next.containsKey(to)) result.add(next.get(to));
        next.values().forEach(s -> s.wildcardClosure(result, to));
    }

    void wildcardClosure(Set<TrieState> result) {
        if (terminal) result.add(this);
        next.values().forEach(s -> s.wildcardClosure(result));
    }

    TrieState by(char c) {
        return next.computeIfAbsent(c, x -> new TrieState(prefix + x));
    }
}
