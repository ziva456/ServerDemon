package com.example.serverdemon;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

@Slf4j
public class SocketChannelThread extends Thread{
    InetSocketAddress addr = new InetSocketAddress("localhost", 7777);
//    private static final String EXIT = "EXIT";
    public static long runTime = 0;

    public void run() {

        try {
            Selector selector = Selector.open();
            ServerSocketChannel serverSocket  = ServerSocketChannel.open();
            serverSocket.configureBlocking(false);				//소켓채널 비차단모드로 설정
            serverSocket.socket().bind(this.addr);				//서버주소 지정
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);	//연결요청 수락모드로 설정

            ByteBuffer buffer = ByteBuffer.allocate(256);

            while (!Thread.currentThread().isInterrupted()) {

                if(selector.select()>0){
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> it = selectedKeys.iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();

                        if (key.isAcceptable()) {
                            log.info("register");
                            register(selector, serverSocket);
                        }

                        if (key.isConnectable()) {
                            System.out.format("Connectable is for client.%n");
                        }

                        if (key.isReadable()) {
                            log.info("readable");
                            answerWithEcho(buffer,key);
                        }

                        if (key.isWritable()) {
                            log.info("readable");
                            System.out.format("Writable is activated.%n");
                        }
                        it.remove();
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void answerWithEcho(ByteBuffer buffer, SelectionKey key)
            throws IOException, InterruptedException {
        log.info("buffer : {}",buffer.remaining());
        SocketChannel client = (SocketChannel) key.channel();
        log.info("SocketChannel client : {}", client);
        buffer.clear();

        int bytesRead = client.read(buffer);
        if (bytesRead == -1) {						//연결이 끊어졌을 때
            throw new InterruptedException();
        }
        buffer.flip();
        String msg = StandardCharsets.UTF_8.decode(buffer).toString();
        log.info("bytesRead : {}, msg : {}", bytesRead, msg);
    }

    private static void register(Selector selector, ServerSocketChannel serverSocket)
            throws IOException {
        SocketChannel client = serverSocket.accept();
        if (client != null) {
            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);
            System.out.println("connected new client.");
        }
    }


    private static long diffTime(long runTime) {
        runTime = (System.currentTimeMillis() - runTime) / 1000;
        return runTime;
    }
}
