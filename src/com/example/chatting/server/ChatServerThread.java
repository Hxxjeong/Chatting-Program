package com.example.chatting.server;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Map.*;

class ChatServerThread extends Thread {
    private Socket socket;
    private String id;
    private Map<String, PrintWriter> clients;   // 클라이언트 메시지
    private Map<String, Integer> userRooms; // 클라이언트의 방

    private BufferedReader br;
    private PrintWriter pw;
    private Map<Integer, List<String>> roomLogs;

    private static AtomicInteger nextRoomNumber = new AtomicInteger(1);

    // 다음 사용 가능한 방 번호를 반환
    public static int getNextRoomNumber() {
        return nextRoomNumber.getAndIncrement();
    }

    public ChatServerThread(Socket socket, Map<String, PrintWriter> clients, Map<String, Integer> userRooms, Map<Integer, List<String>> roomLogs) {
        this.socket = socket;
        this.clients = clients;
        this.userRooms = userRooms;
        this.roomLogs = roomLogs;

        try {
            pw = new PrintWriter(socket.getOutputStream(), true);
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            while(true) {
                // 클라이언트가 접속 시 id 설정
                id = br.readLine();

                // id가 이미 있는 경우
                if (clients.containsKey(id))
                    pw.println("이미 존재하는 닉네임입니다. 다시 입력해주세요.");
                    // 공백인 경우
                else if (id.isEmpty() || id.equals(" "))
                    pw.println("닉네임을 공백으로 설정할 수 없습니다.");
                else break;
            }

            // 접속 시 클라이언트의 정보 출력
            System.out.println("접속 멤버: " + id + ", Client Port: " + socket.getInetAddress());

            printGuide();

            // 동시에 입장하는 경우 고려
            synchronized (clients) {
                clients.put(this.id, pw);
                userRooms.put(this.id, 0);    // 처음에 입장 시 방번호 0번
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }

    @Override
    public void run() {
        String msg;
        try {
            while ((msg = br.readLine()) != null) {
                // 접속 종료
                if("/bye".equalsIgnoreCase(msg)) {
                    pw.println("접속을 종료합니다.");
                    break;
                }

                boolean isSameClient = userRooms.get(this.id).equals(0);

                // 방 목록 보기
                if("/list".equalsIgnoreCase(msg))
                    seeRooms();
                // @id 메시지 형식 귓속말
                // 로비에서도 귓속말 가능
                else if(msg.indexOf("@") == 0)
                    toSomeone(msg);
                // 사용자 목록 확인
                else if ("/users".equalsIgnoreCase(msg))
                    checkMembers();
                // 방 생성
                else if("/create".equalsIgnoreCase(msg)) {
                    synchronized (userRooms) {
                        int room = getNextRoomNumber();
                        userRooms.put(this.id, room);
                        enterRoom();
                        System.out.println(room + "번 방이 생성되었습니다.");
                    }
                }
                // 방 참가
                else if(msg.startsWith("/join")) {
                    synchronized (userRooms) {
                        try {
                            // 공백으로 분리
                            int firstSpaceIndex = msg.indexOf(" "); // 첫번째 공백의 인덱스
                            int roomNum = Integer.parseInt(msg.substring(firstSpaceIndex + 1));

                            // 방이 존재할 떄만 들어가기
                            if (userRooms.values().stream().noneMatch(r -> r.equals(roomNum)) || roomNum == 0)
                                pw.println("방 번호를 올바르게 입력해주세요.");
                            else {
                                userRooms.put(this.id, roomNum);
                                enterRoom(roomNum);
                            }
                        }
                        catch (NumberFormatException e) {
                            pw.println("방은 숫자로 구성되어 있습니다. 올바르게 입력해주세요.");
                        }
                        catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                }
                // 사용자의 방 번호가 0이면 채팅 불가
                else if(isSameClient)
                    pw.println("방에 먼저 입장해주세요. /create: 방 생성, /join [방번호]: 방 입장");
                // 현재 방에 있는 사용자 보기
                else if(!isSameClient && "/roomusers".equalsIgnoreCase(msg))
                    seeCurrentRoomUsers();
                else if(!isSameClient && "/save".equalsIgnoreCase(msg))
                    saveChat();
                // 방 나가기
                else if(!isSameClient && "/exit".equalsIgnoreCase(msg)) {
                    synchronized (userRooms) {
                        exitRoom();
                    }
                }
                // 방에 참가된 경우 메시지 주고 받기 가능
                else /*if(!userRooms.get(this.id).equals(0))*/
                    sendMessage(msg);
            }
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            // 동시에 종료하는 경우 고려
            synchronized (clients) {
                clients.remove(id);
            }
            System.out.println("종료한 멤버: " + id);

            // 사용 후 BufferedReader와 Socket 닫기
            if(br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if(socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    // 로비 메시지
    public void printGuide() {
        pw.println("방 목록 보기 : /list\n" +
                "접속 유저 보기 : /users\n" +
                "귓속말 : @[id] [메시지]\n" +
                "방 생성 : /create\n" +
                "방 입장 : /join [방번호]\n" +
                "방 나가기 : /exit\n" +
                "접속종료 : /bye\n");
    }

    // 방의 리스트 보기
    public void seeRooms() {
        synchronized (userRooms) {
            // 방 목록
            Map<String, Integer> existRooms = userRooms.entrySet().stream()
                    .filter(entry -> entry.getValue() != 0)  // 방 번호가 0이 아닌 것만 필터링
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

            // 방 목록이 비어있는지 확인하여 메시지 전송
            if (existRooms.isEmpty()) {
                pw.println("방이 없습니다. 방을 생성하려면 /create를 입력하세요.");
            }
            else {
                pw.print("방 목록: ");
                existRooms.forEach((roomNumber, roomName) -> pw.print(roomName + " "));
                pw.println();
            }
        }
    }

    public void checkMembers() {
        pw.print("접속 중인 사용자 목록: ");
        Set<String> users = new HashSet<>(clients.keySet());
        pw.println(users);
    }

    // 방 참가
    // create 해서 들어온 경우
    public void enterRoom() {
        int currentRoom = userRooms.get(this.id);   // 들어온 방 번호

        // 방 멤버들에게 입장 알림
        sendMessageToRoom(currentRoom, id + "님이 입장하였습니다.");
    }

    // join 해서 들어온 경우
    public void enterRoom(int room) {
        sendMessageToRoom(room, id + "님이 입장하였습니다.");
    }

    public void saveChat() {
        int roomNum = userRooms.get(this.id);
        String filename = this.id + "saved_" + roomNum + "_chatlog.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            List<String> messages = roomLogs.get(roomNum);
            if(messages != null) {
                for (String message : messages) {
                    writer.write(message);
                    writer.newLine();
                }
            }
            pw.println("채팅 내용이 저장되었습니다. 프로그램 종료 시 파일이 생성됩니다.");
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    // 방 나가기
    public void exitRoom() {
        int currentRoom = userRooms.get(this.id);
        userRooms.put(this.id, 0); // 사용자의 방 번호를 0으로 설정
        pw.println("방을 퇴장하였습니다.");

        synchronized (clients) {
            // 해당 방에 남아있는 사용자가 있는지 확인
            boolean isRoomEmpty = userRooms.values().stream()
                    .noneMatch(room -> room == currentRoom);

            // 비었으면 방 삭제
            if (isRoomEmpty) {
                System.out.println(currentRoom + "번 방이 삭제되었습니다.");
            }
            else {
                // 방에 남아있는 사용자에게 메시지 전송
                sendMessageToRoom(currentRoom, id + "님이 퇴장했습니다.");
            }
        }
        // 로비로 돌아오면 가이드 메시지 출력
        printGuide();
    }

    // 해당 방을 사용하는 사람에게 입장/퇴장 메시지 전송
    public void sendMessageToRoom(int room, String message) {
        synchronized (clients) {
            clients.forEach((clientId, clientPw) -> {
                if (userRooms.get(clientId).equals(room)) {
                    try {
                        clientPw.println(message);
                    } catch (Exception e) {
                        clients.remove(clientId);
                        e.printStackTrace();
                    }
                }
            });
            pw.println("현재 방에 있는 유저 목록 보기: /roomusers");
            pw.println("채팅 내용 저장: /save");
        }
    }

    // 현재 방에 있는 유저 보기
    public void seeCurrentRoomUsers() {
        int currentRoomNum = userRooms.get(this.id);

        pw.print("현재 방에 접속한 유저 목록: ");
        userRooms.entrySet().stream()
                .filter(u -> u.getValue().equals(currentRoomNum))
                .map(Entry::getKey)
                .forEach(user -> pw.print(user + " "));
        pw.println();
    }

    // 메시지 보내기
    public void sendMessage(String msg) {
        int currentRoom = userRooms.get(this.id); // 현재 클라이언트의 방 번호

        synchronized (clients) {
            clients.forEach((clientId, clientPw) -> {
                // 현재 클라이언트와 같은 방에 있는 클라이언트에게만 메시지 전송
                if (userRooms.get(clientId).equals(currentRoom)) {
                    try {
                        clientPw.println(id + ": " + msg);
                    } catch (Exception e) {
                        clients.remove(clientId);
                        e.printStackTrace();
                    }
                }
            });
            // 채팅 로그 저장
            roomLogs.computeIfAbsent(currentRoom, k -> new ArrayList<>()).add(id + ": " + msg);
        }
    }
    
    // 귓속말 기능
    public void toSomeone(String msg) {
        try {
            int spaceIndex = msg.indexOf(" ");

            String targetId = msg.substring(1, spaceIndex); // @이후부터 공백 전까지
            String message = msg.substring(spaceIndex+1);   // 공백 이후

            // 수신자에게 메시지 전송
            PrintWriter out = clients.get(targetId);
            if (targetId.equals(this.id)) {
                pw.println("본인에게는 메시지를 보낼 수 없습니다.");
            } else if (out != null) {
                pw.println("메시지를 전송하였습니다.");
                out.println(id + "님의 귓속말: " + message);
            } else {
                pw.println(targetId + " 님을 찾을 수 없습니다.");
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }
}
