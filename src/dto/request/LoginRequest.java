package dto.request;

/**
 * LOGIN (C→S): 로그인할게요.
 */
public class LoginRequest {

    private String loginId;   // 로그인 아이디
    private String password;  // 비밀번호

    public LoginRequest() {
    }

    public LoginRequest(String loginId, String password) {
        this.loginId = loginId;
        this.password = password;
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

    @Override
    public String toString() {
        // 비밀번호는 로그에 찍히지 않도록 가린다
        return "LoginRequest{loginId=" + loginId + ", password=****}";
    }
}
