package dto.response;

/**
 * TYPING_NOTI (S→C): 이 사람이 입력 중이에요.
 */
public class TypingNoti {

    private long roomId;     // 방 번호
    private String loginId;  // 입력 중인 사람의 로그인 아이디

    public TypingNoti() {
    }

    public TypingNoti(long roomId, String loginId) {
        this.roomId = roomId;
        this.loginId = loginId;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    @Override
    public String toString() {
        return "TypingNoti{roomId=" + roomId
                + ", loginId=" + loginId + "}";
    }
}
