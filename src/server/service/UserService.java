package server.service;

import java.sql.SQLException;
import java.util.List;

import common.Command;
import common.Protocol;
import dto.common.Status;
import dto.request.IdCheckRequest;
import dto.request.JoinRequest;
import dto.request.LoginRequest;
import dto.response.IdCheckResult;
import dto.response.JoinResult;
import dto.response.StatusNoti;
import server.dao.FriendDAO;
import server.dao.UserDAO;
import server.session.ClientSession;
import server.session.SessionRegistry;

public class UserService {
	private final UserDAO userDAO;
	private final FriendDAO friendDAO;
	private final SessionRegistry registry;
	
	public UserService(UserDAO userDAO, FriendDAO friendDAO, SessionRegistry registry) {
		this.userDAO = userDAO;
		this.friendDAO = friendDAO;
		this.registry = registry;
	}
	
	public IdCheckResult checkId(IdCheckRequest req) {
		String loginId = req.getLoginId();
		
		validateLoginId(loginId);
		
		boolean available = userDAO.countByLoginId(loginId) == 0;
		
		if(available) {
			return new IdCheckResult(true, "사용 가능한 아이디입니다.");
		}
		
		return new IdCheckResult(false, "이미 사용 중인 아이디입니다.");

	}
	
	public JoinResult join(JoinRequest req) {
		String loginId = req.getLoginId();
		
		validateLoginId(loginId);
		validatePassword(req.getPassword());
		validateNickname(req.getNickname());
		
		if(userDAO.countByLoginId(loginId) > 0) {
			return new JoinResult(false, "이미 사용 중인 아이디입니다.");
		}
		
		userDAO.insert(loginId, req.getPassword(), req.getNickname()); // DB에 정보 저장
		return new JoinResult(true, "환영합니다! 가입이 완료되었습니다.");
		
	}
	
	public LoginInfo login(LoginRequest req, ClientSession session) {
		
		// 비밀번호 확인
		String saved = userDAO.findPassword(req.getLoginId());
		if(saved == null || !saved.equals(req.getPassword())) {
			throw new ServiceException("아이디 또는 비밀번호가 틀렸습니다.");
		}
		
		
		// 명부 등록
		if(!registry.register(req.getLoginId(), session)) {
			throw new ServiceException("이미 접속 중인 아이디입니다.");
		}
		
		// 상태를 Online으로 바꾸기 
		long me = userDAO.findUserId(req.getLoginId());
		userDAO.updateStatus(me, Status.ONLINE);
		
		// 친구들에게 로그인 알림 보내기 (실제로 팝업으로 알릴 건지는 추후 결정)
		notifyFriends(me, req.getLoginId(), Status.ONLINE);
		
		return new LoginInfo(me, userDAO.findById(me));
	}
	
	public void logout(long me, String loginId, ClientSession session){
		userDAO.updateStatus(me, Status.OFFLINE); // 상태를 Offline으로 바꾸기 
		notifyFriends(me, loginId, Status.OFFLINE); // 친구들에게 로그아웃 알림 보내기 (실제로 팝업으로 알릴 건지는 추후 결정)
		notifyFriends(me, loginId, Status.OFFLINE); //명부 해제
	}
	
	public void resetAllOffline() {
		userDAO.updateAllStatus(Status.OFFLINE);
	}

	private void validateLoginId(String loginId) {
		if(loginId == null || loginId.isBlank()) {
			throw new ServiceException("아이디를 입력하세요.");
		}
		
		if(loginId.length() > Protocol.MAX_LOGIN_ID) {
			throw new ServiceException("아이디는 "+ Protocol.MAX_LOGIN_ID + "자 이하로 입력하세요.");
		}
		
		if(!loginId.matches(Protocol.LOGIN_ID_PATTERN)) {
			throw new ServiceException("아이디는 영문과 숫자만 쓸 수 있습니다.");
		}
	}
	
	private void validatePassword(String password) {
		if(password == null || password.isBlank()) {
			throw new ServiceException("비밀번호를 입력하세요.");
		}
		
		if(password.length() > Protocol.MAX_PASSWORD) {
			throw new ServiceException("비밀번호는 "+ Protocol.MAX_PASSWORD + "자 이하로 입력하세요.");
		}
		
		if(!password.matches(Protocol.PASSWORD_PATTERN)) {
			throw new ServiceException("비밀번호는 숫자만 쓸 수 있습니다.");
		}
	}
	
	
	private void validateNickname(String nickname) {
		if(nickname == null || nickname.isBlank()) {
			throw new ServiceException("닉네임을 입력하세요.");
		}
		
		if(nickname.length() > Protocol.MAX_NICKNAME) {
			throw new ServiceException("닉네임은 "+ Protocol.MAX_NICKNAME + "자 이하로 입력하세요.");
		}
		
	}
	
	private void notifyFriends(long me, String loginId, Status status) {
		List<String> friends = friendDAO.findFriendLoginIds(me);
		registry.sendToAll(friends, Command.STATUS_NOTI, new StatusNoti(loginId, status));
	}
	
}

