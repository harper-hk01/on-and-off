
package client.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import client.network.ServerConnection;
import client.network.ServerListener;
import client.model.ChatModel;
import client.view.MainView; //이건 아직 안만들어져 있음. 근데 필요
import client.view.ChatView;

import common.Message;
import common.Command;
import dto.common.Status;
import dto.common.UserDTO;
import dto.common.MessageDTO;
import dto.common.ChatRoomDTO;
import dto.request.*;

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

    private MainView mainView; //이건 아직 안만들어져 있음. 근데 필요
    private Map<Long, ChatView> chatViews;

    public ChatController(ServerConnection connection,
                          ChatModel model,
                          CommandHandler commandHandler) {

    }
    /**
    * 1. 서버에서 받은 메시지를 뷰 동작으로 연결
    *
    * - 

    */


    @Override
    public void onMessage(Message msg) {
    	SwingUtilities.invokeLater(() -> handle(msg)); //view
    }

    @Override
    public void onDisconnected() {
    	SwingUtilities.invokeLater(() -> handleDisconnected());
    }

    //Command 종류에 따라 응답 분류 및 처리
    //Model 갱신, View 갱신
    private void handle(Message msg) {
    }

    private void handleDisconnected() {

    }
    
    
    /**
    * 2. View에서 발생한 동작을 서버에 요청으로 연결
    *     1) 회원 및 사용자 기능
    * - 

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
    * - 

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

    public void toggleMeeting(long roomId) {
        MeetingRequest data = new MeetingRequest(roomId);

        if (isMeeting) {
            connection.send(Command.MEETING_START, data);
        } else {
            connection.send(Command.MEETING_END, data);
        }
    }

    /**
    * 3. 화면 관리
    * - 

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
