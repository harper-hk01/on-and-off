package dto.request;

/**
 * MEETING_START (C→S): 회의를 시작할게요.
 */
public class MeetingStartRequest {

    private long roomId;  // 회의를 시작할 방 번호

    public MeetingStartRequest() {
    }

    public MeetingStartRequest(long roomId) {
        this.roomId = roomId;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    @Override
    public String toString() {
        return "MeetingStartRequest{roomId=" + roomId + "}";
    }
}
