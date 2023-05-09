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

@Slf4j
public class SocketChannelThread extends Thread{
    InetSocketAddress addr = new InetSocketAddress("localhost", 7777);
    private final ByteBuffer buffer = ByteBuffer.allocate(1024);
    private final Selector selector;
    private final ServerSocketChannel serverSocketChannel;

    public SocketChannelThread() throws IOException {
        selector = Selector.open();
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(addr);
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        log.info("Server started on port  : {} " , 7777);
    }

    private void register() throws IOException {
        selector.select();
        Iterator<SelectionKey> it = selector.selectedKeys().iterator();
        while (it.hasNext()) {
            SelectionKey key = it.next();
            it.remove();
            if (!key.isValid() || !key.isAcceptable()) continue;
            ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
            SocketChannel channel = serverSocketChannel.accept();
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            log.info("Client connected:{} ", channel.getLocalAddress());
        }
    }
    private void recvMessage() throws IOException, InterruptedException {
        log.info("recvMessage");

        //타이머 5초 적용
//        long timeout = 5000;
//        long sleep = Math.min(timeout, 1000);
//
//        while (timeout > 0) {
//            if (selector.select(sleep) < 1) {  //Select 값이 0이면 채널 없음 0이 아니면 채널 있음 1초마다 찾아보기
//                timeout -= sleep;
//                continue;
//            }


        //응답받는 즉시
        while (true){
            int readyChannels = selector.select();
            if (readyChannels == 0) continue;

            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();
                log.info("isValid : {}, isAcceptable() : {} ,isReadable() : {},  isWritable() : {}, isConnectable() : {}",key.isValid(), key.isAcceptable(), key.isReadable(), key.isWritable(), key.isConnectable());
                if (!key.isValid() || !key.isReadable()) continue;

                SocketChannel channel = (SocketChannel) key.channel();
                log.info("SocketChannel client : {}, key : {}", channel, key);
                buffer.clear();
                int bytesRead = channel.read(buffer);
                if (bytesRead == -1) {                        //연결이 끊어졌을 때
                    log.info("Connection closed");
                    channel.close();
                    throw new InterruptedException();
                }
                buffer.flip();
                //UTF_8 받은 메세지 찍어보기
    //            String msg = StandardCharsets.UTF_8.decode(buffer).toString();
    //            log.info("bytesRead : {}, msg : {}", bytesRead, msg);

                //Echo 기능
                byte[] bytes = new byte[buffer.limit()];
                buffer.get(bytes);
                log.info("Received : {}, msg :{}, key : {}", channel.getRemoteAddress() , new String(bytes), key);

                log.info("Before : {}", bytes.length);
                channel.write(ByteBuffer.wrap(new String(bytes).getBytes()));
                log.info("After : {}", bytes.length);
                log.info("Echo");
            }
        }
    }

    public void closeSocket() {
        try {
            if(serverSocketChannel!=null) serverSocketChannel.close();
        }catch (Exception ex){ }
        try {
            if(selector!=null) selector.close();
        }catch (Exception ex){ }

    }

    public void run() {

        try {
            register();
            while (!Thread.currentThread().isInterrupted()) {
                recvMessage();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            closeSocket();
        }
    }

}
