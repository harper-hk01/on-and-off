package dto.request;

/**
 * ROOM_READ (C→S): 이 방은 여기까지 읽었어요.
 */
public class RoomReadRequest {

    private long roomId;             // 방 번호
    private long lastReadMessageId;  // 마지막으로 읽은 메시지 번호

    public RoomReadRequest() {
    }

    public RoomReadRequest(long roomId, long lastReadMessageId) {
        this.roomId = roomId;
        this.lastReadMessageId = lastReadMessageId;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public long getLastReadMessageId() {
        return lastReadMessageId;
    }

    public void setLastReadMessageId(long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
    }

    @Override
    public String toString() {
        return "RoomReadRequest{roomId=" + roomId
                + ", lastReadMessageId=" + lastReadMessageId + "}";
    }
}
