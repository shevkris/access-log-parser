package org.example;
import org.example.UserAgent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogEntry {
//    private final String ipAddress;
//    private final String firstDash;
//    private final String secondDash;
//    private final LocalDateTime timestamp;
//    private final HttpMethod method;
//    private final String path;
//    private final String protocol;
//    private final int statusCode;
//    private final int responseSize;
//    private final String referer;
//    private final UserAgent userAgent;
//
//    private static final Pattern LOG_PATTERN = Pattern.compile(
//            "^([\\d.]+) (\\S+) (\\S+) \\[([^\\]]+)\\] \"(\\S+) (\\S+) (\\S+)\" (\\d{3}) (\\d+) \"([^\"]*)\" \"([^\"]*)\"$"
//    );
//
//    public enum HttpMethod {
//        GET, POST, PUT, DELETE, HEAD, OPTIONS, PATCH, CONNECT, TRACE
//    }
//
//    public LogEntry(String logLine) {
//        Matcher matcher = LOG_PATTERN.matcher(logLine);
//
//        if (!matcher.find()) {
//            throw new IllegalArgumentException("Некорректный формат строки лога: " + logLine);
//        }
//
//        this.ipAddress = matcher.group(1);
//        this.firstDash = matcher.group(2);
//        this.secondDash = matcher.group(3);
//
//        String dateTimeStr = matcher.group(4);
//        this.timestamp = parseDateTime(dateTimeStr);
//
//        String methodStr = matcher.group(5);
//        this.method = parseHttpMethod(methodStr);
//
//        this.path = matcher.group(6);
//
//        this.protocol = matcher.group(7);
//
//        this.statusCode = Integer.parseInt(matcher.group(8));
//
//        this.responseSize = Integer.parseInt(matcher.group(9));
//
//        this.referer = matcher.group(10);
//
//        String userAgentString = matcher.group(11);
//        this.userAgent = new UserAgent(userAgentString);
//    }
//
//    private LocalDateTime parseDateTime(String dateTimeStr) {
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
//        return LocalDateTime.parse(dateTimeStr, formatter);
//    }
//
//    private HttpMethod parseHttpMethod(String methodStr) {
//        try {
//            return HttpMethod.valueOf(methodStr);
//        } catch (IllegalArgumentException e) {
//            return HttpMethod.GET;
//        }
//    }
//
//    public String getIpAddress() {
//        return ipAddress;
//    }
//
//    public String getFirstDash() {
//        return firstDash;
//    }
//
//    public String getSecondDash() {
//        return secondDash;
//    }
//
//    public LocalDateTime getTimestamp() {
//        return timestamp;
//    }
//
//    public HttpMethod getMethod() {
//        return method;
//    }
//
//    public String getPath() {
//        return path;
//    }
//
//    public String getProtocol() {
//        return protocol;
//    }
//
//    public int getStatusCode() {
//        return statusCode;
//    }
//
//    public int getResponseSize() {
//        return responseSize;
//    }
//
//    public String getReferer() {
//        return referer;
//    }
//
//    public UserAgent getUserAgent() {
//        return userAgent;
//    }
//
//    @Override
//    public String toString() {
//        return String.format("LogEntry{ip='%s', time=%s, method=%s, path='%s', status=%d, size=%d, os=%s, browser=%s}",
//                ipAddress, timestamp, method, path, statusCode, responseSize,
//                userAgent.getOperatingSystem(), userAgent.getBrowser());
//    }
private final String ipAddress;
    private final String firstDash;
    private final String secondDash;
    private final LocalDateTime timestamp;
    private final HttpMethod method;
    private final String path;
    private final int statusCode;
    private final int responseSize;
    private final String referer;
    private final String userAgent;

    // Enum для методов HTTP-запросов
    public enum HttpMethod {
        GET, POST, PUT, DELETE, HEAD, OPTIONS, PATCH, CONNECT, TRACE
    }

    // Конструктор, принимающий строку лога
    public LogEntry(String logLine) {

        String[] parts = logLine.split(" ", 4); // Разделяем на первые 4 части

        this.ipAddress = parts[0];
        this.firstDash = parts[1];
        this.secondDash = parts[2];
        String remaining = parts[3];

        int bracketStart = remaining.indexOf('[');
        int bracketEnd = remaining.indexOf(']');
        String dateTimeStr = remaining.substring(bracketStart + 1, bracketEnd);

        this.timestamp = parseDateTime(dateTimeStr);
        remaining = remaining.substring(bracketEnd + 2);
        int quoteStart = remaining.indexOf('"');
        int quoteEnd = remaining.indexOf('"', quoteStart + 1);
        String requestLine = remaining.substring(quoteStart + 1, quoteEnd);
        String[] requestParts = requestLine.split(" ");
        this.method = parseHttpMethod(requestParts[0]);
        this.path = requestParts[1];
        remaining = remaining.substring(quoteEnd + 2);
        String[] responseParts = remaining.split(" ", 3);
        this.statusCode = Integer.parseInt(responseParts[0]);
        this.responseSize = Integer.parseInt(responseParts[1]);
        remaining = remaining.substring(responseParts[0].length() + responseParts[1].length() + 2);


        if (remaining.startsWith("\"") && remaining.length() > 1) {
            int refQuoteEnd = remaining.indexOf('"', 1);
            this.referer = remaining.substring(1, refQuoteEnd);
            remaining = remaining.substring(refQuoteEnd + 2); // +2 для '" '
        } else {
            this.referer = "-";
            if (remaining.startsWith("- ")) {
                remaining = remaining.substring(2);
            } else {
                remaining = remaining.substring(1); // Просто "-"
            }
        }

        if (remaining.startsWith("\"") && remaining.length() > 1) {
            this.userAgent = remaining.substring(1, remaining.length() - 1);
        } else {
            this.userAgent = remaining;
        }
    }
    private LocalDateTime parseDateTime(String dateTimeStr) {
        // Формат: 25/Sep/2022:06:25:10 +0300
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
        return LocalDateTime.parse(dateTimeStr.replace("Sep", "Sep"), formatter);
    }
    private HttpMethod parseHttpMethod(String methodStr) {
        try {
            return HttpMethod.valueOf(methodStr);
        } catch (IllegalArgumentException e) {// Если метод не найден в enum, возвращаем GET как значение по умолчанию
            return HttpMethod.GET;
        }
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getFirstDash() {
        return firstDash;
    }

    public String getSecondDash() {
        return secondDash;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public int getResponseSize() {
        return responseSize;
    }

    public String getReferer() {
        return referer;
    }

    public String getUserAgent() {
        return userAgent;
    }

    @Override
    public String toString() {
        return String.format("LogEntry{ip='%s', time=%s, method=%s, path='%s', status=%d, size=%d, ua='%s'}",
                ipAddress, timestamp, method, path, statusCode, responseSize, userAgent);
    }
}