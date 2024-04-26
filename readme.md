### 자바 채팅 프로그램 서버 구현

<hr>




#### 구현 목록

0. 로비 입장

   - 닉네임 중복/공백 불가 처리

     ![image-20240426163130937](C:\Users\HJ\AppData\Roaming\Typora\typora-user-images\image-20240426163130937.png)

     ![image-20240426163035724](C:\Users\HJ\AppData\Roaming\Typora\typora-user-images\image-20240426163035724.png)

   - 입장 시 안내 메시지 출력



1. 방 목록 보기

   - "/list" 입력 시 방 목록 출력

     방이 없을 경우 생성하라는 메시지 전송

     ![image-20240426163211768](C:\Users\HJ\AppData\Roaming\Typora\typora-user-images\image-20240426163211768.png)



2. 방 생성

   - "/create" 입력 시 1부터 증가하여 새로운 방 생성

   - "/join [방 번호]" 입력 시 존재하는 방으로 입장

     방 번호가 존재하지 않거나 올바르지 않을 경우 메시지 전송

     ![image-20240426163713295](C:\Users\HJ\AppData\Roaming\Typora\typora-user-images\image-20240426163713295.png)

   - 방에 입장 시 멤버들에게 입장 메시지 전송

   - 해당 방에 있는 멤버끼리만 대화 가능



3. 방 나가기

   - 방에 있는 사람이 "/exit" 입력 시 로비로 나옴

     로비에서 "/exit" 입력 시 명령어가 적용되지 않음

     ![image-20240426163934426](C:\Users\HJ\AppData\Roaming\Typora\typora-user-images\image-20240426163934426.png)

   - 방에서 퇴장 시 멤버들에게 퇴장 메시지 전송



4. 접속 종료

   - "/bye" 입력 시 접속 종료

     클라이언트 종료 시 서버에 종료한 멤버 id 출력



#### 실행 방법

`ChatServer` 실행 후 `ChatClient` 실행





##### 추가로 구현해야 할 기능

1. 방에서 퇴장 후 로비에서 안내 메시지 다시 출력
2. 사용자 및 방 관련 정보 제공
   - "/users" 입력 시 접속 중인 사용자 목록 확인
   - "/roomusers" 입력 시 현재 방에 있는 사용자의 목록 확인
3. 귓속말 기능
   - "/whisper [닉네임] [메시지]" 입력 시 특정 사용자에게 메시지 전송
4. 채팅 내역 저장 기능
   - 유저 간 채팅 내용 파일로 저장

