package server.service;

import dto.common.UserDTO;

/* 로그인 성공 결과 login()를 dispatcher에 돌려줄 때 쓰는 서버 내부용 상자
 * userId는 클라이언트로 보내지 않음  */

public class LoginInfo {
	private final long userId;
	private final UserDTO user;
	
	public LoginInfo(long userId, UserDTO user) {
		this.userId = userId;
		this.user = user;
	}
	
	public long getUserId() { return userId; }
	public UserDTO getUser() { return user; }
}
