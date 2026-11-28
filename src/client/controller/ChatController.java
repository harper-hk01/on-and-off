
package client.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import client.model.ChatModel;
import client.network.ServerConnection;
import client.network.ServerListener;
import client.view.ChatView;
import client.view.MainView;
import common.Command;
import common.Message;
import dto.common.ChatRoomDTO;
import dto.common.MeetingState;
import dto.common.MessageDTO;
import dto.common.Status;
import dto.common.UserDTO;
import dto.request.BuzzRequest;
import dto.request.ChatRequest;
import dto.request.FriendAddRequest;
import dto.request.IdCheckRequest;
import dto.request.JoinRequest;
import dto.request.LoginRequest;
import dto.request.MeetingEndRequest;
import dto.request.MeetingStartRequest;
import dto.request.NoticeSetRequest;
import dto.request.ProfileUpdateRequest;
import dto.request.RoomCreateRequest;
import dto.request.RoomEnterRequest;
import dto.request.RoomOpenRequest;
import dto.request.RoomReadRequest;
import dto.request.SearchRequest;
import dto.request.StatusRequest;
import dto.request.TypingRequest;
import dto.response.BuzzRecv;
import dto.response.ErrorDTO;
import dto.response.FriendAddResult;
import dto.response.IdCheckResult;
import dto.response.JoinResult;
import dto.response.LoginResult;
import dto.response.MeetingNoti;
import dto.response.NoticeNoti;
import dto.response.NudgeNoti;
import dto.response.ProfileUpdateResult;
import dto.response.RoomCreateResult;
import dto.response.RoomInfo;
import dto.response.SearchResult;
import dto.response.StatusNoti;
import dto.response.TypingNoti;


/**
 * 서버와 통신하고, 받은 데이터를 해석해서 적절한 동작을 연결하는 전체 관리자
 *
 * - 서버에서 받은 메시지를 뷰 동작으로 연결
 * - View에서 발생한 동작을 서버에 요청으로 연결
 * - 화면 관리 (회원 및 사용자 기능, 채팅방 기능, 채팅 기능)
 */

public class ChatController implements ServerListener {

    private ServerConnection connection;
    private ChatModel model;
    private CommandHandler commandHandler;

    private MainView mainView;
    private Map<Long, ChatView> chatViews = new HashMap<>();

    public ChatController(ServerConnection connection,
                          ChatModel model,
                          CommandHandler commandHandler) {
        this.connection = connection;
        this.model = model;
        this.commandHandler = commandHandler;
        
    }
    
    /**
    * 1. 서버에서 받은 메시지를 뷰 동작으로 연결
    *     1) 메시지 왔음, 서버 연결 종료 시 호출

    */

    @Override
    public void onMessage(Message msg) {
    	SwingUtilities.invokeLater(() -> handle(msg)); //handle() 실행작업을 EDT 대기열에 등록
    }

    @Override
    public void onDisconnected() {
    	SwingUtilities.invokeLater(() -> handleDisconnected());
    }
    
    /**
    * 1. 서버에서 받은 메시지를 뷰 동작으로 연결
    *     2) 메시지 처리 - Model, View
    */

    //Command 종류에 따라 응답 분류 및 처리
    //result, noti, info 메시지들
    //Model, View 처리
    private void handle(Message msg) {
        
    	switch (msg.getType()) {

    	//성공했을 때는 화면 변화 없는 것만 팝업 표현하고, 실패는 다 팝업 표시
        // ===== 회원 =====
        case ID_CHECK_RESULT:
        	IdCheckResult idCheckResult = (IdCheckResult) msg.getData(); //(성공 or 실패 여부를 나타내는 ResultDTO 상속받는 애로 형변환)
        	if(idCheckResult.isSuccess()) {
        		mainView.showMessage("사용 가능한 아이디입니다");
        	}
        	else {
        		mainView.showMessage(idCheckResult.getMessage());//실패이유 메시지
        	}
            break;

        case JOIN_RESULT: //가입완료
        	JoinResult joinResult = (JoinResult) msg.getData();
        	if(joinResult.isSuccess()) {
        		mainView.showMessage("회원가입이 완료되었습니다");
        		mainView.showLogin();//로그인 페이지 보여주기
        	}
        	else {
        		mainView.showMessage(joinResult.getMessage());
        	}
            break;

        case LOGIN_RESULT:
        	LoginResult loginResult = (LoginResult) msg.getData();
        	if(loginResult.isSuccess()) {
        		model.setMe(loginResult.getMe()); //로그인한 사람의 UserDTO
        		mainView.showMain(); //메인화면 띄우기 -> 친구목록, 대화방이런건 다른 명령어로 서버가 보내줌
        	} else {
        		mainView.showMessage(loginResult.getMessage());
        	}
            break;

        // ===== 친구 =====
        case FRIEND_ADD_RESULT: //내가 다른이를 친구 추가 성공했다
        	FriendAddResult friendAddResult = (FriendAddResult) msg.getData();
        	if (friendAddResult.isSuccess()) {
        		model.addFriend(friendAddResult.getFriend());//model에 친구 추가 -> 화면 바로 나타남
        	} else {
        		mainView.showMessage(friendAddResult.getMessage());
        	}
        	
            break;

        case FRIEND_NOTI: //누군가 나를 친구로 추가했을 떄 내 목록에도 추가
        	UserDTO newFriend = (UserDTO) msg.getData();
        	model.addFriend(newFriend);
            break;

        case FRIEND_LIST: //로그인 직후 친구목록 전송 (접속 중인 치구 위로?? 어케함)
        	List<UserDTO> friendList = (List<UserDTO>) msg.getData();
        	model.setFriends(friendList); //친구목록 Model 갱신 -> JList는 자동으로 갱신!!
            break;

        // ===== 상태 및 프로필 =====
        case STATUS_NOTI: //친구가 상태 바꿈
        	StatusNoti statusNoti = (StatusNoti) msg.getData();
        	model.updateFriendStatus(statusNoti.getLoginId(), statusNoti.getStatus());
            break;

        case PROFILE_UPDATE_RESULT:
        	ProfileUpdateResult profileResult = (ProfileUpdateResult) msg.getData();
        	if (profileResult.isSuccess()) {
        		mainView.showMessage("프로필을 변경했습니다.");
        	} else {
        		mainView.showMessage(profileResult.getMessage());
        	}
            break;

        case PROFILE_NOTI:
        	UserDTO changedFriend = (UserDTO) msg.getData();
        	model.updateFriend(changedFriend); // ChatModel에서 DefaultListModel 사용되면서 새정보로 교체됨!
            break;

        // ===== 대화방 =====
        case ROOM_CREATE_RESULT:
        	RoomCreateResult roomCreateResult = (RoomCreateResult) msg.getData();
        	if (roomCreateResult.isSuccess()) {
        		ChatRoomDTO newRoom = roomCreateResult.getRoom();
        		if (model.getRoom(newRoom.getRoomId()) == null) {
        			model.addRoom(newRoom);
        		}
        		enterRoom(newRoom.getRoomId()); //만든 사람은 바로 입장 → ROOM_INFO로 응답 옴
        	} else {
        		mainView.showMessage(roomCreateResult.getMessage());
        	}
            break;

        case ROOM_INFO: //방입장 꾸러미. ROOM_OPEN, ROOM_ENTER 둘 다 이걸로 응답 옴
        	RoomInfo roomInfo = (RoomInfo) msg.getData();
        	ChatRoomDTO room = roomInfo.getRoom();
        	long infoRoomId = room.getRoomId();

        	// 1. Model 갱신
        	if (model.getRoom(infoRoomId) == null) {
        		model.addRoom(room); //처음 보는 방이면 목록에 추가
        	}
        	model.setMessages(infoRoomId, roomInfo.getMessages()); //이 방의 이전 대화 저장
        	model.setMeetingActive(infoRoomId, roomInfo.isMeeting()); // 회의 중인지 저장
        	model.setPinnedMessage(infoRoomId, room.getPinnedMessage()); //공지 저장
        	model.setRoomNew(infoRoomId, false);//들어왔으니까 NEW 표시 끄기

        	// 2. 채팅창 열기 (없으면 새로 만들고, 있으면 내용만 다시 채움)
        	ChatView roomView = chatViews.get(infoRoomId);
        	if (roomView == null) {
        		roomView = new ChatView(infoRoomId, this, model); //생성자가 Model 보고 화면을 채움
        		chatViews.put(infoRoomId, roomView); 
        	} else { //이미 있으면 내용만 최신으로
        		roomView.showMessages(model.getMessages(infoRoomId));
        		roomView.showNotice(room.getPinnedMessage());
        		roomView.setMeetingState(roomInfo.isMeeting());
        	}
        	roomView.setVisible(true);
        	roomView.toFront();

        	// 3. 마지막 메시지까지 읽음 처리
        	List<MessageDTO> history = model.getMessages(infoRoomId);
        	if (history.size() > 0) {
        		MessageDTO last = history.get(history.size() - 1); //가장 최근 메시지
        		markRead(infoRoomId, last.getMessageId()); //읽었다고 서버에 알림
        	}
            break;

        case ROOM_LIST: //로그인 직후 방 초기화?
        	List<ChatRoomDTO> roomList = (List<ChatRoomDTO>) msg.getData();
        	model.setRooms(roomList);
            break;

        case ROOM_INVITED: //새 방은 대화방 목록에 추가 (누가 나를 방에 초대함)
          	ChatRoomDTO invitedRoom = (ChatRoomDTO) msg.getData();
        	if (model.getRoom(invitedRoom.getRoomId()) == null) {
        		model.addRoom(invitedRoom);
        	}
            break;

        //해당하는 방에
        // ===== 채팅 =====
        case CHAT_RECV: //채팅 하나 도착
        	MessageDTO chat = (MessageDTO) msg.getData();
        	long chatRoomId = chat.getRoomId();
        	model.addMessage(chat);

        	ChatView chatView = chatViews.get(chatRoomId);
        	if (chatView != null) {
        		// 창이 열려 있음 -> 화면에 추가하고 읽음 처리
        		chatView.appendMessage(chat);
        		markRead(chatRoomId, chat.getMessageId());
        		model.setRoomNew(chatRoomId, false); //방 목록의 마지막 메시지 표시 갱신용
        	} else {
        		// 창이 닫혀 있음 -> NEW 표시, 남이 보낸 거면 팝업
        		model.setRoomNew(chatRoomId, true);
        		String myId = model.getMe().getLoginId();
        		if (!myId.equals(chat.getSenderId())) {
        			mainView.showPopup(chat);
        		}
        	}
            break;

        case TYPING_NOTI:
          	TypingNoti typingNoti = (TypingNoti) msg.getData();
        	ChatView typingView = chatViews.get(typingNoti.getRoomId());
        	if (typingView != null) {
        		typingView.showTyping(typingNoti.getLoginId());
        	}
            break;

        // ===== 일상 모드 =====
        case BUZZ_RECV: //-> CommandHandler에 보내기
         	BuzzRecv buzzRecv = (BuzzRecv) msg.getData();
        	ChatView buzzView = chatViews.get(buzzRecv.getRoomId());
        	if (buzzView != null) {
        		commandHandler.handleCommand(msg, buzzView);
        	}
            break;

        // ===== 업무 모드 =====
        case NOTICE_NOTI:
          	NoticeNoti noticeNoti = (NoticeNoti) msg.getData();
        	model.setPinnedMessage(noticeNoti.getRoomId(), noticeNoti.getPinnedMessage()); //null이면 공지 해제
        	ChatView noticeView = chatViews.get(noticeNoti.getRoomId());
        	if (noticeView != null) {
        		noticeView.showNotice(noticeNoti.getPinnedMessage());
        	}
            break;

        case MEETING_NOTI: //회의 시작/종료 + 버튼 전환디ㅣ
        	MeetingNoti meetingNoti = (MeetingNoti) msg.getData();
        	boolean active = false;
        	if (meetingNoti.getState() == MeetingState.START) {
        		active = true;
        	}
        	model.setMeetingActive(meetingNoti.getRoomId(), active);
        	ChatView meetingView = chatViews.get(meetingNoti.getRoomId());
        	if (meetingView != null) {
        		meetingView.setMeetingState(active);
        	}
            break;

        case NUDGE_NOTI: //-> CommandHandler에 보내기
        	NudgeNoti nudgeNoti = (NudgeNoti) msg.getData();
        	ChatView nudgeView = chatViews.get(nudgeNoti.getRoomId());//roomId에 맞는 해당 ChatView 불러오기
        	if (nudgeView != null) {
        		commandHandler.handleCommand(msg, nudgeView); //CommandHandler로 넘김
        	}
            break;

        // ===== 공통 =====
        case ERROR:
        	ErrorDTO error = (ErrorDTO) msg.getData();
        	mainView.showMessage(error.getMessage());
        	
            break;
    	}
    }

    
    /**
    * 1. 서버에서 받은 메시지를 뷰 동작으로 연결
    *     3)연결 종료 후 화면 및 데이터 정리
    */
    private void handleDisconnected() {
        // 1. 열려 있는 모든 채팅 화면 닫기
        for (ChatView view : chatViews.values()) {
            view.dispose();
        }

        // 2. 채팅 화면 목록 초기화
        chatViews.clear();

        // 3. Model 초기화
        model.clear();

        // 4. MainView에 연결 종료 처리 요청 (서버에 다시 연결하는 기능이 없어서 그냥 프로그램 끄는 쪽으로, 로그인화면 돌아가기X)
        mainView.showMessage("서버와 연결이 끊어졌습니다. 프로그램을 종료합니다.");
        System.exit(0);
    }
    
    
    /**
    * 2. View에서 발생한 동작을 서버에 요청으로 연결
    *     1) 회원 및 사용자 기능
    *   아래 모두 계속 같은 구조임!
    */

    public void checkId(String loginId) {

        // 1. 서버에 보낼 요청 데이터 구성
    	IdCheckRequest request = new IdCheckRequest(loginId);

        // 2. ServerConnection을 통해 서버에 요청 전송
        connection.send(Command.ID_CHECK, request);
    }

    public void join(String loginId, String password, String nickname) {
        JoinRequest data = new JoinRequest(loginId, password, nickname);
        connection.send(Command.JOIN, data);
    }

    public void login(String loginId, String password) {
        LoginRequest data = new LoginRequest(loginId, password);
        connection.send(Command.LOGIN, data);
    }

    public void logout() {
        connection.send(Command.LOGOUT, null); //로그아웃만 전달
        //로그아웃 화면 나가기! (view에서 이 함수를 부름)
        for (ChatView view : chatViews.values()) {
            view.dispose();
        }
        chatViews.clear();
        model.clear();
        mainView.showLogin();
    }

    public void addFriend(String loginId) {
        FriendAddRequest data = new FriendAddRequest(loginId);
        connection.send(Command.FRIEND_ADD, data);
    }

    public void changeStatus(Status status) { //상태를 받음
        StatusRequest data = new StatusRequest(status);
        connection.send(Command.STATUS, data);
        //새로운 상태로 갱신!
        model.getMe().setStatus(status);
    }

    public void updateProfile(String nickname, String statusMessage) {
        ProfileUpdateRequest data = new ProfileUpdateRequest(nickname, statusMessage);
        connection.send(Command.PROFILE_UPDATE, data);
        //새로운 상태로 갱신!
        model.getMe().setNickname(nickname); 
        model.getMe().setStatusMessage(statusMessage);
    }
    
    
    /**
    * 2. View에서 발생한 동작을 서버에 요청으로 연결
    *     2) 채팅방 기능
    */

    public void openRoom(String friendLoginId) {
        RoomOpenRequest data = new RoomOpenRequest(friendLoginId);
        connection.send(Command.ROOM_OPEN, data);
    }

    public void enterRoom(long roomId) {
        RoomEnterRequest data = new RoomEnterRequest(roomId);
        connection.send(Command.ROOM_ENTER, data);
    }

    public void createRoom(String roomName, List<String> memberIds) {
        RoomCreateRequest data = new RoomCreateRequest(roomName, memberIds);
        connection.send(Command.ROOM_CREATE, data);
    }

    public void markRead(long roomId, long lastReadMessageId) {
        RoomReadRequest data = new RoomReadRequest(roomId, lastReadMessageId);
        connection.send(Command.ROOM_READ, data);
    }


    /**
    * 2. View에서 발생한 동작을 서버에 요청으로 연결
    *     3) 채팅 기능
    */
    public void sendMessage(long roomId, String text) {
        ChatRequest data = new ChatRequest(roomId, text);
        connection.send(Command.CHAT, data);
    }

    public void typing(long roomId) {
        TypingRequest data = new TypingRequest(roomId);
        connection.send(Command.TYPING, data);
    }

    public void buzz(long roomId) {
        BuzzRequest data = new BuzzRequest(roomId);
        connection.send(Command.BUZZ, data);
    }

    public void setNotice(long roomId, Long messageId) {
        NoticeSetRequest data = new NoticeSetRequest(roomId, messageId);
        connection.send(Command.NOTICE_SET, data);
    }

    // 회의 중이면 종료 요청, 아니면 시작 요청 (Model은 MEETING_NOTI가 왔을 때 바꿈)
    public void toggleMeeting(long roomId) {
    	if (model.isMeetingActive(roomId)) {
    		MeetingEndRequest data = new MeetingEndRequest(roomId);
    		connection.send(Command.MEETING_END, data);
    	} else {
    		MeetingStartRequest data = new MeetingStartRequest(roomId);
    		connection.send(Command.MEETING_START, data);
    	}
    }

    /**
    * 3. 화면 관리
    */
    public void setMainView(MainView view) {
    	this.mainView = view;
    }

    public void closeChatView(long roomId) {
        ChatView view = chatViews.remove(roomId);

        if (view != null) {
            view.dispose(); //swing창 닫기
        }
    }

}
