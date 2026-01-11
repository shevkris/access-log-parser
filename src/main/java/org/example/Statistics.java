package org.example;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Statistics {
    private int totalTraffic;
    private LocalDateTime minTime;
    private LocalDateTime maxTime;
    private int entryCount;

    private final Set<String> existingPages;

    private final Map<String, Integer> osStatistics;

    public Statistics() {
        this.totalTraffic = 0;
        this.minTime = null;
        this.maxTime = null;
        this.entryCount = 0;
        this.existingPages = new HashSet<>();
        this.osStatistics = new HashMap<>();
    }

    public void addEntry(LogEntry entry) {

        this.totalTraffic += entry.getResponseSize();
        this.entryCount++;

        LocalDateTime entryTime = entry.getTimestamp();

        if (this.minTime == null || entryTime.isBefore(this.minTime)) {
            this.minTime = entryTime;
        }

        if (this.maxTime == null || entryTime.isAfter(this.maxTime)) {
            this.maxTime = entryTime;
        }

        // Добавляем страницу в список существующих, если код ответа 200
        if (entry.getStatusCode() == 200) {
            String path = entry.getPath();
            if (path != null && !path.isEmpty()) {
                existingPages.add(path);
            }
        }
        // Обновляем статистику операционных систем
        UserAgent userAgent = entry.getUserAgent();
        if (userAgent != null) {
            String osName = userAgent.getOperatingSystem().name();
            osStatistics.put(osName, osStatistics.getOrDefault(osName, 0) + 1);
        }
    }

    // Метод для возврата списка всех существующих страниц сайта (с кодом 200)
    public Set<String> getExistingPages() {
        // Возвращаем копию множества, чтобы защитить исходные данные
        return new HashSet<>(existingPages);
    }

    // Метод для возврата статистики операционных систем (доли от 0 до 1)
    public Map<String, Double> getOsStatistics() {
        Map<String, Double> result = new HashMap<>();

        if (osStatistics.isEmpty() || entryCount == 0) {
            return result;
        }

        // Рассчитываем общее количество записей с информацией об ОС
        int totalOsEntries = osStatistics.values().stream().mapToInt(Integer::intValue).sum();

        // Рассчитываем долю для каждой операционной системы
        for (Map.Entry<String, Integer> entry : osStatistics.entrySet()) {
            String osName = entry.getKey();
            int count = entry.getValue();
            double percentage = (double) count / totalOsEntries;
            result.put(osName, percentage);
        }
        return result;
    }

    public double getTrafficRate() {
        if (minTime == null || maxTime == null || totalTraffic == 0) {
            return 0.0;
        }

        Duration duration = Duration.between(minTime, maxTime);
        long hours = duration.toHours();

        if (hours == 0) {
            hours = 1;
        }

        return (double) totalTraffic / hours;
    }

    public int getTotalTraffic() {
        return totalTraffic;
    }

    public LocalDateTime getMinTime() {
        return minTime;
    }

    public LocalDateTime getMaxTime() {
        return maxTime;
    }

    public int getEntryCount() {
        return entryCount;
    }

    public double getDurationInHours() {
        if (minTime == null || maxTime == null) {
            return 0.0;
        }

        Duration duration = Duration.between(minTime, maxTime);
        return duration.toMinutes() / 60.0; // Точное значение в часах с дробной частью
    }

    public String getStatisticsReport() {
        StringBuilder report = new StringBuilder();
        report.append(String.format("   Всего записей: %,d\n", entryCount));
        report.append(String.format("   Общий объем трафика: %,d байт\n", totalTraffic));

        if (minTime != null && maxTime != null) {
            report.append(String.format("   Период анализа: с %s по %s\n",
                    minTime, maxTime));

            Duration duration = Duration.between(minTime, maxTime);
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;

            report.append(String.format("   Продолжительность: %d ч. %d мин.\n", hours, minutes));

            double trafficRate = getTrafficRate();
            report.append(String.format("   Средний трафик: %,.2f байт/час\n", trafficRate));
        }

        report.append(String.format("   Количество уникальных страниц: %d\n", existingPages.size()));

        if (!existingPages.isEmpty()) {
            int pageNumber = 1;
            for (String page : existingPages) {
                report.append(String.format("   %d. %s\n", pageNumber, page));
                pageNumber++;
            }
        } else {
            report.append("   Нет страниц с кодом ответа 200\n");
        }

        Map<String, Double> osStats = getOsStatistics();
        if (!osStats.isEmpty()) {
            report.append(String.format("   Уникальных операционных систем: %d\n", osStats.size()));
            report.append("   Распределение по операционным системам:\n");

            // Сортируем по убыванию доли для наглядности
            osStats.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .forEach(entry -> {
                        String osName = entry.getKey();
                        double percentage = entry.getValue() * 100;
                        int count = osStatistics.get(osName);
                        report.append(String.format("   - %s: %.2f%% (%d записей)\n",
                                osName, percentage, count));
                    });
        } else {
            report.append("   Нет данных об операционных системах\n");
        }
        return report.toString();
    }
}