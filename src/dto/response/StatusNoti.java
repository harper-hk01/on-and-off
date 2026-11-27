package dto.response;

import dto.common.Status;

/**
 * STATUS_NOTI (S→C): 친구들에게, 이 사람 상태가 바뀌었어요.
 */
public class StatusNoti {

    private String loginId;  // 상태가 바뀐 사람의 로그인 아이디
    private Status status;   // 바뀐 접속 상태

    public StatusNoti() {
    }

    public StatusNoti(String loginId, Status status) {
        this.loginId = loginId;
        this.status = status;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "StatusNoti{loginId=" + loginId
                + ", status=" + status + "}";
    }
}
