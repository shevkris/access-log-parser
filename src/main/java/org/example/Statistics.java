package org.example;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Statistics {
    private int totalTraffic;
    private LocalDateTime minTime;
    private LocalDateTime maxTime;
    private int entryCount;

    private final Set<String> existingPages;

    private final Map<String, Integer> osStatistics;

    private final Set<String> notFoundPages;
    private final Map<String, Integer> browserStatistics;

    public Statistics() {
        this.totalTraffic = 0;
        this.minTime = null;
        this.maxTime = null;
        this.entryCount = 0;
        this.existingPages = new HashSet<>();
        this.osStatistics = new HashMap<>();
        this.notFoundPages = new HashSet<>();
        this.browserStatistics = new HashMap<>();
    }

    public void addEntry(LogEntry entry) {
        // Добавляем трафик
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

        if (entry.getStatusCode() == 404) {
            String path = entry.getPath();
            if (path != null && !path.isEmpty()) {
                notFoundPages.add(path);
            }
        }
        // Обновляем статистику операционных систем
        UserAgent userAgent = entry.getUserAgent();
        if (userAgent != null) {
            String osName = userAgent.getOperatingSystem().name();
            osStatistics.put(osName, osStatistics.getOrDefault(osName, 0) + 1);
            String browserName = userAgent.getBrowser().name();
            browserStatistics.put(browserName, browserStatistics.getOrDefault(browserName, 0) + 1);
        }
    }

    // Метод для возврата списка всех существующих страниц сайта (с кодом 200)
    public Set<String> getExistingPages() {
        // Возвращаем копию множества, чтобы защитить исходные данные
        return new HashSet<>(existingPages);
    }
    public Set<String> getNotFoundPages() {
        return new HashSet<>(notFoundPages);
    }

    public Map<String, Double> getBrowserStatistics() {
        Map<String, Double> result = new HashMap<>();

        if (browserStatistics.isEmpty()) {
            return result;
        }

        int totalBrowserEntries = 0;
        for (Integer count : browserStatistics.values()) {
            totalBrowserEntries += count;
        }

        for (Map.Entry<String, Integer> entry : browserStatistics.entrySet()) {
            String browserName = entry.getKey();
            int count = entry.getValue();
            double percentage = (double) count / totalBrowserEntries;
            result.put(browserName, percentage);
        }

        return result;
    }

    // Метод для возврата статистики операционных систем (доли от 0 до 1)
    public Map<String, Double> getOsStatistics() {
        Map<String, Double> result = new HashMap<>();

        if (osStatistics.isEmpty() || entryCount == 0) {
            return result;
        }

        // Рассчитываем общее количество записей с информацией об ОС
        int totalOsEntries =
                osStatistics
                        .values()
                        .stream()
                        .mapToInt(Integer::intValue)
                        .sum();

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
            long days = duration.toDays();
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;

            report.append(String.format("Продолжительность периода: %d дн., %d ч., %d мин.\n",
                    days, hours, minutes));

            double trafficRate = getTrafficRate();
            report.append(String.format("Средний трафик: %.2f байт/час\n", getTrafficRate()));
        } else {
            report.append("Нет данных для анализа\n");
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
        report.append(String.format("   Количество уникальных несуществуюших страниц (код 404): %d\n", notFoundPages.size()));

        if (!notFoundPages.isEmpty()) {
            int pageNumber = 1;
            for (String page : notFoundPages) {
                report.append(String.format("   %d. %s\n", pageNumber, page));
                pageNumber++;
            }
        } else {
            report.append("   Нет страниц с кодом ответа 404\n");
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

        Map<String, Double> browserStats = getBrowserStatistics();
        if (!browserStats.isEmpty()) {
            report.append(String.format("   Уникальных браузеров: %d\n", browserStats.size()));
            report.append("   Распределение по браузерам:\n");

            browserStats.entrySet().stream()
                    .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                    .forEach(entry -> {
                        String browserName = entry.getKey();
                        double percentage = entry.getValue() * 100;
                        int count = browserStatistics.get(browserName);
                        report.append(String.format("   - %s: %.2f%% (%d записей)\n",
                                browserName, percentage, count));
                    });
        } else {
            report.append("   Нет данных о браузерах\n");
        }
        return report.toString();
    }
}
