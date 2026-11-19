package dto.response;

import dto.common.ResultDTO;

/**
 * PROFILE_UPDATE_RESULT (S→C): 프로필 수정 성공 여부.
 * success, message는 ResultDTO에서 물려받는다. 추가 필드 없음.
 */
public class ProfileUpdateResult extends ResultDTO {

    public ProfileUpdateResult() {
    }

    public ProfileUpdateResult(boolean success, String message) {
        super(success, message);
    }

    @Override
    public String toString() {
        return "ProfileUpdateResult{success=" + isSuccess()
                + ", message=" + getMessage() + "}";
    }
}
