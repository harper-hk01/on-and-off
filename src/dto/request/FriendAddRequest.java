package dto.request;

/**
 * FRIEND_ADD (C→S): 이 사람을 친구로 추가할게요.
 */
public class FriendAddRequest {

    private String loginId;  // 친구로 추가할 사람의 로그인 아이디

    public FriendAddRequest() {
    }

    public FriendAddRequest(String loginId) {
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
        return "FriendAddRequest{loginId=" + loginId + "}";
    }
}
