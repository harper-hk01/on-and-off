package common;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import common.dto_old.*;
import dto.common.ChatRoomDTO;
import dto.common.MessageDTO;
import dto.common.UserDTO;

/**
 * 서버와 클라이언트가 주고받는 모든 명령어(type).
 * 괄호 안 = 그 명령어의 data를 담는 DTO 종류. data가 없으면 null.
 * Message.parse()가 이 정보를 보고 data를 알맞은 DTO로 바꾼다.
 */
public enum Command {

    // ===== 4.1 회원 =====
    ID_CHECK(IdCheckRequest.class),                 // C→S
    ID_CHECK_RESULT(IdCheckResult.class),           // S→C
    JOIN(JoinRequest.class),                        // C→S
    JOIN_RESULT(JoinResult.class),                  // S→C
    LOGIN(LoginRequest.class),                      // C→S
    LOGIN_RESULT(LoginResult.class),                // S→C
    LOGOUT(null),                                   // C→S, data 없음 → {}

    // ===== 4.2 친구 =====
    SEARCH(SearchRequest.class),                    // C→S
    SEARCH_RESULT(SearchResult.class),              // S→C
    FRIEND_ADD(FriendAddRequest.class),             // C→S
    FRIEND_ADD_RESULT(FriendAddResult.class),       // S→C
    FRIEND_NOTI(UserDTO.class),                     // S→C
    FRIEND_LIST(new TypeToken<List<UserDTO>>() { }.getType()),          // S→C

    // ===== 4.3 상태·프로필 =====
    STATUS(StatusRequest.class),                    // C→S
    STATUS_NOTI(StatusNoti.class),                  // S→C
    PROFILE_UPDATE(ProfileUpdateRequest.class),     // C→S
    PROFILE_UPDATE_RESULT(ProfileUpdateResult.class), // S→C
    PROFILE_NOTI(UserDTO.class),                    // S→C

    // ===== 4.4 대화방 =====
    ROOM_OPEN(RoomOpenRequest.class),               // C→S
    ROOM_ENTER(RoomEnterRequest.class),             // C→S
    ROOM_CREATE(RoomCreateRequest.class),           // C→S
    ROOM_CREATE_RESULT(RoomCreateResult.class),     // S→C
    ROOM_INFO(RoomInfo.class),                      // S→C
    ROOM_LIST(new TypeToken<List<ChatRoomDTO>>() { }.getType()),        // S→C
    ROOM_INVITED(ChatRoomDTO.class),                // S→C
    ROOM_READ(RoomReadRequest.class),               // C→S

    // ===== 4.5 채팅 =====
    CHAT(ChatRequest.class),                        // C→S
    CHAT_RECV(MessageDTO.class),                    // S→C
    TYPING(TypingRequest.class),                    // C→S
    TYPING_NOTI(TypingNoti.class),                  // S→C

    // ===== 4.6 일상 모드? =====
    BUZZ(BuzzRequest.class),                        // C→S
    BUZZ_RECV(BuzzRecv.class),                      // S→C

    // ===== 4.7 업무 모드 =====
    NOTICE_SET(NoticeSetRequest.class),             // C→S
    NOTICE_NOTI(NoticeNoti.class),                  // S→C
    MEETING_START(MeetingStartRequest.class),       // C→S
    MEETING_END(MeetingEndRequest.class),           // C→S
    MEETING_NOTI(MeetingNoti.class),                // S→C
    NUDGE_NOTI(NudgeNoti.class),                    // S→C

    // ===== 4.8 공통 =====
    ERROR(ErrorDTO.class);                          // S→C

    // ---------------------------------------------

    private final Type dataType;

    Command(Type dataType) {
        this.dataType = dataType;
    }

    /* 이 명령어의 data를 어떤 DTO로 풀어야 하는지.
     * 하나짜리는 그냥.class, data가 목록이면 TypeToken, data가 없으면 null. */
      
    public Type getDataType() {
        return dataType;
    }
}
