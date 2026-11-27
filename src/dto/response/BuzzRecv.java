package dto.response;

/**
 * BUZZ_RECV (S→C): 이 사람이 버즈를 보냈어요.
 */
public class BuzzRecv {

    private long roomId;      // 방 번호
    private String senderId;  // 버즈를 보낸 사람의 로그인 아이디

    public BuzzRecv() {
    }

    public BuzzRecv(long roomId, String senderId) {
        this.roomId = roomId;
        this.senderId = senderId;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    @Override
    public String toString() {
        return "BuzzRecv{roomId=" + roomId
                + ", senderId=" + senderId + "}";
    }
}
