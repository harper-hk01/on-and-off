package dto.request;

/**
 * JOIN (C→S): 가입할게요.
 */
public class JoinRequest {

    private String loginId;   // 로그인 아이디
    private String password;  // 비밀번호
    private String nickname;  // 화면에 보이는 이름

    public JoinRequest() {
    }

    public JoinRequest(String loginId, String password, String nickname) {
        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    @Override
    public String toString() {
        return "JoinRequest{loginId=" + loginId
                + ", password=" + password
                + ", nickname=" + nickname + "}";
    }
}
