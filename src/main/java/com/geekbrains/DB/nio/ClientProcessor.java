package com.geekbrains.DB.nio;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface ClientProcessor {

    void onMessageReceived(String message, SocketChannel channel) throws IOException;

    void onClientAccepted(SocketChannel acceptedChannel) throws IOException;

    void onClientDisconnected(SocketChannel channel) throws IOException;

    void onExceptionCaught(Throwable ex) throws IOException;

}
