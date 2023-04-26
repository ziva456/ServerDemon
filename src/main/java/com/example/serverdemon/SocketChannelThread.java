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
    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;
    public SocketChannelThread() throws IOException{
        selector = Selector.open();
        serverSocketChannel  = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);				//서버소켓채널 비차단모드로 설정
        serverSocketChannel.socket().bind(this.addr);				//서버주소 지정
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);	//연결요청 수락
    }

    public void run() {

        try {
            while (!Thread.currentThread().isInterrupted()) {

                if(selector.select()>0){
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> it = selectedKeys.iterator();
                    while (it.hasNext()) {
                        SelectionKey key = it.next();


//                        log.info("isValid : {}, isAcceptable() : {} ,isReadable() : {},  isWritable() : {}, isConnectable() : {}",key.isValid(), key.isAcceptable(), key.isReadable(), key.isWritable(), key.isConnectable());

                        if (!key.isValid()) {
                            log.info("Error !!!");
                            continue;
                        }

                        if (key.isAcceptable()) {
                            log.info("REGI");
                            register(selector, serverSocketChannel);
                        }

                        if (key.isReadable()) {
                            log.info("RECV");
                            recvMessage(selector,key);
                        }

                        it.remove();
                    }
                }
            }
            serverSocketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
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
            key.cancel();
        }
            buffer.flip();
            //UTF_8 받은 메세지 찍어보기
//            String msg = StandardCharsets.UTF_8.decode(buffer).toString();
//            log.info("bytesRead : {}, msg : {}", bytesRead, msg);

            //Echo 기능
            byte[] bytes = new byte[buffer.limit()];
            buffer.get(bytes);
            log.info("Received : {}, msg :{}", socketChannel.getRemoteAddress() , new String(bytes));
            socketChannel.register(selector, SelectionKey.OP_WRITE, ByteBuffer.wrap(bytes));
    }

    private void register(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel socketChannel = serverSocket.accept();
        if (socketChannel != null) {
            socketChannel.configureBlocking(false); //client 소켓채널 비차단모드로 설정
            socketChannel.register(selector, SelectionKey.OP_READ);
            System.out.println("connected new client.");
        }
    }

}
