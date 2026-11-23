package client.network;

import common.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
public class ServerConnection {

	//생성은 connect 실행시

	private Socket socket; //서버와 클라이언트 사이의 연결 통로
	private BufferedReader in; //서버에서 데이터를 읽어오는 역할
	private PrintWriter out; //서버로 데이터를 보내는 역할
	private ServerListener listener; //메시지를 전달해줄 객체
	private Thread receiveThread;
	private boolean connected;


	public void setListener(ServerListener listener) {
		this.listener = listener;
	}

	//connect

	public void connect(String host, int port) throws IOException { //연결 실패는 불러온 곳(ChatClient)에서 처리

		socket = new Socket(host, port);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
		out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
		connected = true;
		receiveLoop();

	}

	//JSON send

	//synchronized : 여러 메시지가 동시에 전송될 때 전송 순서나 출력 처리가 꼬이는 상황을 방지하기 위해서

	public synchronized void send(Command type, Object data) {
		out.println(new Message(type, data).toLine()); //message를 JSON문자열로 변환해 서버에 전송, println은 자동 flush 설정 됨
	}

	//JSON receive 수신쓰레드 만들고 받은거 처리

	private void receiveLoop() {
		receiveThread = new Thread(()->{
			try {
				String line; //JSON 문자열
				while((line = in.readLine())!=null) { //readline 반복해서 빼내고, null의미 : 서버연결 끊김
					Message msg = Message.parse(line); //JSON 문자열을 message로 변환
					listener.onMessage(msg); //메시지 들어왔다고 알림, 메시지 전달
				}

			} catch (IOException e) {

			    if (connected) { //이걸
			        e.printStackTrace();
			    }

			} finally {
				connected = false;
			    if (listener != null) {
			        listener.onDisconnected();//연결 끊겼다고 알림
			    }

			}

		});

		receiveThread.start();

	}

	public void disconnect() {

		System.out.println("[확인] disconnect() 실행");
		connected = false;
		try {
			socket.close();
		} catch (IOException e) {

			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean isConnected() {
		return connected;
	}


}