package server.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import common.Protocol;
import server.dao.FriendDAO;
import server.dao.MessageDAO;
import server.dao.RoomDAO;
import server.dao.UserDAO;
import server.exception.DAOException;
import server.service.ChatService;
import server.service.FriendService;
import server.service.MeetingService;
import server.service.RoomService;
import server.service.UserService;
import server.session.SessionRegistry;

/*
 * 서버 시작점. 객체를 조립하고 접속을 받을 때마다 ClientHandler에 맡김
 */

public class ChatServer {

	public static void main(String[] args) {

		// 1. 객체 조립
		UserDAO userDAO = new UserDAO();
		FriendDAO friendDAO = new FriendDAO();
		RoomDAO roomDAO = new RoomDAO();
		MessageDAO messageDAO = new MessageDAO();
		SessionRegistry registry = new SessionRegistry();

		MeetingService meetingService = new MeetingService(roomDAO, userDAO, registry);
		UserService userService = new UserService(userDAO, friendDAO, registry);
		FriendService friendService = new FriendService(friendDAO, userDAO, registry);
		RoomService roomService = new RoomService(roomDAO, userDAO, messageDAO, meetingService, registry, friendDAO);
		ChatService chatService = new ChatService(messageDAO, roomDAO, userDAO, meetingService, registry);

		CommandDispatcher dispatcher = new CommandDispatcher(userService, friendService, chatService, roomService,
				meetingService);

		// 2. 시작 준비
		try {
			userService.resetAllOffline(); // 모든 사용자 OFFLINE으로
		} catch (DAOException e) {
			consoleLog("DB에 연결 실패: DB 서버 실행 여부와 db.properties 설정을 확인하세요.");
			return;
		}

		// 3. 회의독려 감시 스레드 시작
		meetingService.startScheduler();
		// 4. 접속 대기
		acceptClients(dispatcher);
	}

	// ==== 포트를 열고 접속이 올 때마다 전담 ClientHandler 생성 ====
	private static void acceptClients(CommandDispatcher dispatcher) {
		try (ServerSocket serverSocket = new ServerSocket(Protocol.PORT)) { // 자원이 자동으로 close()되도록
			consoleLog("서버 시작됨 IP: " + InetAddress.getLocalHost().getHostAddress() + " PORT:" + Protocol.PORT);

			while (true) {
				Socket socket = serverSocket.accept(); // 사용자 1명 전용 socket
				consoleLog("클라이언트 연결: " + socket.getInetAddress().getHostAddress());
				new ClientHandler(socket, dispatcher).start(); // 사용자 1명 전담 객체 만들고 socket/dispatcher 넣어줌
			}
		} catch (IOException e) {
			consoleLog("서버 소켓 오류로 종료합니다. 포트 " + Protocol.PORT + "가 이미 사용 중인지 확인하세요.");
			e.printStackTrace();
		}
	}

	private static void consoleLog(String message) {
		System.out.println("[server] " + message);
	}

}
