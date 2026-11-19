package dto.response;

import java.util.List;

import dto.common.ChatRoomDTO;
import dto.common.MessageDTO;

/**
 * ROOM_INFO (S→C): 방 정보, 회의 중 여부, 최근 메시지.
 * messages는 최근 Protocol.HISTORY_LIMIT(50)개.
 */
public class RoomInfo {

    private ChatRoomDTO room;           // 방 정보
    private boolean meeting;            // 지금 회의 중인지
    private List<MessageDTO> messages;  // 최근 메시지 목록

    public RoomInfo() {
    }

    public RoomInfo(ChatRoomDTO room, boolean meeting, List<MessageDTO> messages) {
        this.room = room;
        this.meeting = meeting;
        this.messages = messages;
    }

    public ChatRoomDTO getRoom() {
        return room;
    }

    public void setRoom(ChatRoomDTO room) {
        this.room = room;
    }

    public boolean isMeeting() {
        return meeting;
    }

    public void setMeeting(boolean meeting) {
        this.meeting = meeting;
    }

    public List<MessageDTO> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageDTO> messages) {
        this.messages = messages;
    }

    @Override
    public String toString() {
        return "RoomInfo{room=" + room
                + ", meeting=" + meeting
                + ", messages=" + messages + "}";
    }
}
