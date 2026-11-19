package dto.response;

import dto.common.ResultDTO;

/**
 * ID_CHECK_RESULT (S→C): 아이디 사용 가능 여부.
 * success, message는 ResultDTO에서 물려받는다. 추가 필드 없음.
 */
public class IdCheckResult extends ResultDTO {

    public IdCheckResult() {
    }

    public IdCheckResult(boolean success, String message) {
        super(success, message);
    }

    @Override
    public String toString() {
        return "IdCheckResult{success=" + isSuccess()
                + ", message=" + getMessage() + "}";
    }
}
