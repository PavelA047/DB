package com.geekbrains.DB.nio;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.Set;

public class NioEchoServer {

    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ByteBuffer buf;
    private Path currentDirPath;

    public NioEchoServer() throws IOException {
        currentDirPath = Paths.get("serverDir");
        buf = ByteBuffer.allocate(256);
        serverChannel = ServerSocketChannel.open();
        selector = Selector.open();
        serverChannel.configureBlocking(false);
        serverChannel.bind(new InetSocketAddress(8189));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started...");
        while (serverChannel.isOpen()) {
            selector.select(); // block
            System.out.println("Keys selected...");
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept();
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
                iterator.remove();
            }
        }
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        StringBuilder s = new StringBuilder();
        int read = 0;
        while (true) {
            read = channel.read(buf);
            if (read == 0) {
                break;
            }
            if (read < 0) {
                channel.close();
                return;
            }
            buf.flip();
            while (buf.hasRemaining()) {
                s.append((char) buf.get());
            }
            buf.clear();
        }
        System.out.println("Received: " + s);
        if (s.toString().trim().equals("ls")) {
            ls(channel);
        } else if (s.toString().startsWith("cd")) {
            String[] token = s.toString().split("\\s", 2);
            String fileToFind = File.separator + token[1].trim();
            try {
                Files.walkFileTree(Paths.get("serverDir"), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        String fileString = dir.toAbsolutePath().toString();
                        if (fileString.endsWith(fileToFind)) {
                            currentDirPath = Paths.get(fileString);
                            return FileVisitResult.TERMINATE;
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (s.toString().startsWith("..")) {
//          TODO go to upper dir
        } else if (s.toString().startsWith("cat")) {
            String[] token = s.toString().split("\\s", 2);
            String fileName = token[1].trim();
            System.out.println(new String(Files.readAllBytes(currentDirPath.resolve(fileName))));
        } else if (s.toString().startsWith("mkdir")) {
            String[] token = s.toString().split("\\s", 2);
            Files.createDirectory(currentDirPath.resolve(token[1].trim()));
        } else if (s.toString().startsWith("touch")) {
            String[] token = s.toString().split("\\s", 2);
            Files.createFile(currentDirPath.resolve(token[1].trim()));
        } else {
            channel.write(ByteBuffer.wrap(s.toString().getBytes(StandardCharsets.UTF_8)));
        }
    }

    private void handleAccept() throws IOException {
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        channel.write(ByteBuffer.wrap(
                "Hello user. Welcome to our terminal\n\r".getBytes(StandardCharsets.UTF_8)
        ));
        System.out.println("Client accepted...");
    }

    private void ls(SocketChannel channel) throws IOException {
        String[] listFiles = currentDirPath.toFile().list();
        StringBuilder sOut = new StringBuilder();
        assert listFiles != null;
        if (listFiles.length != 0) {
            for (String file : listFiles) {
                sOut.append("[").append(file).append("]\n\r");
            }
            channel.write(ByteBuffer.wrap(sOut.toString().getBytes(StandardCharsets.UTF_8)));
        } else {
            channel.write(ByteBuffer.wrap("dir is empty\n\r".getBytes(StandardCharsets.UTF_8)));
        }
    }

    public static void main(String[] args) throws IOException {
        new NioEchoServer();
    }
}