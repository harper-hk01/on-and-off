package server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import common.Protocol;
import server.dao.*;
import server.service.*;
import server.session.SessionRegistry;

public class ChatServer {

	public static void main(String[] args) {
		
		ServerSocket serverSocket = null;
		
		// TODO Auto-generated method stub
		UserDAO userDAO = new UserDAO();
		FriendDAO friendDAO = new FriendDAO();
        RoomDAO roomDAO = new RoomDAO();
        MessageDAO messageDAO = new MessageDAO();
        
        SessionRegistry registry = new SessionRegistry();
        
        // MeetingService를 먼저! RoomService와 ChatService가 MeetingService를 받음
        MeetingService meetingService = new MeetingService(roomDAO, userDAO, registry);
        UserService userService = new UserService(userDAO, friendDAO, registry);
        FriendService friendService = new FriendService(friendDAO, userDAO, registry);
        RoomService roomService = new RoomService(roomDAO, userDAO, messageDAO, meetingService, registry, friendDAO);
        ChatService chatService = new ChatService(messageDAO, roomDAO, userDAO, meetingService, registry);
        
        CommandDispatcher dispatcher =
                new CommandDispatcher(userService, friendService, chatService, roomService, meetingService);
        
        try {
            userService.resetAllOffline(); // 모든 사용자의 상태 OFFLINE으로
            meetingService.startScheduler();  // 회의독려 감시 스레드
            
        	serverSocket = new ServerSocket(Protocol.PORT); // 지정된 포트에서 새 창구 열기
        	consoleLog(serverSocket.getInetAddress().getHostAddress()+":"+Protocol.PORT+" 서버 시작됨");
			
        	while(true) {
        		Socket socket = serverSocket.accept(); // 사용자 1명 전용 socket
        		consoleLog("클라이언트가 연결되었습니다: " + socket.getRemoteSocketAddress());
        		new ClientHandler(socket, dispatcher).start(); // 사용자 1명 전담 객체 만들고 socket/dispatcher 넣어줌
        	}
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
        
        
	}

	private static void consoleLog(String string) {
		// TODO Auto-generated method stub
		System.out.println("[server "+Thread.currentThread().threadId()+ "] "+string);
	}

}
