package dto.request;

/**
 * ID_CHECK (C→S): 이 아이디 쓸 수 있나요?
 */
public class IdCheckRequest {

    private String loginId;  // 확인할 로그인 아이디

    public IdCheckRequest() {
    }

    public IdCheckRequest(String loginId) {
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
        return "IdCheckRequest{loginId=" + loginId + "}";
    }
}
