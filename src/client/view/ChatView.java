package client.view;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import client.controller.ChatController;
import client.model.ChatModel;
import dto.common.ChatRoomDTO;
import dto.common.MessageDTO;
import dto.common.RoomType;
import dto.common.UserDTO;

public class ChatView extends JFrame implements ActionListener {

	// 현재 채팅방 정보, 모델 컨트롤러
	private long roomId;
	private ChatController controller;
	private ChatModel model;
	private boolean isDaily; // 일상방이면 true, 업무방이면 false

	// 상단 - 방 이름, 공지
	private final JLabel roomTitle = new JLabel("방 이름");
	private final JPanel noticePanel = new JPanel(new BorderLayout());
	private final JLabel noticeLabel = new JLabel("공지 없음");

	// 중앙 - 대화 영역
	// lines에 행 추가하면 messageList에 표시
	private final JList<ChatLine> messageList = new JList<>();
	private final DefaultListModel<ChatLine> lines = new DefaultListModel<>();
	private final JTextField input = new JTextField();

	// 일상방 전용 - 입력 중 표시, 버즈
	private final JPanel dailyPanel = new JPanel(new BorderLayout());
	private final JLabel typingLabel = new JLabel("입력 중 표시");
	private final JButton buzzButton = new JButton("버즈");

	// 업무방 전용 - 회의 기능들
	private final JPanel workPanel = new JPanel(new BorderLayout());
	private final JLabel nudgeLabel = new JLabel("회의 독려 알림");
	private final JButton startMeeting = new JButton("회의 시작");
	private final JButton endMeeting = new JButton("회의 종료");

	// 타이머
	private Timer typingTimer;
	private Timer nudgeTimer;
	private Timer buzzTimer;

	// 버즈 끝나고 창 원래 위치로 되돌리기
	private Point originalLocation;
	private int shakeCount;

	private long lastTypingTime;
	private Boolean meetingActive;

	// 기본 화면, 버튼 등 배치
	private void initializeUI() {
		setTitle("온앤오프 · 대화방");
		setSize(420, 570);
		setLocationByPlatform(true);

		// 채팅방 닫으면 자원 정리
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		JPanel content = new JPanel(new BorderLayout(4, 4));
		setContentPane(content);

		// 상단 공통 영역과 업무방 공지
		JPanel header = new JPanel(new BorderLayout());
		content.add(header, BorderLayout.NORTH);
		header.add(roomTitle, BorderLayout.NORTH);
		header.add(noticePanel, BorderLayout.SOUTH);
		noticePanel.add(noticeLabel, BorderLayout.CENTER);
		noticeLabel.setHorizontalAlignment(JLabel.CENTER);
		noticeLabel.setOpaque(true);
		noticeLabel.setBackground(new java.awt.Color(255, 243, 205));

		// 중앙 대화 내역 스크롤 팬으로, 공지는 1개 메세지만 선택 가능
		JScrollPane chatScroll = new JScrollPane();
		content.add(chatScroll, BorderLayout.CENTER);
		chatScroll.setViewportView(messageList);
		messageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// 하단 모드별 기능을 위, 입력칸은 아래
		JPanel footer = new JPanel(new BorderLayout());
		content.add(footer, BorderLayout.SOUTH);

		JPanel modeArea = new JPanel(new BorderLayout());
		footer.add(modeArea, BorderLayout.NORTH);

		// 일상방 전용 - 상대가 입력중
		modeArea.add(dailyPanel, BorderLayout.NORTH);
		dailyPanel.add(typingLabel, BorderLayout.CENTER);

		// 업무방 전용 - 독려 문구, 회의, 공지 버튼 4개
		modeArea.add(workPanel, BorderLayout.SOUTH);
		workPanel.add(nudgeLabel, BorderLayout.NORTH);
		nudgeLabel.setHorizontalAlignment(JLabel.CENTER);
		nudgeLabel.setOpaque(true);
		nudgeLabel.setBackground(new java.awt.Color(255, 100, 100));

		JPanel workButtons = new JPanel(new GridLayout(2, 2, 4, 4));
		workPanel.add(workButtons, BorderLayout.CENTER);
		workButtons.add(startMeeting);
		workButtons.add(endMeeting);

		JButton pinButton = new JButton("공지 설정");
		workButtons.add(pinButton);

		JButton unpinButton = new JButton("공지 해제");
		workButtons.add(unpinButton);

		// 모드 공통 입력 영역 : 메세지 입력칸 오른쪽에 전송/버즈
		JPanel inputPanel = new JPanel(new BorderLayout(4, 4));
		footer.add(inputPanel, BorderLayout.SOUTH);
		inputPanel.add(input, BorderLayout.CENTER);

		JPanel sendButtons = new JPanel(new GridLayout(2, 1));
		inputPanel.add(sendButtons, BorderLayout.EAST);

		JButton sendButton = new JButton("전송");
		sendButtons.add(sendButton);

		// 버즈는 공통 영역에 배치하되 활성은 일상방에서만
		sendButtons.add(buzzButton);

		// 엔터/전송 버튼 둘다 send 실행
		input.setActionCommand("send");
		input.addActionListener(this);
		sendButton.setActionCommand("send");
		sendButton.addActionListener(this);

		// 버즈 요청
		buzzButton.setActionCommand("buzz");
		buzzButton.addActionListener(this);

		// 회의 상태에 따라 회의 시작/종료 요청
		startMeeting.setActionCommand("meeting");
		startMeeting.addActionListener(this);
		endMeeting.setActionCommand("meeting");
		endMeeting.addActionListener(this);

		// 공지 설정
		pinButton.setActionCommand("pin");
		pinButton.addActionListener(this);
		unpinButton.setActionCommand("unpin");
		unpinButton.addActionListener(this);
	}

	// 생성자, 인자로 방 정보/컨트롤러/모델
	public ChatView(long roomId, ChatController controller, ChatModel model) {
		initializeUI();
		this.roomId = roomId;
		this.controller = controller;
		this.model = model;

		// 방 정보->방 종류, 제목 결정
		ChatRoomDTO room = model.getRoom(roomId);
		if (room == null) {
			throw new IllegalArgumentException("방 정보를 찾을 수 없습니다. " + roomId);
		}

		isDaily = room.getRoomType() == RoomType.DAILY;
		messageList.setModel(lines);

		// 방 열었을때 모델 내용을 화면에 반영
		applyRoomMode();
		refreshHeader();
		showMessages(model.getMessages(roomId));
		showNotice(room.getPinnedMessage());
		setMeetingState(model.isMeetingActive(roomId));

		// 일상 방이면 입력중 표시
		if (isDaily) {
			installTypingListener();
			model.getFriends().addListDataListener(friendChanges);
		}

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				controller.closeChatView(roomId);
			}
		});
	}

	// 모드별 적용
	private void applyRoomMode() {
		dailyPanel.setVisible(isDaily);
		workPanel.setVisible(!isDaily);
		noticePanel.setVisible(!isDaily);
		typingLabel.setText(" ");
		nudgeLabel.setVisible(false);
		buzzButton.setEnabled(isDaily);
	}

	// 데이터 리스너
	private final ListDataListener friendChanges = new ListDataListener() {
		public void intervalAdded(ListDataEvent e) {
			refreshHeader();
		}

		public void intervalRemoved(ListDataEvent e) {
			refreshHeader();
		}

		public void contentsChanged(ListDataEvent e) {
			refreshHeader();
		}
	};

	// 상단 리프레쉬
	private void refreshHeader() {
		ChatRoomDTO room = model.getRoom(roomId);
		if (room == null) {
			return;
		}

		UserDTO other = MainView.peer(room, model);
		String title = MainView.roomTitle(room, other);
		if (other != null) {
			title = "[" + other.getStatus() + "] " + title;
		} else if (!isDaily && room.getMembers() != null) {
			title += "   멤버 " + room.getMembers().size() + "명";
		}
		roomTitle.setText(title);
	}

	// 액션이벤트 처리
	@Override
	public void actionPerformed(ActionEvent event) {
		switch (event.getActionCommand()) {
		case "send":
			String text = input.getText();
			if (!text.isBlank()) {
				controller.sendMessage(roomId, text);
				input.setText("");
				lastTypingTime = 0;
				input.requestFocusInWindow();
			}
			break;
		case "buzz":
			controller.buzz(roomId);
			break;
		case "meeting":
			controller.toggleMeeting(roomId);
			break;
		case "pin":
			ChatLine selected = messageList.getSelectedValue();
			if (selected == null || selected.message == null) {
				JOptionPane.showMessageDialog(this, "공지로 설정할 메시지를 선택해 주세요.");
				return;
			}
			controller.setNotice(roomId, selected.message.getMessageId());
			break;
		case "unpin":
			controller.setNotice(roomId, null);
			break;
		}
	}

	private void installTypingListener() {
		input.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				notifyTyping();
			}

			public void removeUpdate(DocumentEvent e) {
				notifyTyping();
			}

			public void changedUpdate(DocumentEvent e) {
			}
		});
	}

	private void notifyTyping() {
		if (input.getText().isBlank()) {
			return;
		}
		long now = System.nanoTime();
		if (lastTypingTime == 0 || now - lastTypingTime >= 750_000_000L) {
			controller.typing(roomId);
			lastTypingTime = now;
		}
	}

	public void showTyping(String loginId) {
		if (!isDaily) {
			return;
		}

		if (typingTimer != null) {
			typingTimer.stop();
		}
		typingLabel.setText(" ");

		if (loginId == null || loginId.isBlank()) {
			return;
		}
		if (model.getMe() != null && loginId.equals(model.getMe().getLoginId())) {
			return;
		}

		String name = loginId;
		ChatRoomDTO room = model.getRoom(roomId);
		if (room != null) {
			UserDTO other = MainView.peer(room, model);
			if (other != null && loginId.equals(other.getLoginId())) {
				name = other.getNickname();
			}
		}
		typingLabel.setText(name + " 님이 입력 중…");

		if (typingTimer == null) {
			typingTimer = new Timer(2200, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					typingLabel.setText(" ");
				}
			});
			typingTimer.setRepeats(false);
		}
		typingTimer.restart();
	}

	// 메시지는 원본 DTO를 보관해야 선택한 메시지의 번호로 공지를 설정할 수 있다.
	private static class ChatLine {
		private final MessageDTO message;
		private final String text;

		ChatLine(MessageDTO message, String text) {
			this.message = message;
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	private ChatLine messageLine(MessageDTO msg) {
		String sender = msg.getSenderNickname();
		if (model.getMe() != null && model.getMe().getLoginId().equals(msg.getSenderId())) {
			sender = "나";
		}

		String time = msg.getSentAt();
		if (time == null) {
			time = "";
		} else if (time.length() >= 16) {
			time = time.substring(11, 16);
		}
		return new ChatLine(msg, "[" + sender + "] " + msg.getContent() + "   " + time);
	}

	public void showMessages(List<MessageDTO> history) {
		lines.clear();
		for (MessageDTO msg : history) {
			lines.addElement(messageLine(msg));
		}
		scrollToLast();
	}

	public void appendMessage(MessageDTO msg) {
		lines.addElement(messageLine(msg));
		if (isDaily && model.getMe() != null && !model.getMe().getLoginId().equals(msg.getSenderId())) {
			showTyping("");
		}
		scrollToLast();
	}

	public void showSystemText(String text) {
		lines.addElement(new ChatLine(null, text));
		scrollToLast();
	}

	private void scrollToLast() {
		if (!lines.isEmpty()) {
			messageList.ensureIndexIsVisible(lines.size() - 1);
		}
	}

	public void showNotice(MessageDTO msg) {
		if (isDaily) {
			return;
		}
		if (msg == null) {
			noticeLabel.setText("공지 없음");
		} else {
			noticeLabel.setText("공지   " + msg.getContent());
		}
	}

	public void setMeetingState(boolean active) {
		if (isDaily) {
			return;
		}

		if (meetingActive != null && meetingActive.booleanValue() != active) {
			String text = "회의 종료";
			if (active) {
				text = "회의 시작";
			}
			String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
			showSystemText("── " + text + " " + time + " ──");
		}

		meetingActive = active;
		startMeeting.setEnabled(!active);
		endMeeting.setEnabled(active);
		if (!active) {
			nudgeLabel.setVisible(false);
		}
	}

	public void showNudge() {
		if (isDaily) {
			return;
		}

		nudgeLabel.setText("※ " + model.getMe().getNickname() + " 님, 의견을 내주세요. ※");
		nudgeLabel.setVisible(true);

		if (nudgeTimer == null) {
			nudgeTimer = new Timer(7000, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					nudgeLabel.setVisible(false);
				}
			});
			nudgeTimer.setRepeats(false);
		}
		nudgeTimer.restart();
		showBuzz();
	}

	public void showBuzz() {
		if (!isShowing()) {
			return;
		}
		if (buzzTimer == null) {
			buzzTimer = new Timer(45, new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					shakeCount++;
					if (shakeCount > 8) {
						buzzTimer.stop();
						setLocation(originalLocation);
						return;
					}
					int offset = 6;
					if (shakeCount % 2 == 0) {
						offset = -6;
					}
					setLocation(originalLocation.x + offset, originalLocation.y);
				}
			});
		}
		if (!buzzTimer.isRunning()) {
			originalLocation = getLocation();
		}
		shakeCount = 0;
		buzzTimer.restart();
	}

	@Override
	public void dispose() {
		if (typingTimer != null) {
			typingTimer.stop();
		}
		if (nudgeTimer != null) {
			nudgeTimer.stop();
		}
		if (buzzTimer != null) {
			buzzTimer.stop();
		}
		if (isDaily && model != null) {
			model.getFriends().removeListDataListener(friendChanges);
		}
		super.dispose();
	}
}
