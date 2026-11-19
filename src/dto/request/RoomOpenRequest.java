package dto.request;

/**
 * ROOM_OPEN (C→S): 이 친구와의 1:1 방을 열어 주세요.
 */
public class RoomOpenRequest {

    private String loginId;  // 1:1 방을 열 친구의 로그인 아이디

    public RoomOpenRequest() {
    }

    public RoomOpenRequest(String loginId) {
        this.loginId = loginId;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    @Override
    public String toString() {
        return "RoomOpenRequest{loginId=" + loginId + "}";
    }
}
