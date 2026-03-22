package ru.herooo.projects.mp3metadatasetter;

import com.mpatric.mp3agic.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import java.util.Scanner;

public class Main {
    private static Scanner SCANNER = new Scanner(System.in, "cp866");

    public static void main(String[] args) throws IOException, InterruptedException {
        while (true) {
            clearScreen();
            printHeader();

            // Получаем путь к папке, где будем устанавливать метаданные
            System.out.print("Введите путь к папке альбома: ");
            String text = SCANNER.nextLine();
            System.out.println(text);
            File album = new File(text);
            printLine();

            if (!album.exists() || !album.isDirectory()) {
                System.out.println("Папка альбома не найдена");
                System.out.println(album.getPath());
                printLine();
                waitAnyButton();
                continue;
            }

            clearScreen();
            printHeader();

            System.out.println("Папка альбома \"" + album.getName() + "\" (только корректные .mp3)");
            printLine();

            // Отображаем только корректные .mp3 файлы
            File[] files = album.listFiles();
            int numberOfCorrectMp3Files = 0;
            for (int i = 0; i < files.length; i++) {
                try {
                    Mp3File mp3 = new Mp3File(files[i].getPath());
                    System.out.println(files[i].getName());
                    numberOfCorrectMp3Files++;
                } catch (Exception e) {

                }
            }

            // Если не нашли, начинаем заново
            if (numberOfCorrectMp3Files == 0) {
                System.out.println("Треки альбома не найдены");
                printLine();
                waitAnyButton();
                continue;
            }

            // Вводим метаданные
            printLine();
            System.out.println("1. Вы можете дать согласие на перенос порядкового номера из названия трека в метаданные." +
                    " Название треков должно быть записано в следующем формате: \"1. Название трека.mp3\"." +
                    " При согласии о переносе порядкового номера он будет удален из названия файла.");
            System.out.println("2. Если в папке с альбомом лежит файл \"image.jpg\", то это изображение будет установлено на каждый трек.");
            printLine();
            System.out.print("Введите название альбома: ");
            String name = SCANNER.nextLine();
            System.out.print("Введите исполнителя альбома: ");
            String artist = SCANNER.nextLine();
            System.out.print("Введите год альбома: ");
            String year = SCANNER.nextLine();
            System.out.print("Перенести порядковый номер трека из названия в метаданные? (y/n): ");
            boolean doNeedToReplaceNumber = SCANNER.nextLine().equalsIgnoreCase("y");
            printLine();

            // Вносим введённые метаданные в .mp3 файлы
            for (int i = 0; i < files.length; i++) {
                Mp3File mp3 = null;

                try {
                    mp3 = new Mp3File(files[i].getPath());
                } catch (Exception e) {
                    continue;
                }

                try {
                    // Добавляем область метаданных, если она отсутствует
                    if (!mp3.hasId3v2Tag()) {
                        mp3.setId3v2Tag(new ID3v24Tag());
                    }

                    ID3v2 id3v2 = mp3.getId3v2Tag();

                    // Пробуем получить номер трека из названия, вносим в метаданные (если разрешено)
                    String filename = files[i].getName();
                    if (doNeedToReplaceNumber) {
                        String numberStr = files[i].getName();
                        numberStr = numberStr.substring(0, numberStr.indexOf("."));

                        try {
                            int number = Integer.parseInt(numberStr);
                            filename = filename.substring(filename.indexOf(".") + 1).trim();
                            id3v2.setTrack(String.valueOf(number));
                        } catch (Exception ignored) {

                        }
                    }

                    // Пробуем получить обложку внутри папки альбома, вносим в метаданные
                    File albumImage = new File(album.getPath() + "\\" + "image.jpg");
                    if (albumImage.exists()) {
                        byte[] albumImageBytes = Files.readAllBytes(albumImage.toPath());
                        id3v2.setAlbumImage(albumImageBytes, "image/jpg");
                    }

                    // Вносим остальные метаданные
                    id3v2.setAlbum(name);
                    id3v2.setArtist(artist);
                    id3v2.setYear(year);

                    // Запоминаем старый путь к файлу, формируем новый путь к файлу
                    String oldPath = files[i].getParent() + "\\" + filename;
                    String newPath = oldPath + "_";

                    // Сохраняем файл под новым путём и, после удаления старого файла, переименовываем
                    mp3.save(newPath);
                    File newMp3 = new File(newPath);
                    if (files[i].delete()) {
                        newMp3.renameTo(new File(oldPath));
                        newMp3 = new File(oldPath);
                    } else {
                        newMp3.delete();
                        throw new Exception();
                    }

                    System.out.println(newMp3.getName() + " - успешно");
                } catch (Exception e) {
                    System.out.println(files[i].getName() + " - ошибка");
                }
            }

            printLine();
            waitAnyButton();
        }
    }

    private static void clearScreen() throws IOException {
        try {
            String operatingSystem = System.getProperty("os.name"); // Check the current operating system

            if (operatingSystem.contains("Windows")) {
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "cls");
                Process startProcess = pb.inheritIO().start();
                startProcess.waitFor();
            }
            else {
                ProcessBuilder pb = new ProcessBuilder("clear");
                Process startProcess = pb.inheritIO().start();

                startProcess.waitFor();
            }
        } catch (Exception e) {

        }
    }

    private static void printLine() {
        System.out.println("--------------------");
    }

    private static void printHeader() throws IOException {
        Properties props = new Properties();
        props.load(Main.class.getResourceAsStream("/version.properties"));
        String version = props.getProperty("version");

        printLine();
        System.out.printf("Установка метаданных в альбомных треках (v. %s)\n", version);
        printLine();
    }

    private static void waitAnyButton() {
        System.out.print("Нажмите любую клавишу...");
        SCANNER.nextLine();
    }
}