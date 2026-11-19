package dto.request;

/**
 * SEARCH (C→S): 이 아이디인 사람 찾아 주세요.
 */
public class SearchRequest {

    private String loginId;  // 찾을 사람의 로그인 아이디

    public SearchRequest() {
    }

    public SearchRequest(String loginId) {
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
        return "SearchRequest{loginId=" + loginId + "}";
    }
}
