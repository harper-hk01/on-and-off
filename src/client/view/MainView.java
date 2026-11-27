package client.view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.JTextComponent;

import client.controller.ChatController;
import client.model.ChatModel;
import dto.common.ChatRoomDTO;
import dto.common.MessageDTO;
import dto.common.RoomType;
import dto.common.Status;
import dto.common.UserDTO;

public class MainView extends JFrame implements ActionListener {

	// 컨트롤러, 모델
	private ChatController controller;
	private ChatModel model;

	// 한 frame에서 화면 전환하기 위해 카드 레이아웃 사용
	// pages에 여러 화면 등록하고, 등록할 때 붙인 이름으로 화면 전환
	private final CardLayout cards = new CardLayout();
	private final JPanel pages = new JPanel(cards);

	// 로그인 후 메인화면에서 창 아래쪽에 친구/일상/업무 탭 표시
	private final JTabbedPane tabs = new JTabbedPane(JTabbedPane.BOTTOM);

	// 로그인 화면 입력값(기존 계정 아이디/비번)
	private final JTextField loginId = new JTextField();
	private final JPasswordField loginPassword = new JPasswordField();

	// 회원가입 화면 입력값(새 계정 아이디/비번)
	private final JTextField joinId = new JTextField();
	private final JPasswordField joinPassword = new JPasswordField();

	// 닉네임 설정화면 입력값
	private final JTextField nickname = new JTextField();

	// 메인화면 상단 메뉴 - 아이디 정보
	private JMenuItem accountID;

	// 메인 화면 친구탭의 내 프로필
	private final JLabel myInfo = new JLabel("내 프로필");
	private final JLabel myStatus = new JLabel("상태 메시지");
	// 친구 수
	private final JLabel friendCount = new JLabel("친구 0");

	// 친구 추가 -friendId / 1:1대화 시작 - chatFriend
	private final JTextField friendId = new JTextField();
	private final JTextField chatFriendId = new JTextField();

	// 각 탭에서 사용자가 클릭하는 목록들
	private final JList<UserDTO> friends = new JList<>();
	private final JList<ChatRoomDTO> dailyList = new JList<>();
	private final JList<ChatRoomDTO> workList = new JList<>();

	// 전체 채팅방 중 각 탭에 표시할 방을 나누어 저장하여 표시
	private final DefaultListModel<ChatRoomDTO> dailyRooms = new DefaultListModel<>();
	private final DefaultListModel<ChatRoomDTO> workRooms = new DefaultListModel<>();

	// 프로필 수정 화면 입력값들
	private final JTextField profileName = new JTextField();
	private final JTextArea profileText = new JTextArea(3, 15);
	// 라디오버튼, 버튼 그룹화로 상태 1개만 선택 가능하도록
	private final JRadioButton online = new JRadioButton("온라인");
	private final JRadioButton away = new JRadioButton("자리 비움");
	private final ButtonGroup statusGroup = new ButtonGroup();

	// 업무방 생성에서 방이름/초대할 사람 아이디 입력값
	private final JTextField groupName = new JTextField();
	private final JTextField memberId = new JTextField();
	// 추가한 아이디 members에 저장하고 memberlist에 표시
	private final DefaultListModel<String> members = new DefaultListModel<>();
	private final JList<String> memberList = new JList<>();

	// 기본 화면, 버튼 등 배치
	private void initializeUI() {
		// 기본 설정
		setTitle("온앤오프");
		setSize(390, 570);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// 창 내용에 pages 사용
		setContentPane(pages);

		// 1. 로그인 페이지
		JPanel loginPage = new JPanel(new BorderLayout());
		pages.add(loginPage, "login");

		// 입력 폼
		JPanel loginForm = new JPanel(new GridLayout(0, 1, 4, 6));
		loginPage.add(loginForm, BorderLayout.NORTH);
		loginForm.add(new JLabel("온앤오프"));
		loginForm.add(new JLabel("아이디"));
		loginForm.add(loginId);
		loginForm.add(new JLabel("비밀번호"));
		loginForm.add(loginPassword);

		// 로그인 버튼 등록
		JButton loginButton = new JButton("로그인");
		loginForm.add(loginButton);

		// 로그인 버튼 클릭 or 비밀번호 입력 후 enter -> login 동작으로
		// 액션은 actionPerformed()에서 실행
		loginButton.setActionCommand("login");
		loginButton.addActionListener(this);
		loginPassword.setActionCommand("login");
		loginPassword.addActionListener(this);

		// 회원가입 페이지로 이동
		JButton joinLink = new JButton("회원가입");
		loginForm.add(joinLink);
		joinLink.setActionCommand("showJoin");
		joinLink.addActionListener(this);

		// 2. 회원가입 페이지
		JPanel joinPage = new JPanel(new BorderLayout());
		pages.add(joinPage, "join");

		JPanel joinForm = new JPanel(new GridLayout(0, 1, 4, 6));
		joinPage.add(joinForm, BorderLayout.NORTH);
		joinForm.add(new JLabel("회원가입"));
		joinForm.add(new JLabel("아이디"));

		// 아이디 입력칸/중복 확인 버튼을 같은 줄에 배치
		JPanel checkRow = new JPanel(new BorderLayout());
		joinForm.add(checkRow);
		checkRow.add(joinId, BorderLayout.CENTER);

		// 중복 확인 버튼 등록 + 동작
		JButton checkButton = new JButton("중복확인");
		checkRow.add(checkButton, BorderLayout.EAST);
		checkButton.setActionCommand("checkId");
		checkButton.addActionListener(this);

		// 비밀번호 입력칸
		joinForm.add(new JLabel("비밀번호"));
		joinForm.add(joinPassword);

		// 아이디/비밀번호 입력 여부 확인하고 닉네임 설정 단계로 이동
		JButton nextButton = new JButton("닉네임 설정으로");
		joinForm.add(nextButton);
		nextButton.setActionCommand("showNickname");
		nextButton.addActionListener(this);

		// 로그인 페이지로 돌아가기
		JButton backToLogin = new JButton("로그인으로");
		joinForm.add(backToLogin);
		backToLogin.setActionCommand("showLogin");
		backToLogin.addActionListener(this);

		// 3. 닉네임 설정 페이지
		JPanel nicknamePage = new JPanel(new BorderLayout());
		pages.add(nicknamePage, "nickname");

		JPanel nicknameForm = new JPanel(new GridLayout(0, 1, 4, 6));
		nicknamePage.add(nicknameForm, BorderLayout.NORTH);
		nicknameForm.add(new JLabel("사용하실 닉네임을 입력해 주세요."));
		nicknameForm.add(nickname);

		// 가입완료 버튼 or 입력 후 enter로 회원가입
		JButton joinButton = new JButton("가입 완료");
		nicknameForm.add(joinButton);
		joinButton.setActionCommand("join");
		joinButton.addActionListener(this);
		nickname.setActionCommand("join");
		nickname.addActionListener(this);

		// 돌아가기
		JButton backToJoin = new JButton("이전");
		nicknameForm.add(backToJoin);
		backToJoin.setActionCommand("showJoin");
		backToJoin.addActionListener(this);

		// 4. 메인 화면
		JPanel homePage = new JPanel(new BorderLayout());
		pages.add(homePage, "main");
		homePage.add(tabs, BorderLayout.CENTER);

		// 1) 친구 탭
		JPanel friendTab = new JPanel(new BorderLayout());
		tabs.addTab("친구", friendTab);

		// 친구 목록 상단에 내 프로필 + 상태메시지
		JPanel friendHeader = new JPanel(new GridLayout(0, 1, 2, 4));
		friendTab.add(friendHeader, BorderLayout.NORTH);
		friendHeader.add(myInfo);
		friendHeader.add(myStatus);

		// 프로필 수정 버튼 누르면 수정 페이지로
		JButton profileButton = new JButton("프로필 수정");
		friendHeader.add(profileButton);
		profileButton.setActionCommand("editProfile");
		profileButton.addActionListener(this);

		// 친구 아이디 입력 + 버튼
		JPanel friendRow = new JPanel(new BorderLayout());
		friendHeader.add(friendRow);
		friendId.setToolTipText("추가할 친구 아이디");
		friendRow.add(friendId, BorderLayout.CENTER);

		JButton addFriendButton = new JButton("친구 추가");
		friendRow.add(addFriendButton, BorderLayout.EAST);
		addFriendButton.setActionCommand("addFriend");
		addFriendButton.addActionListener(this);
		friendId.setActionCommand("addFriend");
		friendId.addActionListener(this);

		// 친구 수
		friendHeader.add(friendCount);

		// 스크롤에 친구 목록 표시
		JScrollPane friendScroll = new JScrollPane();
		friendTab.add(friendScroll, BorderLayout.CENTER);
		friendScroll.setViewportView(friends);

		// 2) 메인 화면 - 일상 탭
		JPanel dailyTab = new JPanel(new BorderLayout());
		tabs.addTab("일상", dailyTab);

		// 대화할 친구 아이디 일상 탭 상단에서 입력
		JPanel directRow = new JPanel(new BorderLayout());
		dailyTab.add(directRow, BorderLayout.NORTH);
		chatFriendId.setToolTipText("대화할 친구 아이디");
		directRow.add(chatFriendId, BorderLayout.CENTER);

		// 입력한 아이디로 대화방 열기 요청
		JButton directButton = new JButton("대화하기");
		directRow.add(directButton, BorderLayout.EAST);
		directButton.setActionCommand("openDirect");
		directButton.addActionListener(this);
		chatFriendId.setActionCommand("openDirect");
		chatFriendId.addActionListener(this);

		// 방 목록 스크롤 팬으로 표시
		JScrollPane dailyScroll = new JScrollPane();
		dailyTab.add(dailyScroll, BorderLayout.CENTER);
		dailyScroll.setViewportView(dailyList);

		// 3) 업무 탭
		JPanel workTab = new JPanel(new BorderLayout());
		tabs.addTab("업무", workTab);

		JScrollPane workScroll = new JScrollPane();
		workTab.add(workScroll, BorderLayout.CENTER);
		workScroll.setViewportView(workList);

		// 업무방 만들기 페이지로 이동
		JButton newRoomButton = new JButton("+ 업무방 만들기");
		workTab.add(newRoomButton, BorderLayout.SOUTH);
		newRoomButton.setActionCommand("showGroup");
		newRoomButton.addActionListener(this);

		// 4) 메뉴 바 - homepage에 배치해 세 탭에서 함께 사용
		JMenuBar menuBar = new JMenuBar();
		homePage.add(menuBar, BorderLayout.NORTH);

		JMenu menu_account = new JMenu("내 계정");
		menuBar.add(menu_account);

		accountID = new JMenuItem("내 아이디");
		menu_account.add(accountID);

		JMenu menu_logout = new JMenu("로그아웃");
		menuBar.add(menu_logout);

		JMenuItem logout_Item = new JMenuItem("로그아웃");
		menu_logout.add(logout_Item);
		logout_Item.setText("로그아웃");
		logout_Item.setActionCommand("logout");
		logout_Item.addActionListener(this);

		// 5. 프로필 수정
		JPanel profilePage = new JPanel(new BorderLayout());
		pages.add(profilePage, "profile");

		JPanel profileForm = new JPanel(new GridLayout(0, 1, 4, 6));
		profilePage.add(profileForm, BorderLayout.NORTH);
		profileForm.add(new JLabel("닉네임"));
		profileForm.add(profileName);
		profileForm.add(new JLabel("상태 메시지"));

		JScrollPane profileScroll = new JScrollPane();
		profileForm.add(profileScroll);
		profileScroll.setViewportView(profileText);

		// 동시선택 방지로 Group화
		JPanel statusRow = new JPanel();
		profileForm.add(statusRow);
		statusRow.add(online);
		statusRow.add(away);
		statusGroup.add(online);
		statusGroup.add(away);

		// 수정한 프로필 컨트롤러에 전달
		JButton saveButton = new JButton("수정하기");
		profileForm.add(saveButton);
		saveButton.setActionCommand("saveProfile");
		saveButton.addActionListener(this);

		// 수정요청 없이 돌아가기
		JButton profileBack = new JButton("돌아가기");
		profileForm.add(profileBack);
		profileBack.setActionCommand("showMain");
		profileBack.addActionListener(this);

		// 6. 업무방 만들기 페이지
		JPanel groupPage = new JPanel(new BorderLayout());
		pages.add(groupPage, "group");

		JPanel groupForm = new JPanel(new GridLayout(0, 1, 4, 6));
		groupPage.add(groupForm, BorderLayout.NORTH);
		groupForm.add(new JLabel("방 이름"));
		groupForm.add(groupName);
		groupForm.add(new JLabel("멤버 아이디 입력 후 Enter"));
		groupForm.add(memberId);
		memberId.setActionCommand("addMember");
		memberId.addActionListener(this);

		JScrollPane memberScroll = new JScrollPane();
		groupPage.add(memberScroll, BorderLayout.CENTER);
		memberScroll.setViewportView(memberList);

		JPanel groupButtons = new JPanel();
		groupPage.add(groupButtons, BorderLayout.SOUTH);

		JButton removeButton = new JButton("선택 멤버 삭제");
		groupButtons.add(removeButton);
		removeButton.setActionCommand("removeMember");
		removeButton.addActionListener(this);

		JButton createButton = new JButton("만들기");
		groupButtons.add(createButton);
		createButton.setActionCommand("createRoom");
		createButton.addActionListener(this);

		JButton groupBack = new JButton("돌아가기");
		groupButtons.add(groupBack);
		groupBack.setActionCommand("showMain");
		groupBack.addActionListener(this);
	}

	// 생성자. 인자로 컨트롤러 / 모델을 받음
	public MainView(ChatController controller, ChatModel model) {
		// 기본 화면, 버튼 생성
		initializeUI();

		// 전달받은 컨트롤러 / 모델 객체를 필드에 보관해 갱신과 이벤트 처리에 사용
		this.controller = controller;
		this.model = model;

		// 친구 목록은 챗 모델에 직접 연결
		friends.setModel(model.getFriends());

		// 화면 목록에 연결
		dailyList.setModel(dailyRooms);
		workList.setModel(workRooms);
		memberList.setModel(members);

		// 렌더러(리스트 표시하는 애) 각 리스트에 연결
		ListRenderer renderer = new ListRenderer();
		friends.setCellRenderer(renderer);
		dailyList.setCellRenderer(renderer);
		workList.setCellRenderer(renderer);

		// 클릭 숫자 설정, 친구는 더블클릭, 방은 1번 클릭
		installListClick(friends, 2);
		installListClick(dailyList, 1);
		installListClick(workList, 1);

		// 모델 목록이 바뀌면(ListDataListener) 목록 갱신
		model.getFriends().addListDataListener(listChanges);
		model.getRooms().addListDataListener(listChanges);
		refreshLists();

	}

	private void clearFields(List<JTextComponent> fields) {
		for (JTextComponent field : fields) {
			field.setText("");
		}
	}

	// 이벤트 처리 로직...
	// 아예 다른 클래스로 분리해야 할지? 더 깔끔하게 할 방법이 뭐가 있을까요
	@Override
	public void actionPerformed(ActionEvent event) {
		switch (event.getActionCommand()) {

		// 로그인 페이지 표시
		case "showLogin":
			showLogin();
			break;

		// 회원가입 페이지 표시
		case "showJoin":
			showJoin();
			break;

		// 메인화면 페이지 표시
		case "showMain":
			showMain();
			break;

		// 로그인 상황
		case "login":
			// 로그인 입력값이 비어있는지 검사
			if (loginId.getText().isBlank() || loginPassword.getPassword().length == 0) {
				showMessage("아이디와 비밀번호를 입력해 주세요.");
				return;
			}
			// 컨트롤러에 로그인 요청
			controller.login(loginId.getText().trim(), new String(loginPassword.getPassword()));
			clearFields(List.of(loginId, loginPassword));
			break;

		// 회원가입 시 아이디 중복확인 요청
		case "checkId":

			// 비어있는지 검사
			if (joinId.getText().isBlank()) {
				showMessage("아이디를 입력해 주세요.");
				return;
			}

			// 값이 있으면 컨트롤러에 검사 요청
			controller.checkId(joinId.getText().trim());
			break;

		// 닉네임
		case "showNickname":
			// 회원가입 입력값이 비어있는지 검사
			if (joinId.getText().isBlank() || joinPassword.getPassword().length == 0) {
				showMessage("아이디와 비밀번호를 입력해 주세요.");
				return;
			}
			// 검사 통과 시 닉네임 화면 열기
			cards.show(pages, "nickname");
			nickname.requestFocusInWindow();
			break;

		// 회원가입 - 아이디/비번/닉네임 보내서 가입요청
		case "join":
			if (nickname.getText().isBlank()) {
				showMessage("닉네임을 입력해 주세요.");
				return;
			}

			// .trim()->앞뒤 공백 제거하여 컨트롤러에 전달
			controller.join(joinId.getText().trim(), new String(joinPassword.getPassword()), nickname.getText().trim());
			break;

		// 프로필 수정 화면
		case "editProfile":
			editProfile();
			break;

		// 수정한 프로필로 변경 요청
		case "saveProfile":
			saveProfile();
			break;

		// 친구 추가 요청
		case "addFriend":
			if (friendId.getText().isBlank()) {
				showMessage("친구 아이디를 입력해 주세요.");
				return;
			}
			controller.addFriend(friendId.getText().trim());
			clearFields(List.of(friendId)); // 초기화
			break;

		// 일상 탭에 입력한 아이디로 1:1 대화방 열기 요청
		case "openDirect":
			if (chatFriendId.getText().isBlank()) {
				showMessage("친구 아이디를 입력해 주세요.");
				return;
			}
			controller.openRoom(chatFriendId.getText().trim());
			clearFields(List.of(chatFriendId));
			break;

		// 새 업무방 만들기 할때 초기화
		case "showGroup":
			members.clear();
			clearFields(List.of(memberId, groupName));
			cards.show(pages, "group");
			break;

		// 초대 목록 멤버 추가
		case "addMember":
			addMember();
			clearFields(List.of(memberId));
			break;

		// 초대 목록 멤버 삭제
		case "removeMember":
			int index = memberList.getSelectedIndex();
			if (index >= 0) {
				members.remove(index);
			}
			break;

		// 방 생성
		case "createRoom":
			createRoom();
			break;

		// 로그아웃
		case "logout":
			controller.logout();
			break;
		}
	}

	// 프로필 수정 화면을 열 때 현재 저장된 정보를 입력칸에 채우는 메소드
	private void editProfile() {
		UserDTO me = model.getMe();

		// 사용자 정보 없을 때 예외처리
		if (me == null) {
			return;
		}

		profileName.setText(me.getNickname());
		profileText.setText(me.getStatusMessage());
		statusGroup.clearSelection();
		online.setSelected(me.getStatus() == Status.ONLINE);
		away.setSelected(me.getStatus() == Status.AWAY);
		cards.show(pages, "profile");
	}

	// 수정한 프로필 정보 / 접속 상태를 컨트롤러에 전달하는 메소드
	private void saveProfile() {
		if (profileName.getText().isBlank()) {
			showMessage("닉네임을 입력해 주세요.");
			return;
		}
		controller.updateProfile(profileName.getText().trim(), profileText.getText().trim());
		if (online.isSelected()) {
			controller.changeStatus(Status.ONLINE);
		} else if (away.isSelected()) {
			controller.changeStatus(Status.AWAY);
		}
		showMain();
	}

	// 업무방 생성 화면에서 입력한 아이디를 초대 목록에 추가
	private void addMember() {
		String id = memberId.getText().trim();

		if (id.isEmpty()) {
			return;
		}
		if (model.getMe() != null && id.equals(model.getMe().getLoginId())) {
			showMessage("본인은 자동으로 참여합니다.");
			return;
		}
		if (members.contains(id)) {
			showMessage("이미 추가한 친구입니다.");
			return;
		}

		for (int i = 0; i < model.getFriends().size(); i++) {
			if (id.equals(model.getFriends().get(i).getLoginId())) {
				members.addElement(id);
				return;
			}
		}

		showMessage("친구 목록에 없는 아이디입니다.");
		return;
	}

	// 업무방 생성 요청
	private void createRoom() {
		if (!memberId.getText().isBlank()) {
			showMessage("Enter로 멤버를 먼저 추가해 주세요.");
			return;
		}
		if (groupName.getText().isBlank() || members.isEmpty()) {
			showMessage("방 이름과 초대할 멤버를 입력해 주세요.");
			return;
		}

		ArrayList<String> ids = new ArrayList<>();
		for (int i = 0; i < members.size(); i++) {
			ids.add(members.get(i));
		}
		controller.createRoom(groupName.getText().trim(), ids);
		showMain();
		tabs.setSelectedIndex(2);
	}

	// 클릭 동작 등록
	private void installListClick(JList<?> list, int clickCount) {
		// 한 번에 한 개만 선택
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// 마우스 리스너
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (!SwingUtilities.isLeftMouseButton(event) || event.getClickCount() != clickCount) {
					return;
				}
				int index = list.locationToIndex(event.getPoint());
				if (index < 0 || !list.getCellBounds(index, index).contains(event.getPoint())) {
					return;
				}
				Object value = list.getModel().getElementAt(index);
				if (value instanceof UserDTO user) {
					controller.openRoom(user.getLoginId());
				} else if (value instanceof ChatRoomDTO room) {
					controller.enterRoom(room.getRoomId());
				}
			}
		});
	}

	// 데이터 리스너(목록 갱신용)
	private final ListDataListener listChanges = new ListDataListener() {
		public void intervalAdded(ListDataEvent e) {
			refreshLists();
		}

		public void intervalRemoved(ListDataEvent e) {
			refreshLists();
		}

		public void contentsChanged(ListDataEvent e) {
			refreshLists();
		}
	};

	// 최신 데이터 목록을 메인화면에 반영
	private void refreshLists() {
		friendCount.setText("친구 " + model.getFriends().size());
		dailyRooms.clear();
		workRooms.clear();
		for (int i = 0; i < model.getRooms().size(); i++) {
			ChatRoomDTO room = model.getRooms().get(i);
			if (room.getRoomType() == RoomType.WORK) {
				workRooms.addElement(room);
			} else {
				dailyRooms.addElement(room);
			}
		}
	}

	// 리스트 렌더러 - JList가 한 행을 그릴 때 친구 또는 채팅방의 어떤 정보를 표시할지 정한다.
	private class ListRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected,
				boolean focus) {
			super.getListCellRendererComponent(list, "", index, selected, focus);

			if (value instanceof UserDTO user) {
				String status = "[" + user.getStatus() + "]";
				String message = user.getStatusMessage();

				// 친구 목록: 상태 / 이름 / 상태메시지
				setText(String.format("%-12s %-16s %s", status, user.getNickname(), message == null ? "" : message));

			} else if (value instanceof ChatRoomDTO room) {
				UserDTO other = peer(room, model);
				String name = roomTitle(room, other);

				java.util.List<MessageDTO> history = model.getMessages(room.getRoomId());
				String preview = history.isEmpty() ? "" : history.get(history.size() - 1).getContent();
				if (room.isHasNew()) {
					preview = "【NEW】 " + preview;
				}

				if (room.getRoomType() == RoomType.DAILY) {
					// 일상방 목록: 상태 / 상대 이름 / 메시지 미리보기
					String status = other == null ? "" : "[" + other.getStatus() + "]";
					setText(String.format("%-12s %-16s %s", status, name, preview));
				} else {
					// 업무방 목록: 방 이름 / 멤버 수 / 메시지 미리보기
					String count = room.getMembers() == null ? "" : "멤버 " + room.getMembers().size() + "명";
					setText(String.format("%-20s %-10s %s", name, count, preview));
				}
			}
			return this;
		}

	}

	// 1:1 채팅방에서 나와 대화하는 상대를 찾아서 UserDTO로 반환
	static UserDTO peer(ChatRoomDTO room, ChatModel model) {
		if (room.getRoomType() != RoomType.DAILY || model.getMe() == null || room.getMembers() == null) {
			return null;
		}
		for (UserDTO member : room.getMembers()) {
			if (!member.getLoginId().equals(model.getMe().getLoginId())) {
				for (int i = 0; i < model.getFriends().size(); i++) {
					UserDTO friend = model.getFriends().get(i);
					if (friend.getLoginId().equals(member.getLoginId())) {
						return friend;
					}
				}
				return member;
			}
		}
		return null;
	}

	// 방 이름 설정
	static String roomTitle(ChatRoomDTO room, UserDTO other) {
		if (other != null) {
			return other.getNickname();
		}
		if (room.getRoomName() != null && !room.getRoomName().isBlank()) {
			return room.getRoomName();
		}
		return "대화방 " + room.getRoomId();
	}

	public void showLogin() {
		clearFields(List.of(loginId, loginPassword, joinId, joinPassword, nickname, friendId, chatFriendId));
		tabs.setSelectedIndex(0);

		cards.show(pages, "login");
		setVisible(true);
	}

	public void showJoin() {
		cards.show(pages, "join");
	}

	public void showMain() {
		refreshMyInfo();
		cards.show(pages, "main");
	}

	public void refreshMyInfo() {
		UserDTO me = model.getMe();
		if (me == null) {
			myInfo.setText("");
			myStatus.setText("");
			return;
		}
		myInfo.setText("[" + me.getStatus() + "] " + me.getNickname());
		myStatus.setText(me.getStatusMessage());
		accountID.setText(me.getLoginId());
	}

	public void showMessage(String text) {
		String title = "시스템 알림";
		JOptionPane.showMessageDialog(this, text, title, JOptionPane.INFORMATION_MESSAGE);
	}

	public void showPopup(MessageDTO msg) {
		ChatRoomDTO room = model.getRoom(msg.getRoomId());
		String title = "새 채팅";

		if (room != null) {
			title = room.getRoomType() == RoomType.DAILY ? "일상 채팅 알림" : "업무 채팅 알림 · " + roomTitle(room, null);
		}

		JOptionPane.showMessageDialog(this, msg.getSenderNickname() + " : " + msg.getContent(), title,
				JOptionPane.PLAIN_MESSAGE);
	}

	@Override
	public void dispose() {
		if (model != null) {
			model.getFriends().removeListDataListener(listChanges);
			model.getRooms().removeListDataListener(listChanges);
		}
		super.dispose();
	}
}
