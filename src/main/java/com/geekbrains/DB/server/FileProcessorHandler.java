package com.geekbrains.DB.server;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;

public class FileProcessorHandler implements Runnable {

    private File currentDir;
    private static final int SIZE = 256;
    private DataInputStream is;
    private DataOutputStream os;
    private byte[] buf;
    private String[] listFiles;

    public FileProcessorHandler(Socket socket) throws IOException {
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        buf = new byte[SIZE];
        currentDir = new File("serverDir");
        listFiles = currentDir.list();
    }

    @Override
    public void run() {
        try {
            sendListFiles();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            while (true) {
                String command = is.readUTF();
                System.out.println("Received: " + command);
                if (command.equals("#SEND#FILE#")) {
                    String fileName = is.readUTF();
                    long size = is.readLong();
                    System.out.println("Created file: " + fileName);
                    System.out.println("File size: " + size);
                    Path currentPath = currentDir.toPath().resolve(fileName);
                    try (FileOutputStream fos = new FileOutputStream(currentPath.toFile())) {
                        for (int i = 0; i < (size + SIZE - 1) / SIZE; i++) {
                            int read = is.read(buf);
                            fos.write(buf, 0, read);
                        }
                    }
                    os.writeUTF(fileName);
                    os.writeUTF("File successfully uploaded");
                    os.flush();
                }
                if (command.equals("#DOWNLOAD#FILE")) {
                    String fileName = is.readUTF();
                    Path currentPath = currentDir.toPath().resolve(fileName);
                    long size = currentPath.toFile().length();
                    System.out.println("Downloaded file: " + fileName);
                    System.out.println("File size: " + size);
                    try (FileInputStream is = new FileInputStream(currentPath.toFile())) {
                        while (true) {
                            int read = is.read(buf);
                            if (read == -1) {
                                break;
                            }
                            os.writeUTF("#SEND#FILE#");
                            os.writeUTF(fileName);
                            os.writeLong(size);
                            os.write(buf, 0, read);
                        }
                    }
                    os.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void sendListFiles() throws IOException {
        if (listFiles.length != 0) {
            for (String file : listFiles) {
                os.writeUTF(file);
            }
        } else {
            os.writeUTF("Server directory is empty");
        }
    }
}