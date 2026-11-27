package dto.request;

/**
 * ROOM_ENTER (C→S): 이 방에 들어갈게요.
 */
public class RoomEnterRequest {

    private long roomId;  // 방 번호

    public RoomEnterRequest() {
    }

    public RoomEnterRequest(long roomId) {
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
        return "RoomEnterRequest{roomId=" + roomId + "}";
    }
}
