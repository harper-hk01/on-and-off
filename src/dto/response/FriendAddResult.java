package dto.response;

import dto.common.ResultDTO;
import dto.common.UserDTO;

/**
 * FRIEND_ADD_RESULT (S→C): 새로 추가된 친구.
 * success, message는 ResultDTO에서 물려받는다. 실패하면 friend는 null.
 */
public class FriendAddResult extends ResultDTO {

    private UserDTO friend;  // 새로 추가된 친구

    public FriendAddResult() {
    }

    public FriendAddResult(boolean success, String message, UserDTO friend) {
        super(success, message);
        this.friend = friend;
    }

    public UserDTO getFriend() {
        return friend;
    }

    public void setFriend(UserDTO friend) {
        this.friend = friend;
    }

    @Override
    public String toString() {
        return "FriendAddResult{success=" + isSuccess()
                + ", message=" + getMessage()
                + ", friend=" + friend + "}";
    }
}
