package server.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

import common.Command;
import common.Message;
import common.Protocol;
import dto.response.ErrorDTO;
import server.session.ClientSession;

public class ClientHandler extends Thread implements ClientSession {
	private Socket socket = null;
	private BufferedReader in = null;
	private PrintWriter out = null;
	private CommandDispatcher dispatcher = null;
	private long userId = 0;
	private String loginId = null;

	public ClientHandler(Socket socket, CommandDispatcher dispatcher) {
		this.socket = socket;
		this.dispatcher = dispatcher;
	}

	@Override
	public void run() {
		super.run();
		log("접속: " + socket.getInetAddress().getHostAddress());
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Protocol.CHARSET));
			out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Protocol.CHARSET), true);
			String request;

			while (true) {
				request = in.readLine();

				if (request == null) {
					log("클라이언트로부터 연결 끊김 (EOF)");
					break;
				}

				log("받은 원문: " + request); // 테스트용

				Message msg;
				try {
					msg = Message.parse(request);
				} catch (IllegalArgumentException e) {
					log("형식 오류");
					send(Command.ERROR, new ErrorDTO(null, "잘못된 메시지 형식"));
					continue;
				}

				log("변환 성공: " + msg); // 테스트용
				dispatcher.dispatch(this, msg);

			}
		} catch (SocketException e) {
			log("클라이언트 연결 종료 (" + e.getMessage() + ")");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			dispatcher.disconnect(this);
			close();
			log("종료");
		}
	}

	// 모든 S->C 명령어 보낼 때
	public synchronized void send(Command type, Object data) { // println있어서 synchronized 없어도 되나
		out.println(new Message(type, data).toLine());
	}

	public void setLoginUser(long userId, String loginId) { // 핸들러에 이름표 붙이기
		this.userId = userId; // 유저에게 부여되는번호
		this.loginId = loginId; // 로그인에 사용되는 아이디
	}

	public void clearLoginUser() { // 연결은 유지하고 로그인 정보 지우기
		this.userId = 0;
		this.loginId = null;
	}

	public boolean isLoggedIn() {
		if (userId != 0)
			return true;
		else
			return false;
	}

	public long getUserId() {
		return userId;
	}

	public String getLoginId() {
		return loginId;
	}

	// 소켓 닫기
	private void close() {
		try {
			socket.close();
		} catch (IOException e) {
			// 이미 닫혔으면 무시
		}
	}

	private void log(String log) {
		// TODO Auto-generated method stub
		System.out.println("[" + getName() + "] " + log);
	}

}