package ru.ifmo.rain;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class ClassFinder extends SimpleFileVisitor<Path> {
    private final TrieState root = new TrieState("");
    private final Map<String, Set<String>> fullNames = new HashMap<>();

    private static final Comparator<String> RESULT_COMPARATOR =
            Comparator.comparing(ClassFinder::getName).thenComparing(String::compareTo);

    private Path prefix;
    private String pattern;
    private boolean insensitive;
    private Set<String> result;

    public ClassFinder(Path root, String mode) throws IOException {
        if (mode.equals("-r")) {
            this.prefix = root;
            Files.walkFileTree(root, this);
        } else if (mode.equals("-f")) {
            Files.readAllLines(root).forEach(this::addName);
        } else throw new IllegalArgumentException("Expected mode '-r' or '-f'");
    }

    ClassFinder(Set<String> names) { names.forEach(this::addName); }

    void addName(String fullName) {
        String clazzName = getName(fullName);
        fullNames.computeIfAbsent(clazzName, s -> new HashSet<>()).add(fullName);
        TrieState current = root;
        for (char c: clazzName.toCharArray()) { current = current.by(c); }
        current.terminal = true;
    }

    private static String getName(String fullName) {
        int p = fullName.lastIndexOf('.');
        return (p == -1)? fullName: fullName.substring(p + 1);
    }

    private boolean lower(char c) {
        return Character.isLowerCase(c) || Character.isDigit(c);
    }

    private void matchUpper(TrieState current, int p, char c) {
        Set<TrieState> closure = new HashSet<>();
        current.lowerClosure(closure, c);
        closure.forEach(s -> match(p + 1, s));
    }

    private void matchLower(TrieState current, int p, char c) {
        TrieState next = current.next.get(c);
        if (next != null) match(p + 1, next);
    }

    private void matchWildCard(TrieState current, int p) {
        Set<TrieState> closure = new HashSet<>();
        if (p == pattern.length()) {
            current.wildcardClosure(closure);
            closure.forEach(s -> result.addAll(fullNames.get(s.prefix)));
        } else {
            char after = pattern.charAt(p);
            assert Character.isLetterOrDigit(after);
            if (insensitive && Character.isLetter(after)) {
                current.wildcardClosure(closure, Character.toLowerCase(after));
                current.wildcardClosure(closure, Character.toUpperCase(after));
            } else {
                current.wildcardClosure(closure, after);
            }
            closure.forEach(s -> match(p + 1, s));
        }
    }

    void match(int p, TrieState current) {
        char c = pattern.charAt(p);
        if (insensitive && Character.isLetter(c)) {
            matchLower(current, p, Character.toLowerCase(c));
            matchUpper(current, p, Character.toUpperCase(c));
        }
        else if (lower(c)) { matchLower(current, p, c); }
        else if (Character.isUpperCase(c)) { matchUpper(current, p, c); }
        else if (c == '*') { matchWildCard(current, p + 1); }
        else throw new IllegalArgumentException("Unknown pattern symbol: '" + c + "'");
    }

    private boolean checkSense() {
        for (char c : pattern.toCharArray()) {
            if (Character.isUpperCase(c)) return false;
        }
        return true;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        if (file.toString().endsWith(".java")) {
            String fullName = prefix.relativize(file).toString().replace(File.separatorChar, '.');
            addName(fullName.substring(0, fullName.length() - 5));  // remove .java
        }
        return FileVisitResult.CONTINUE;
    }

    public Set<String> search(String pattern) {
        this.pattern = '*' + pattern + '*';
        this.insensitive = checkSense();
        this.result = new TreeSet<>(RESULT_COMPARATOR);
        this.match(0, root);
        return result;
    }

    public static void main(String[] args) {
        if (args == null || args.length != 3) {
            System.err.println("Expected <mode> <root> <pattern>");
            return;
        }
        try {
            Set<String> result = new ClassFinder(Path.of(args[1]), args[0]).search(args[2]);
            result.forEach(System.out::println);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
