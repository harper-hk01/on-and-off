package dto.request;

import java.util.List;

/**
 * ROOM_CREATE (C→S): 이 사람들과 그룹방을 만들게요.
 * memberIds에 나 자신을 포함하는지는 확인 필요.
 */
public class RoomCreateRequest {

    private String roomName;         // 그룹방 이름
    private List<String> memberIds;  // 초대할 사람들의 로그인 아이디 목록

    public RoomCreateRequest() {
    }

    public RoomCreateRequest(String roomName, List<String> memberIds) {
        this.roomName = roomName;
        this.memberIds = memberIds;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }

    @Override
    public String toString() {
        return "RoomCreateRequest{roomName=" + roomName
                + ", memberIds=" + memberIds + "}";
    }
}
