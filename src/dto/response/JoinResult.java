package dto.response;

import dto.common.ResultDTO;

/**
 * JOIN_RESULT (S→C): 가입 성공 여부.
 * success, message는 ResultDTO에서 물려받는다. 추가 필드 없음.
 */
public class JoinResult extends ResultDTO {

    public JoinResult() {
    }

    public JoinResult(boolean success, String message) {
        super(success, message);
    }

    @Override
    public String toString() {
        return "JoinResult{success=" + isSuccess()
                + ", message=" + getMessage() + "}";
    }
}
