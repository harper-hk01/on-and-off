package dto.request;

/**
 * PROFILE_UPDATE (C→S): 닉네임·상태메시지를 바꿀게요.
 */
public class ProfileUpdateRequest {

    private String nickname;       // 새 닉네임
    private String statusMessage;  // 새 상태 메시지. 없으면 null

    public ProfileUpdateRequest() {
    }

    public ProfileUpdateRequest(String nickname, String statusMessage) {
        this.nickname = nickname;
        this.statusMessage = statusMessage;
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

    @Override
    public String toString() {
        return "ProfileUpdateRequest{nickname=" + nickname
                + ", statusMessage=" + statusMessage + "}";
    }
}
