package dto.request;

/**
 * MEETING_END (C→S): 회의를 끝낼게요.
 */
public class MeetingEndRequest {

    private long roomId;  // 회의를 끝낼 방 번호

    public MeetingEndRequest() {
    }

    public MeetingEndRequest(long roomId) {
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
        return "MeetingEndRequest{roomId=" + roomId + "}";
    }
}
