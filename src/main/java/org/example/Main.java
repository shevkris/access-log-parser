package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
                readFileWithLengthCheck(path);
            } catch (LineTooLongException e) {
                System.err.println("ОШИБКА: " + e.getMessage());
            }
        }
    }

    private static void readFileWithLengthCheck(String filePath) {
        try {
            FileReader fileReader = new FileReader(filePath);
            BufferedReader reader = new BufferedReader(fileReader);
            String line;
            int lineCounter = 0;
            while ((line = reader.readLine()) != null) {
                lineCounter++;
                int currentLength = line.length();
                if (currentLength > MAX_LINE_LENGTH) {
                    throw new LineTooLongException(
                            String.format("Строка " + lineCounter + " слишком длинная: " + currentLength + " символов (максимум " + MAX_LINE_LENGTH + ")")
                    );
                }
            }
            System.out.println("Всего строк: " + lineCounter);
        } catch (LineTooLongException e) {
            throw e;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

