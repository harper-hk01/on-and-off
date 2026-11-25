package client.network;

import java.io.IOException;

import javax.swing.SwingUtilities;

import client.controller.ChatController;
import client.controller.CommandHandler;
import client.model.ChatModel;
import client.view.MainView;
import common.Protocol;

public class ChatClient {

	public static void main(String[] args) {

		// 0. 아래 전부를 SwingUtilities.invokeLater 안에서 실행 (익명 Runnable)
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// 1. 객체 생성 & 연결
				ServerConnection connection = new ServerConnection();
				ChatModel model = new ChatModel();
				CommandHandler commandHandler = new CommandHandler();
				ChatController controller = new ChatController(connection, model, commandHandler);
				MainView mainView = new MainView(controller, model);
				// 2. controller에 view 연결
				controller.setMainView(mainView);
				
				try {
					// 3. 서버 접속 (connect(HOST, PORT))
					connection.connect(Protocol.HOST, Protocol.PORT);
				} catch (IOException e) {
					// 실패하면 어떤 방법으로 안내하고 프로그램 종료할 건지 정하기
					System.out.println("[접속 실패] " + e.getMessage());
					System.exit(1);
				}
				
				// 4. 첫 화면 띄우기 (showLogin)
				mainView.showLogin();
			}
		});
	}
}