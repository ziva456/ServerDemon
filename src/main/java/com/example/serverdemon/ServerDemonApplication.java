package com.example.serverdemon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;

@SpringBootApplication
public class ServerDemonApplication {

    public static void main(String[] args) throws IOException {
        SpringApplication.run(ServerDemonApplication.class, args);
        SocketChannelThread socketChannelThread = new SocketChannelThread();
        socketChannelThread.start();

    }


}
