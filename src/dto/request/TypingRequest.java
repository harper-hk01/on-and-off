package dto.request;

/**
 * TYPING (C→S): 저 지금 입력 중이에요.
 */
public class TypingRequest {

    private long roomId;  // 입력 중인 방 번호

    public TypingRequest() {
    }

    public TypingRequest(long roomId) {
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
        return "TypingRequest{roomId=" + roomId + "}";
    }
}
