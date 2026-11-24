
package client.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import client.model.ChatModel;
import client.network.ServerConnection;
import client.network.ServerListener;
import client.view.ChatView;
import client.view.MainView; //이건 아직 안만들어져 있음. 근데 필요
import common.Command;
import common.Message;
import dto.common.ChatRoomDTO;
import dto.common.Status;
import dto.common.UserDTO;
import dto.request.BuzzRequest;
import dto.request.ChatRequest;
import dto.request.FriendAddRequest;
import dto.request.IdCheckRequest;
import dto.request.JoinRequest;
import dto.request.LoginRequest;
import dto.request.NoticeSetRequest;
import dto.request.ProfileUpdateRequest;
import dto.request.RoomCreateRequest;
import dto.request.RoomEnterRequest;
import dto.request.RoomOpenRequest;
import dto.request.RoomReadRequest;
import dto.request.SearchRequest;
import dto.request.StatusRequest;
import dto.request.TypingRequest;
import dto.response.ErrorDTO;
import dto.response.FriendAddResult;
import dto.response.IdCheckResult;
import dto.response.JoinResult;
import dto.response.LoginResult;
import dto.response.NudgeNoti;
import dto.response.ProfileUpdateResult;
import dto.response.RoomCreateResult;
import dto.response.SearchResult;
import dto.response.StatusNoti;


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
        
        //ServerConnection에 ChatController를 리스너로 등록
        connection.setListener(this);
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
    	
    	//오늘 테스트를 위한 임시 출력문
        System.out.println("서버 응답: " + msg.getType());
        System.out.println("응답 데이터: " + msg.getData());
        
        //model, view만들어져야 가능
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
        		mainView.showMain(); //메인화면 띄우기 -> 친구목록, 대화방이런건 다른 명령어로???
        	} else {
        		mainView.showMessage(loginResult.getMessage());
        	}
            break;

        // ===== 친구 =====
        case SEARCH_RESULT:
        	SearchResult searchResult = (SearchResult) msg.getData();
        	if(searchResult.isSuccess()) {
        		UserDTO found = searchResult.getUser();
        		//검색 결과 어디에 표현함??
        	}
        	else {
        		mainView.showMessage(searchResult.getMessage());
        	}
        	
            break;

        case FRIEND_ADD_RESULT: //내가 다른이를 친구 추가 성공했다
        	FriendAddResult friendAddResult = (FriendAddResult) msg.getData();
        	if (friendAddResult.isSuccess()) {
        		model.addFriend(friendAddResult.getFriend());//model에 친구 추가
        		//메인 화면 친구 목록에 친구 추가된 모습? 아니면 표시?
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
        case STATUS_NOTI:
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

        case ROOM_INFO: //ROOM_OPEN, ROOM_ENTER 둘 다 이걸로 응답 옴
        	
            break;

        case ROOM_LIST: //로그인 직후 방 초기화?
        	List<ChatRoomDTO> roomList = (List<ChatRoomDTO>) msg.getData();
        	model.setRooms(roomList);
            break;

        case ROOM_INVITED: //새 방은 대화방 목록에 추가 (누가 나를 방에 초대함)
          	ChatRoomDTO invitedRoom = (ChatRoomDTO) msg.getData();
        	if (model.getRoom(invitedRoom.getRoomId()) == null) { //if문 넣어야 할까?
        		model.addRoom(invitedRoom);
        	}
            break;

        //해당하는 방에
        // ===== 채팅 =====
        case CHAT_RECV:
        	
            break;

        case TYPING_NOTI:
        	
            break;

        // ===== 일상 모드 =====
        case BUZZ_RECV: //-> CommandHandler에 보내기
        	
            break;

        // ===== 업무 모드 =====
        case NOTICE_NOTI:
        	
            break;

        case MEETING_NOTI: //회의 시작
        	
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

        // 2. 채팅 화면 목록 초기화???해야하려나
        chatViews.clear();

        // 3. Model 초기화
        // model의 초기화 메서드 확인 후 작성

        // 4. MainView에 연결 종료 처리 요청
        // MainView의 메서드 확인 후 작성
    }
    
    
    /**
    * 2. View에서 발생한 동작을 서버에 요청으로 연결
    *     1) 회원 및 사용자 기능
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
    }

    public void searchUser(String loginId) {
        SearchRequest data = new SearchRequest(loginId);
        connection.send(Command.SEARCH, data);
    }

    public void addFriend(String loginId) {
        FriendAddRequest data = new FriendAddRequest(loginId);
        connection.send(Command.FRIEND_ADD, data);
    }

    public void changeStatus(Status status) { //상태를 받음
        StatusRequest data = new StatusRequest(status);
        connection.send(Command.STATUS, data);
    }

    public void updateProfile(String nickname, String statusMessage) {
        ProfileUpdateRequest data = new ProfileUpdateRequest(nickname, statusMessage);
        connection.send(Command.PROFILE_UPDATE, data);
    }
    
    
    /**
    * 2. View에서 발생한 동작을 서버에 요청으로 연결
    *     2) 채팅방 기능
    * - 

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

    //이거 메소드 startMeeting, endMeeting 두 개로 나눠도 괜찮을지
//    public void toggleMeeting(long roomId) {
//        MeetingRequest data = new MeetingRequest(roomId);
//
//        if (isMeeting) {
//            connection.send(Command.MEETING_START, data);
//        } else {
//            connection.send(Command.MEETING_END, data);
//        }
//    }

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
