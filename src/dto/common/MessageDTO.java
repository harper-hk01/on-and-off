package dto.common;

/**
 * 메시지 하나.
 * senderId는 DB의 숫자 번호가 아니라 보낸 사람의 로그인 아이디 문자열이다.
 */
public class MessageDTO {

    private long messageId;         // 메시지 번호. 나중에 보낼수록 큼
    private long roomId;            // 어느 방의 메시지인지
    private String senderId;        // 보낸 사람 아이디 (예: "chulsoo")
    private String senderNickname;  // 보낸 사람 닉네임
    private String content;         // 내용
    private String sentAt;          // 보낸 시각 "yyyy-MM-dd HH:mm"

    public MessageDTO() {
    }

    public MessageDTO(long messageId, long roomId, String senderId, String senderNickname,
                      String content, String sentAt) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.senderId = senderId;
        this.senderNickname = senderNickname;
        this.content = content;
        this.sentAt = sentAt;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
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

    public String getSenderNickname() {
        return senderNickname;
    }

    public void setSenderNickname(String senderNickname) {
        this.senderNickname = senderNickname;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSentAt() {
        return sentAt;
    }

    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }

    @Override
    public String toString() {
        return "MessageDTO{messageId=" + messageId + ", roomId=" + roomId
                + ", senderId=" + senderId + ", senderNickname=" + senderNickname
                + ", content=" + content + ", sentAt=" + sentAt + "}";
    }
}
