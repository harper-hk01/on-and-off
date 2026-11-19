package dto.request;

/**
 * BUZZ (C→S): 버즈를 보낼게요.
 */
public class BuzzRequest {

    private long roomId;  // 버즈를 보낼 방 번호

    public BuzzRequest() {
    }

    public BuzzRequest(long roomId) {
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
        return "BuzzRequest{roomId=" + roomId + "}";
    }
}
