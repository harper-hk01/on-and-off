package server.exception;

/*	Service에서 "예상한 실패"를 알릴 때 던지는 예외.
 *  ex. 비밀번호 틀림, 아이디 형식 틀림, 이미 친구라서 친구 신청 불가 등
 *  CommandDispatcher가 잡아서 success:false 또는 ERROR로 응답
 */
public class ServiceException extends RuntimeException {
	public ServiceException(String message) {
		super(message);
	}
}
