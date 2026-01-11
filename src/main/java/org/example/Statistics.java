package org.example;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Statistics {
    private int totalTraffic;
    private LocalDateTime minTime;
    private LocalDateTime maxTime;
    private int entryCount;

    // Конструктор без параметров
    public Statistics() {
        this.totalTraffic = 0;
        this.minTime = null;
        this.maxTime = null;
        this.entryCount = 0;
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
        report.append(String.format("Общий объем трафика: %,d байт (%.2f МБ)\n",
                totalTraffic, totalTraffic / (1024.0 * 1024.0)));

        if (minTime != null && maxTime != null) {
            report.append(String.format("Период сбора данных: с %s по %s\n",
                    minTime, maxTime));

            Duration duration = Duration.between(minTime, maxTime);
            long days = duration.toDays();
            long hours = duration.toHours() % 24;
            long minutes = duration.toMinutes() % 60;

            report.append(String.format("Продолжительность периода: %d дн., %d ч., %d мин.\n",
                    days, hours, minutes));

            report.append(String.format("Средний трафик: %.2f байт/час\n", getTrafficRate()));
        } else {
            report.append("Нет данных для анализа\n");
        }

        return report.toString();
    }
}
