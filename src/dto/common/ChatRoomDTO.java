package dto.common;

import java.util.List;

/**
 * 대화방 하나.
 * 안에 참여자(UserDTO 여러 개)와 고정 공지(MessageDTO 하나)가 들어간다.
 */
public class ChatRoomDTO {

    private long roomId;                // 방 번호
    private RoomType roomType;          // 방 모드 (DAILY: 1:1 방, WORK: 그룹방)
    private String roomName;            // 그룹방 이름. 1:1 방은 null
    private List<UserDTO> members;      // 참여자 (나 포함)
    private MessageDTO pinnedMessage;   // 고정된 공지. 없으면 null
    private boolean hasNew;             // 안 읽은 메시지가 있나 (NEW 배지)

    public ChatRoomDTO() {
    }

    public ChatRoomDTO(long roomId, RoomType roomType, String roomName,
                       List<UserDTO> members, MessageDTO pinnedMessage, boolean hasNew) {
        this.roomId = roomId;
        this.roomType = roomType;
        this.roomName = roomName;
        this.members = members;
        this.pinnedMessage = pinnedMessage;
        this.hasNew = hasNew;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public List<UserDTO> getMembers() {
        return members;
    }

    public void setMembers(List<UserDTO> members) {
        this.members = members;
    }

    public MessageDTO getPinnedMessage() {
        return pinnedMessage;
    }

    public void setPinnedMessage(MessageDTO pinnedMessage) {
        this.pinnedMessage = pinnedMessage;
    }

    public boolean isHasNew() {
        return hasNew;
    }

    public void setHasNew(boolean hasNew) {
        this.hasNew = hasNew;
    }

    @Override
    public String toString() {
        return "ChatRoomDTO{roomId=" + roomId + ", roomType=" + roomType
                + ", roomName=" + roomName + ", members=" + members
                + ", pinnedMessage=" + pinnedMessage + ", hasNew=" + hasNew + "}";
    }
}