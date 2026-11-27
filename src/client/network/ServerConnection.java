package client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import common.Command;
import common.Message;
import common.Protocol;


public class ServerConnection {

	// 생성은 connect 실행시

	private Socket socket; // 서버와 클라이언트 사이의 연결 통로
	private BufferedReader in; // 서버에서 데이터를 읽어오는 역할
	private PrintWriter out; // 서버로 데이터를 보내는 역할
	private ServerListener listener; // 메시지를 전달해줄 객체
	private Thread receiveThread;
	private boolean connected;

	// 리스너 등록
	public void setListener(ServerListener listener) {
		this.listener = listener;
	}

	// 소켓・스트림 열고 receiveThread 시작
	public void connect(String host, int port) throws IOException { // 연결 실패는 불러온 곳(ChatClient)에서 처리
		socket = new Socket(host, port);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Protocol.CHARSET));
		out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), Protocol.CHARSET), true);
		setConnected(true);
		receiveLoop();

	}

	// synchronized 처리한 메시지 전송 (JSON sent)
	public synchronized void send(Command type, Object data) {
		out.println(new Message(type, data).toLine()); // message를 JSON문자열로 변환해 서버에 전송, println은 자동 flush 설정 됨
	}

	// 수신Thread 만들고 받은 문자열 처리 (JSON received)
	private void receiveLoop() {
		receiveThread = new Thread(() -> {
			try {
				String line; // JSON 문자열
				while ((line = in.readLine()) != null) { // readline 반복해서 빼내고, null = 서버가 연결을 닫음
					try {
						Message msg = Message.parse(line); // JSON 문자열을 Message(type + DTO)로 변환
						listener.onMessage(msg); // 변환한 Message를 리스너에 전달 - 여기부터 Controller가 처리
					} catch (RuntimeException e) {
						System.out.println("[수신 오류] 처리할 수 없는 메시지: " + line + " (" + e.getMessage() + ")");
					}
				}
			} catch (IOException e) {
				if (isConnected()) { // 내가 직접 끊은 경우는 오류 아니므로 출력 안 함
					e.printStackTrace();
				}
			} finally {
				setConnected(false);
				if (listener != null) {
					listener.onDisconnected();// 연결 끊겼다고 알림
				}
			}
		});

		receiveThread.start();

	}

	public void disconnect() {
		setConnected(false);
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private synchronized void setConnected(boolean value) { // 쓰는 쪽
		connected = value;
	}
	
	public synchronized boolean isConnected() { // 읽는 쪽
		return connected;
	}

}