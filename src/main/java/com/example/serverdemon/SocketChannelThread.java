package com.example.serverdemon;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

@Slf4j
public class SocketChannelThread extends Thread{
    InetSocketAddress addr = new InetSocketAddress("localhost", 7777);
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    public void run() {

        try {
            Selector selector = Selector.open();
            ServerSocketChannel serverSocket  = ServerSocketChannel.open();
            serverSocket.configureBlocking(false);				//소켓채널 비차단모드로 설정
            serverSocket.socket().bind(this.addr);				//서버주소 지정
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);	//연결요청 수락모드로 설정

            while (!Thread.currentThread().isInterrupted()) {

                if(selector.select()>0){
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> it = selectedKeys.iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();

                        if (key.isAcceptable()) {
                            log.info("REGI");
                            register(selector, serverSocket);
                        }
                        //Echo
                        if (key.isWritable()) {
                            log.info("SEND");
                            sendMessage(selector,key);
                        }

                        if (key.isReadable()) {
                            log.info("RECV");
                            recvMessage(selector,key);
                        }

                    it.remove();
                    }
                }
            }
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendMessage(Selector selector, SelectionKey key) throws IOException, InterruptedException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        socketChannel.write(buffer);
        if (!buffer.hasRemaining()) {
            log.info("Send message  : {}" ,socketChannel.getRemoteAddress());
            socketChannel.register(selector, SelectionKey.OP_READ);
        }
    }
    private void recvMessage(Selector selector, SelectionKey key) throws IOException, InterruptedException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        log.info("SocketChannel client : {}", socketChannel);
        buffer.clear();
        int bytesRead = socketChannel.read(buffer);
        if (bytesRead == -1) {                        //연결이 끊어졌을 때
            log.info("Connection closed");
          socketChannel.close();
//          throw new InterruptedException();
        }
            buffer.flip();
            //받은 메세지 찍어보기
//            String msg = StandardCharsets.UTF_8.decode(buffer).toString();
//            log.info("bytesRead : {}, msg : {}", bytesRead, msg);

            //Echo 기능
            byte[] bytes = new byte[buffer.limit()];
            buffer.get(bytes);
            System.out.println("Received message from " + socketChannel.getRemoteAddress() + ": " + new String(bytes));
            socketChannel.register(selector, SelectionKey.OP_WRITE, ByteBuffer.wrap(bytes));

    }

    private void register(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel socketChannel = serverSocket.accept();
        if (socketChannel != null) {
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            System.out.println("connected new client.");
        }
    }

}
