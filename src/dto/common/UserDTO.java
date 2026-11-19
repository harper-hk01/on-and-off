package dto.common;

/**
 * 회원 한 명.
 * 비밀번호와 회원 번호(user_id)는 넣지 않는다. 남에게 보내면 안 되기 때문.
 */
public class UserDTO {

    private String loginId;        // 로그인 아이디
    private String nickname;       // 화면에 보이는 이름
    private String statusMessage;  // 상태 메시지. 없으면 null
    private Status status;         // 접속 상태

    public UserDTO() {
    }

    public UserDTO(String loginId, String nickname, String statusMessage, Status status) {
        this.loginId = loginId;
        this.nickname = nickname;
        this.statusMessage = statusMessage;
        this.status = status;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "UserDTO{loginId=" + loginId + ", nickname=" + nickname
                + ", statusMessage=" + statusMessage + ", status=" + status + "}";
    }
}
