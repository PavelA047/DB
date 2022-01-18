package com.geekbrains.DB.nio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class ServerTest {

    public static void main(String[] args) throws IOException {
        NioEchoServer nioEchoServer = new NioEchoServer(new TelnetTerminalProcessor());
        nioEchoServer.start();
    }
}
