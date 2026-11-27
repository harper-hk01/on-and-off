package dto.response;

import dto.common.ResultDTO;
import dto.common.UserDTO;

/**
 * SEARCH_RESULT (S→C): 찾은 사람.
 * success, message는 ResultDTO에서 물려받는다. 실패하면 user는 null.
 */
public class SearchResult extends ResultDTO {

    private UserDTO user;  // 찾은 사람

    public SearchResult() {
    }

    public SearchResult(boolean success, String message, UserDTO user) {
        super(success, message);
        this.user = user;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "SearchResult{success=" + isSuccess()
                + ", message=" + getMessage()
                + ", user=" + user + "}";
    }
}
