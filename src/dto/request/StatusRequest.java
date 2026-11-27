package dto.request;

import dto.common.Status;

/**
 * STATUS (C→S): 내 상태를 바꿀게요.
 * OFFLINE은 서버만 정한다. 클라이언트가 OFFLINE을 보내면 서버가 막아야 한다.
 */
public class StatusRequest {

    private Status status;  // 바꿀 접속 상태 (ONLINE, AWAY)

    public StatusRequest() {
    }

    public StatusRequest(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "StatusRequest{status=" + status + "}";
    }
}
