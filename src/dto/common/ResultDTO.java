package dto.common;

/**
 * 모든 ~Result 상자의 부모.
 * 성공이면 message는 null, 실패면 success=false와 이유(message).
 */
public class ResultDTO {

    private boolean success;  // 성공 여부
    private String message;   // 실패 이유. 성공이면 null

    public ResultDTO() {
    }

    public ResultDTO(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{success=" + success + ", message=" + message + "}";
    }
}
