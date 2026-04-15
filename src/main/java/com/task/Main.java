package com.task;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Ошибка: Укажите путь к входному файлу");
            System.exit(1);
        }

        String inputFile = args[0];
        long startTime = System.currentTimeMillis();

        try {
            Path inputPath = Paths.get(inputFile);
            if (!Files.exists(inputPath)) {
                System.err.println("Ошибка: Файл не найден");
                System.exit(1);
            }

            System.out.println("=== НАЧАЛО ОБРАБОТКИ ===");

            // Шаг 1: Чтение уникальных строк с использованием LinkedHashMap для экономии
            System.out.println("\n[1/3] Чтение файла...");
            List<String> lines = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            try (BufferedReader reader = Files.newBufferedReader(inputPath)) {
                String line;
                long lineCount = 0;

                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty() && isValidLine(line)) {
                        if (!seen.contains(line)) {
                            seen.add(line);
                            lines.add(line);
                        }
                    }
                    lineCount++;

                    if (lineCount % 200000 == 0) {
                        System.out.printf("  Обработано: %d, уникальных: %d%n", lineCount, lines.size());
                        printMemoryUsage();
                    }
                }
            }

            System.out.println("Уникальных строк: " + lines.size());

            if (lines.isEmpty()) {
                System.out.println("Нет данных");
                return;
            }

            // Шаг 2: Группировка с оптимизированной структурой
            System.out.println("\n[2/3] Построение групп...");
            List<Set<Integer>> groups = findGroupsOptimized(lines);

            // Шаг 3: Формирование результата
            System.out.println("\n[3/3] Формирование результата...");
            List<Set<String>> resultGroups = new ArrayList<>();
            for (Set<Integer> groupIndices : groups) {
                Set<String> groupLines = new HashSet<>();
                for (int idx : groupIndices) {
                    groupLines.add(lines.get(idx));
                }
                resultGroups.add(groupLines);
            }

            // Сортировка по размеру
            resultGroups.sort((g1, g2) -> Integer.compare(g2.size(), g1.size()));

            long groupsWithMultiple = resultGroups.stream().filter(g -> g.size() > 1).count();

            // Запись результата
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get("output.txt"))) {
                int groupNum = 1;
                for (Set<String> group : resultGroups) {
                    if (group.size() > 1) {
                        writer.write("Группа " + groupNum++);
                        writer.newLine();
                        for (String line : group) {
                            writer.write(line);
                            writer.newLine();
                        }
                        writer.newLine();
                    }
                }
            }

            long duration = System.currentTimeMillis() - startTime;

            System.out.println("\n=== РЕЗУЛЬТАТ ===");
            System.out.println("Групп с >1 элементом: " + groupsWithMultiple);
            System.out.println("Всего групп: " + resultGroups.size());
            System.out.println("Время: " + duration / 1000.0 + " sec");

        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<Set<Integer>> findGroupsOptimized(List<String> lines) {
        int n = lines.size();

        // DSU массивы (компактные)
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;

        // ОПТИМИЗАЦИЯ: Используем HashMap с Integer[] вместо List
        // и очищаем после использования
        Map<String, Integer> firstOccurrence = new HashMap<>();

        System.out.println("  Построение связей...");

        for (int i = 0; i < n; i++) {
            String line = lines.get(i);
            String[] cols = line.split(";", -1);

            for (int pos = 0; pos < cols.length; pos++) {
                String val = cols[pos].trim();
                if (!val.isEmpty()) {
                    String key = val + "|" + pos;
                    Integer existing = firstOccurrence.get(key);
                    if (existing != null) {
                        // Объединяем текущую строку с первой встреченной
                        union(parent, existing, i);
                    } else {
                        firstOccurrence.put(key, i);
                    }
                }
            }

            if ((i + 1) % 100000 == 0) {
                System.out.printf("  Прогресс: %d/%d (%.1f%%), память: %.1f MB%n",
                        i + 1, n, 100.0 * (i + 1) / n, getUsedMemoryMB());
            }
        }

        // Очищаем маппинг для экономии памяти
        firstOccurrence.clear();
        System.gc();

        // Сборка групп
        System.out.println("  Сборка групп...");
        Map<Integer, Set<Integer>> groupMap = new HashMap<>();

        for (int i = 0; i < n; i++) {
            int root = find(parent, i);
            groupMap.computeIfAbsent(root, k -> new HashSet<>()).add(i);
        }

        return new ArrayList<>(groupMap.values());
    }

    private static int find(int[] parent, int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]];
            x = parent[x];
        }
        return x;
    }

    private static void union(int[] parent, int a, int b) {
        int rootA = find(parent, a);
        int rootB = find(parent, b);
        if (rootA != rootB) {
            parent[rootB] = rootA;
        }
    }

    private static boolean isValidLine(String line) {
        int quoteCount = 0;
        for (char c : line.toCharArray()) {
            if (c == '"') quoteCount++;
        }
        return quoteCount % 2 == 0;
    }

    private static void printMemoryUsage() {
        double used = getUsedMemoryMB();
        System.out.printf("  Память: %.1f MB / 1024.0 MB (%.1f%%)%n", used, (used / 1024) * 100);
    }

    private static double getUsedMemoryMB() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024.0 * 1024);
    }
}