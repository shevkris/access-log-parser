package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.Scanner;


public class Main {
    private static final int MAX_LINE_LENGTH = 1024;

    public static void main(String[] args) {
        int N = 0;
        while (true) {
            System.out.println("Введите путь к файлу: ");
            String path = new Scanner(System.in).nextLine();

            File file = new File(path);
            boolean fileExists = file.exists();
            boolean isDirectory = file.isDirectory();

            if (!fileExists) {
                System.out.println("Указанный файл не существует");
                continue;
            }
            if (isDirectory) {
                System.out.println("Указанный путь ведет к папке, а не к файлу");
                continue;
            }
            System.out.println("Путь указан верно");
            N++;
            System.out.println("Путь №" + N);
            try {
                analyzeLogFile(path);
            } catch (LineTooLongException e) {
                System.err.println("ОШИБКА: " + e.getMessage());
            }
        }
    }

    private static void analyzeLogFile(String filePath) {
       try  {
            FileReader fileReader = new FileReader(filePath);
            BufferedReader reader = new BufferedReader(fileReader);
            String line;
            int lineCounter = 0;
            int googlebotCount = 0;
            int yandexbotCount = 0;

            while ((line = reader.readLine()) != null) {
                lineCounter++;
                int currentLength = line.length();
                if (currentLength > MAX_LINE_LENGTH) {
                    throw new LineTooLongException(
                            String.format("Строка " + lineCounter + " слишком длинная: " + currentLength + " символов (максимум " + MAX_LINE_LENGTH + ")")
                    );
                }
                String userAgent = extractUserAgentFromLogLine(line);
                if (userAgent != null && !userAgent.isEmpty() && !userAgent.equals("-")) {
                    String botName = extractBotName(userAgent);
                    if ("Googlebot".equals(botName)) {
                        googlebotCount++;
                    } else if ("YandexBot".equals(botName)) {
                        yandexbotCount++;
                    }
                }
            }

            DecimalFormat df = new DecimalFormat("0.00%");
            System.out.println("Всего строк в файле: " + lineCounter);
            System.out.println("Запросов от Googlebot: " + googlebotCount);
            System.out.println("Запросов от YandexBot: " + yandexbotCount);

            if (lineCounter > 0) {
                System.out.println("\nДоли запросов:");
                System.out.println("Googlebot: " + df.format((double) googlebotCount / lineCounter));
                System.out.println("YandexBot: " + df.format((double) yandexbotCount / lineCounter));
                System.out.println("Всего ботов: " + df.format((double) (googlebotCount + yandexbotCount) / lineCounter));
            }

        } catch (LineTooLongException e) {
            throw e;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    private static String extractUserAgentFromLogLine(String logLine) {
        int lastQuote = logLine.lastIndexOf('"');
        if (lastQuote == -1) return null;
        int secondLastQuote = logLine.lastIndexOf('"', lastQuote - 1);
        if (secondLastQuote == -1) return null;
        return logLine.substring(secondLastQuote + 1, lastQuote).trim();
    }

    private static String extractBotName(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return null;
        }
        String[] bracketContents = extractAllBracketContents(userAgent);

        for (String bracketContent : bracketContents) {
            if (bracketContent.contains("compatible")) {String botName = extractBotNameFromBracketContent(bracketContent);
                if (botName != null) {
                    return botName;
                }
            }
        }
        return null;
    }

    private static String[] extractAllBracketContents(String userAgent) {
        java.util.List<String> contents = new java.util.ArrayList<>();
        int start = 0;

        while (true) {
            int openBracket = userAgent.indexOf('(', start);
            if (openBracket == -1) break;
            int closeBracket = userAgent.indexOf(')', openBracket);
            if (closeBracket == -1) break;
            String content = userAgent.substring(openBracket + 1, closeBracket);
            contents.add(content);
            start = closeBracket + 1;
        }

        return contents.toArray(new String[0]);
    }

    private static String extractBotNameFromBracketContent(String bracketContent) {
        try {
            String[] parts = bracketContent.split(";");

            if (parts.length >= 2) {
                for (int i = 0; i < parts.length; i++) {
                    parts[i] = parts[i].trim();
                }
                for (String part : parts) {
                    if (part.contains("Googlebot") || part.contains("YandexBot")) {
                        int slashIndex = part.indexOf('/');
                        if (slashIndex != -1) {
                            String botName = part.substring(0, slashIndex).trim();
                            if ("Googlebot".equals(botName) || "YandexBot".equals(botName)) {
                                return botName;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {}

        return null;
    }
}
