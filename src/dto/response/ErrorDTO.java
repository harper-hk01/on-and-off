package dto.response;

/**
 * ERROR (S→C): 요청을 처리하지 못했을 때 보내는 응답.
 * CommandDispatcher만 만든다.
 */
public class ErrorDTO {

    private String requestType;  // 실패한 요청의 명령어 이름 (예: "STATUS")
    private String message;      // 실패한 이유. 클라이언트는 그대로 화면에 띄움

    public ErrorDTO() {
    }

    public ErrorDTO(String requestType, String message) {
        this.requestType = requestType;
        this.message = message;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "ErrorDTO{requestType=" + requestType + ", message=" + message + "}";
    }
}
