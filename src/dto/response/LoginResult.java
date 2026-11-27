package dto.response;

import dto.common.ResultDTO;
import dto.common.UserDTO;

/**
 * LOGIN_RESULT (S→C): 로그인 결과와 내 정보.
 * success, message는 ResultDTO에서 물려받는다. 실패하면 me는 null.
 */
public class LoginResult extends ResultDTO {

    private UserDTO me;  // 로그인한 내 정보

    public LoginResult() {
    }

    public LoginResult(boolean success, String message, UserDTO me) {
        super(success, message);
        this.me = me;
    }

    public UserDTO getMe() {
        return me;
    }

    public void setMe(UserDTO me) {
        this.me = me;
    }

    @Override
    public String toString() {
        return "LoginResult{success=" + isSuccess() + ", message=" + getMessage() + ", me=" + me + "}";
    }
}
