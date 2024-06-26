package com.example.chatting.server;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345);) {
            System.out.println("서버가 준비되었습니다.");

            // 클라이언트의 정보를 넣을 자료구조
            Map<String, PrintWriter> clients = new HashMap<>();

            // 사용자에 해당하는 방 정보를 넣을 자료구조
            Map<String, Integer> userRooms = new HashMap<>();

            // 방에 해당하는 채팅 로그를 넣을 자료구조
            Map<Integer, List<String>> roomLogs = new HashMap<>();

            while (true) {
                Socket clientSocket = serverSocket.accept();

                new ChatServerThread(clientSocket, clients, userRooms, roomLogs).start();
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}