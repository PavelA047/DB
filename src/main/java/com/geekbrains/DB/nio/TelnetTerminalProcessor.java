package com.geekbrains.DB.nio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Collectors;

@Slf4j
public class TelnetTerminalProcessor implements ClientProcessor {

    private static Path currentDir;

    public TelnetTerminalProcessor() {
        currentDir = Paths.get(System.getProperty("user.home"));
    }

    @Override
    public void onMessageReceived(String msg, SocketChannel channel) throws IOException {
        log.info("received: {}", msg);
        msg = msg.trim();
        if (msg.equals("ls")) {
            String listFilesResponse = Files.list(currentDir)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.joining("\n")) + "\n";
            channel.write(ByteBuffer.wrap(
                    listFilesResponse.getBytes(StandardCharsets.UTF_8)
                    )
            );
//            String[] listFiles = currentDir.toFile().list();
//            StringBuilder sOut = new StringBuilder();
//            assert listFiles != null;
//            if (listFiles.length != 0) {
//                for (String file : listFiles) {
//                    sOut.append("[").append(file).append("]\n\r");
//                }
//                channel.write(ByteBuffer.wrap(sOut.toString().getBytes(StandardCharsets.UTF_8)));
//            } else {
//                channel.write(ByteBuffer.wrap("dir is empty\n\r".getBytes(StandardCharsets.UTF_8)));
//            }
        } else if (msg.startsWith("cd")) {
            String dst = msg.split(" +")[1];
            if (Files.isDirectory(currentDir.resolve(dst))) {
                currentDir = currentDir.resolve(dst);
            }
//            String[] token = msg.split("\\s", 2);
//            String fileToFind = File.separator + token[1].trim();
//            try {
//                Files.walkFileTree(Paths.get("serverDir"), new SimpleFileVisitor<Path>() {
//                    @Override
//                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
//                        String fileString = dir.toAbsolutePath().toString();
//                        if (fileString.endsWith(fileToFind)) {
//                            currentDir = Paths.get(fileString);
//                            return FileVisitResult.TERMINATE;
//                        }
//                        return FileVisitResult.CONTINUE;
//                    }
//                });
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        } else if (msg.trim().equals("..")) {
            currentDir = Paths.get(String.valueOf(currentDir.getParent()));
        } else if (msg.startsWith("cat")) {
            String[] token = msg.split("\\s", 2);
            String fileName = token[1].trim();
            System.out.println(new String(Files.readAllBytes(currentDir.resolve(fileName))));
        } else if (msg.startsWith("mkdir")) {
            String[] token = msg.split("\\s", 2);
            Files.createDirectory(currentDir.resolve(token[1].trim()));
        } else if (msg.startsWith("touch")) {
            String[] token = msg.split("\\s", 2);
            Files.createFile(currentDir.resolve(token[1].trim()));
        } else {
            channel.write(ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));
        }
        String prefix = currentDir.getFileName().toString() + "> ";
        channel.write(ByteBuffer.wrap(prefix.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public void onClientAccepted(SocketChannel channel) throws IOException {
        channel.write(ByteBuffer.wrap(
                "Hello user. Welcome to our terminal\n\r".getBytes(StandardCharsets.UTF_8)
        ));
        log.info("Client accepted...");
        String prefix = currentDir.getFileName().toString() + "> ";
        channel.write(ByteBuffer.wrap(prefix.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public void onClientDisconnected(SocketChannel channel) throws IOException {
        log.info("Client disconnected...");
    }

    @Override
    public void onExceptionCaught(Throwable ex) throws IOException {
        log.error("error: ", ex);
    }
}
