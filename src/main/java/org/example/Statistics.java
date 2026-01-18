package org.example;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private int humanVisitsCount;
    private int errorRequestsCount;
    private final Set<String> uniqueHumanIPs;
    private final Map<Long, Integer> visitsPerSecond;
    private final Map<String, Integer> visitsPerUser;
    private final Set<String> refererDomains;

    public Statistics() {
        this.totalTraffic = 0;
        this.minTime = null;
        this.maxTime = null;
        this.entryCount = 0;
        this.existingPages = new HashSet<>();
        this.osStatistics = new HashMap<>();
        this.notFoundPages = new HashSet<>();
        this.browserStatistics = new HashMap<>();
        this.humanVisitsCount = 0;
        this.errorRequestsCount = 0;
        this.uniqueHumanIPs = new HashSet<>();
        this.visitsPerSecond = new HashMap<>();
        this.visitsPerUser = new HashMap<>();
        this.refererDomains = new HashSet<>();
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
        // Проверяем, является ли запрос от обычного пользователя (не бота)
        boolean isHuman = !isBot(entry.getUserAgent());
        // Подсчет посещений обычными пользователями и их уникальных IP
        if (isHuman) {
                // Получаем секунду в Unix time
                long second = entryTime.toEpochSecond(ZoneOffset.UTC);

                // Увеличиваем счетчик для данной секунды
                visitsPerSecond.put(second, visitsPerSecond.getOrDefault(second, 0) + 1);

                // Подсчет посещений по пользователям (IP-адресам)
                String ipAddress = entry.getIpAddress();
                if (ipAddress != null && !ipAddress.isEmpty()) {
                    visitsPerUser.put(ipAddress, visitsPerUser.getOrDefault(ipAddress, 0) + 1);
                }
            humanVisitsCount++;
            uniqueHumanIPs.add(ipAddress);
        }

        // 3. Сбор доменов из referer-ов
        String referer = entry.getReferer();
        if (referer != null && !referer.isEmpty() && !referer.equals("-")) {
            String domain = extractDomainFromUrl(referer);
            if (domain != null && !domain.isEmpty()) {
                refererDomains.add(domain);
            }
        }

        // Подсчет ошибочных запросов (4xx или 5xx)
        int statusCode = entry.getStatusCode();
        if (statusCode >= 400 && statusCode < 600) {
            errorRequestsCount++;
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

    private boolean isBot(UserAgent userAgent) {
        if (userAgent == null) {
            return false;
        }

        // Проверяем по названию браузера
        UserAgent.Browser browser = userAgent.getBrowser();
        if (browser == UserAgent.Browser.GOOGLEBOT ||
                browser == UserAgent.Browser.YANDEXBOT) {
            return true;
        }
        String originalUA = userAgent.getOriginalUserAgent().toLowerCase();
        return originalUA.contains("bot") ||
                originalUA.contains("crawler") ||
                originalUA.contains("spider");
    }

    // Метод для извлечения домена из URL
    private String extractDomainFromUrl(String url) {
        if (url == null || url.isEmpty() || url.equals("-")) {
            return null;
        }

        try {
            // Убираем протокол (http://, https://)
            String domain = url.replaceFirst("^(https?://)?(www\\.)?", "");

            // Убираем путь и параметры после домена
            int slashIndex = domain.indexOf('/');
            if (slashIndex != -1) {
                domain = domain.substring(0, slashIndex);
            }

            // Убираем порт, если есть
            int colonIndex = domain.indexOf(':');
            if (colonIndex != -1) {
                domain = domain.substring(0, colonIndex);
            }

            return domain.toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }

    // 1. Метод расчёта пиковой посещаемости сайта (в секунду)
    public int getPeakVisitsPerSecond() {
        if (visitsPerSecond.isEmpty()) {
            return 0;
        }
        // Находим максимальное значение среди всех секунд
        return visitsPerSecond.values().stream()
                .max(Integer::compareTo)
                .orElse(0);
    }

    // Вспомогательный метод для получения времени пиковой посещаемости
    public Map.Entry<Long, Integer> getPeakVisitsSecondInfo() {
        if (visitsPerSecond.isEmpty()) {
            return null;
        }
        return visitsPerSecond.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
    }

    // 2. Метод, возвращающий список сайтов, со страниц которых есть ссылки на текущий сайт
    public Set<String> getRefererDomains() {
        return new HashSet<>(refererDomains);
    }

    // 3. Метод расчёта максимальной посещаемости одним пользователем
    public int getMaxVisitsPerUser() {
        if (visitsPerUser.isEmpty()) {
            return 0;
        }

        // Находим максимальное количество посещений среди всех пользователей
        return visitsPerUser.values().stream()
                .max(Integer::compareTo)
                .orElse(0);
    }

    // Вспомогательный метод для получения информации о пользователе с максимальной посещаемостью
    public Map.Entry<String, Integer> getTopUserInfo() {
        if (visitsPerUser.isEmpty()) {
            return null;
        }

        return visitsPerUser.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);
    }

    // Метод подсчёта среднего количества посещений сайта за час обычными пользователями
    public double getAverageVisitsPerHour() {
        if (minTime == null || maxTime == null || humanVisitsCount == 0) {
            return 0.0;
        }

        Duration duration = Duration.between(minTime, maxTime);
        double hours = duration.toHours();

        if (hours < 1.0) {
            hours = 1.0;
        }

        return (double) humanVisitsCount / hours;
    }

    // Метод подсчёта среднего количества ошибочных запросов в час
    public double getAverageErrorRequestsPerHour() {
        if (minTime == null || maxTime == null || errorRequestsCount == 0) {
            return 0.0;
        }

        Duration duration = Duration.between(minTime, maxTime);
        double hours = duration.toHours();

        if (hours < 1.0) {
            hours = 1.0;
        }

        return (double) errorRequestsCount / hours;
    }

    // Метод расчёта средней посещаемости одним пользователем
    public double getAverageVisitsPerUser() {
        if (humanVisitsCount == 0 || uniqueHumanIPs.isEmpty()) {
            return 0.0;
        }

        return (double) humanVisitsCount / uniqueHumanIPs.size();
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
    // Геттеры для новых полей
    public int getHumanVisitsCount() {
        return humanVisitsCount;
    }

    public int getErrorRequestsCount() {
        return errorRequestsCount;
    }

    public int getUniqueHumanUsersCount() {
        return uniqueHumanIPs.size();
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
        double avgVisitsPerHour = getAverageVisitsPerHour();
        report.append(String.format("   Среднее количество посещений в час: %.2f\n", avgVisitsPerHour));
        report.append(String.format("   Посещений обычными пользователями: %,d\n", humanVisitsCount));
        report.append(String.format("   Уникальных пользователей (IP-адресов): %,d\n", uniqueHumanIPs.size()));

        double avgErrorsPerHour = getAverageErrorRequestsPerHour();
        report.append(String.format("   Среднее количество ошибочных запросов в час: %.2f\n", avgErrorsPerHour));

        double avgVisitsPerUser = getAverageVisitsPerUser();
        report.append(String.format("   Средняя посещаемость одним пользователем: %.2f\n", avgVisitsPerUser));

        int peakVisits = getPeakVisitsPerSecond();
        Map.Entry<Long, Integer> peakSecondInfo = getPeakVisitsSecondInfo();

        if (peakSecondInfo != null) {
            LocalDateTime peakTime = LocalDateTime.ofEpochSecond(peakSecondInfo.getKey(), 0, ZoneOffset.UTC);
            report.append(String.format("   Максимальная посещаемость в секунду: %d\n", peakVisits));
            report.append(String.format("   Время пика: %s\n", peakTime));
            report.append(String.format("   Секунд с данными: %,d\n", visitsPerSecond.size()));
        } else {
            report.append("   Нет данных о пиковой посещаемости\n");
        }

        report.append("\n3. СТАТИСТИКА ПО ПОЛЬЗОВАТЕЛЯМ:\n");
        report.append(String.format("   Уникальных пользователей: %,d\n", uniqueHumanIPs.size()));
        report.append(String.format("   Средняя посещаемость на пользователя: %.2f\n", getAverageVisitsPerUser()));
        int maxVisits = getMaxVisitsPerUser();
        Map.Entry<String, Integer> topUser = getTopUserInfo();
        if (topUser != null) {
            report.append(String.format("   Максимальная посещаемость одним пользователем: %d\n", maxVisits));
            report.append(String.format("   Пользователь с максимальной посещаемостью: %s\n", topUser.getKey()));
        }

        report.append(String.format("   Количество сайтов с ссылками: %,d\n", refererDomains.size()));

        if (!refererDomains.isEmpty()) {
            report.append("   Список сайтов-референтов:\n");
            refererDomains.stream()
                    .sorted()
                    .forEach(domain -> report.append(String.format("   - %s\n", domain)));
        } else {
            report.append("   Нет данных о сайтах-референтах\n");
        }

        return report.toString();
    }
}
