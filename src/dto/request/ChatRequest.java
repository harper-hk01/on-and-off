package dto.request;

/**
 * CHAT (C→S): 이 방에 메시지를 보낼게요.
 */
public class ChatRequest {

    private long roomId;     // 방 번호
    private String content;  // 보낼 내용

    public ChatRequest() {
    }

    public ChatRequest(long roomId, String content) {
        this.roomId = roomId;
        this.content = content;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "ChatRequest{roomId=" + roomId + ", content=" + content + "}";
    }
}
