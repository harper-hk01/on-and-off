package dto.request;

/**
 * NOTICE_SET (C→S): 이 메시지를 공지로 고정할게요.
 * messageId가 null이면 공지 해제. 그래서 long이 아니라 Long이다.
 */
public class NoticeSetRequest {

    private long roomId;     // 방 번호
    private Long messageId;  // 공지로 고정할 메시지 번호. null이면 공지 해제

    public NoticeSetRequest() {
    }

    public NoticeSetRequest(long roomId, Long messageId) {
        this.roomId = roomId;
        this.messageId = messageId;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    @Override
    public String toString() {
        return "NoticeSetRequest{roomId=" + roomId
                + ", messageId=" + messageId + "}";
    }
}
