package org.example;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogEntry {
    private String ipAddress;
    private String firstDash;
    private String secondDash;
    private LocalDateTime timestamp;
    private HttpMethod method;
    private String path;
    private String protocol;
    private int statusCode;
    private int responseSize;
    private String referer;
    private UserAgent userAgent;
    private boolean isValid;
    private String errorMessage;

    // Регулярное выражение для парсинга строки лога
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^([\\d.]+) (\\S+) (\\S+) \\[([^\\]]+)\\] \"(\\S+) (\\S+) (\\S+)\" (\\d{3}|-) (\\d+|-) \"([^\"]*)\" \"([^\"]*)\"$"
    );

    public LogEntry(String logLine) {
        this.ipAddress = "";
        this.firstDash = "";
        this.secondDash = "";
        this.timestamp = null;
        this.method = HttpMethod.UNKNOWN;
        this.path = "";
        this.protocol = "";
        this.statusCode = 0;
        this.responseSize = 0;
        this.referer = "";
        this.userAgent = new UserAgent("");
        this.isValid = false;
        this.errorMessage = "";

        parseLogLine(logLine);
    }

    private void parseLogLine(String logLine) {
        if (logLine == null || logLine.trim().isEmpty()) {
            this.errorMessage = "Пустая строка";
            return;
        }

        try {
            Matcher matcher = LOG_PATTERN.matcher(logLine);

            if (matcher.find()) {
                parseWithRegex(matcher, logLine);
            } else {
                parseManually(logLine);
            }

        } catch (Exception e) {
            this.errorMessage = "Ошибка парсинга: " + e.getMessage();
        }
    }

    private void parseWithRegex(Matcher matcher, String logLine) {
        try {
            this.ipAddress = matcher.group(1);
            this.firstDash = matcher.group(2);
            this.secondDash = matcher.group(3);

            String dateTimeStr = matcher.group(4);
            this.timestamp = parseDateTime(dateTimeStr);

            String methodStr = matcher.group(5);
            this.method = parseHttpMethod(methodStr);

            this.path = matcher.group(6);

            this.protocol = matcher.group(7);

            String statusStr = matcher.group(8);
            this.statusCode = parseStatusCode(statusStr);

            String sizeStr = matcher.group(9);
            this.responseSize = parseResponseSize(sizeStr);

            this.referer = matcher.group(10);

            String userAgentString = matcher.group(11);
            this.userAgent = new UserAgent(userAgentString);

            this.isValid = true;
            this.errorMessage = "";

        } catch (Exception e) {
            this.errorMessage = "Ошибка парсинга с regex: " + e.getMessage();
        }
    }

    private void parseManually(String logLine) {
        try {
            String[] parts = logLine.split(" ", 2);
            if (parts.length > 0) {
                this.ipAddress = parts[0];
            }

            int bracketStart = logLine.indexOf('[');
            int bracketEnd = logLine.indexOf(']', bracketStart);

            if (bracketStart != -1 && bracketEnd != -1) {
                String dateTimeStr = logLine.substring(bracketStart + 1, bracketEnd);
                this.timestamp = parseDateTime(dateTimeStr);

                int quoteStart = logLine.indexOf('"', bracketEnd);
                if (quoteStart != -1) {
                    int quoteEnd = logLine.indexOf('"', quoteStart + 1);
                    if (quoteEnd != -1) {
                        String requestLine = logLine.substring(quoteStart + 1, quoteEnd);
                        parseRequestLine(requestLine);

                        // 4. Остаток после запроса
                        String remaining = logLine.substring(quoteEnd + 1).trim();
                        parseRemainingParts(remaining);
                    }
                }
            }

            this.isValid = (this.timestamp != null && this.method != HttpMethod.UNKNOWN);
            this.errorMessage = this.isValid ? "" : "Не удалось извлечь необходимые данные";

        } catch (Exception e) {
            this.errorMessage = "Ошибка ручного парсинга: " + e.getMessage();
        }
    }

    private void parseRequestLine(String requestLine) {
        try {
            String[] requestParts = requestLine.split(" ");
            if (requestParts.length >= 3) {
                this.method = parseHttpMethod(requestParts[0]);
                this.path = requestParts[1];
                this.protocol = requestParts[2];
            } else if (requestParts.length == 1) {
                this.method = parseHttpMethod(requestParts[0]);
            }
        } catch (Exception e) {
        }
    }

    private void parseRemainingParts(String remaining) {
        try {
            String[] parts = remaining.split("\\s+");

            if (parts.length >= 2) {
                this.statusCode = parseStatusCode(parts[0]);

                if (parts.length > 1) {
                    this.responseSize = parseResponseSize(parts[1]);
                }

                if (parts.length > 3) {
                    StringBuilder refererBuilder = new StringBuilder();
                    int startIndex = remaining.indexOf('"');
                    if (startIndex != -1) {
                        int endIndex = remaining.indexOf('"', startIndex + 1);
                        if (endIndex != -1) {
                            this.referer = remaining.substring(startIndex + 1, endIndex);

                            String afterReferer = remaining.substring(endIndex + 1).trim();
                            if (afterReferer.startsWith("\"")) {
                                int uaEnd = afterReferer.indexOf('"', 1);
                                if (uaEnd != -1) {
                                    String uaString = afterReferer.substring(1, uaEnd);
                                    this.userAgent = new UserAgent(uaString);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (DateTimeParseException e) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss", Locale.ENGLISH);
                return LocalDateTime.parse(dateTimeStr.split(" ")[0], formatter);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }

    private HttpMethod parseHttpMethod(String methodStr) {
        try {
            return HttpMethod.valueOf(methodStr);
        } catch (IllegalArgumentException e) {
            return HttpMethod.UNKNOWN;
        }
    }

    private int parseStatusCode(String statusStr) {
        try {
            if (statusStr.equals("-") || statusStr.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(statusStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private int parseResponseSize(String sizeStr) {
        try {
            if (sizeStr.equals("-") || sizeStr.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(sizeStr);
        } catch (NumberFormatException e) {
            return 0;
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

    public String getProtocol() {
        return protocol;
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

    public UserAgent getUserAgent() {
        return userAgent;
    }

    public boolean isValid() {
        return isValid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        if (!isValid) {
            return String.format("LogEntry{INVALID: %s}", errorMessage);
        }

        return String.format("LogEntry{ip='%s', time=%s, method=%s, path='%s', status=%d, size=%d}",
                ipAddress, timestamp, method, path, statusCode, responseSize);
    }
}