package server.service;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import common.Command;
import common.Protocol;
import dto.common.Status;
import dto.common.UserDTO;
import dto.request.FriendAddRequest;
import dto.request.ProfileUpdateRequest;
import dto.request.SearchRequest;
import dto.request.StatusRequest;
import dto.response.FriendAddResult;
import dto.response.ProfileUpdateResult;
import dto.response.SearchResult;
import dto.response.StatusNoti;
import server.dao.FriendDAO;
import server.dao.UserDAO;
import server.exception.DAOException;
import server.exception.ServiceException;
import server.session.SessionRegistry;

/**
 * 친구·상태·프로필 기능의 판단을 맡는 클래스.
 *
 * - 요청한 사람(me)은 필드가 아니라 매개변수로 받는다. 서버에 FriendService가 하나뿐이고 모든 사용자가 같이 쓰기 때문.
 * - *_RESULT 응답이 있는 명령(FRIEND_ADD, PROFILE_UPDATE)은 실패하면 success=false 결과를 돌려준다.
 *   응답 상자가 없는 명령(STATUS)은 ServiceException을 던진다.
 * - DB 오류(DAOException)는 잡지 않고 CommandDispatcher로 올려 보낸다.
 *   단, 친구 추가의 중복 저장(1062)만 여기서 잡아 알아볼 수 있는 문구로 바꾼다 (팀 결정).
 * - 알림(NOTI) 전송이 실패해도 요청 자체는 성공으로 처리하고 로그만 남긴다 (팀 결정).
 */
public class FriendService {

	private static final Logger LOGGER = Logger.getLogger(FriendService.class.getName());

	private final FriendDAO friendDAO;
	private final UserDAO userDAO;
	private final SessionRegistry registry;

	public FriendService(FriendDAO friendDAO, UserDAO userDAO, SessionRegistry registry) {
		this.friendDAO = friendDAO;
		this.userDAO = userDAO;
		this.registry = registry;
	}

	// 친구 추가 (FRIEND_ADD)
	public FriendAddResult addFriend(long me, FriendAddRequest req) {
		String loginId = trimToNull(req.getLoginId());
		if (loginId == null) {
			return new FriendAddResult(false, "추가할 아이디를 입력해 주세요.", null);
		}

		Long friendId = userDAO.findUserId(loginId);
		if (friendId == null) {
			return new FriendAddResult(false, "사용자를 찾을 수 없습니다.", null);
		}
		if (friendId == me) {
			return new FriendAddResult(false, "자기 자신은 친구로 추가할 수 없습니다.", null);
		}
		if (friendDAO.isFriend(me, friendId)) {
			return new FriendAddResult(false, "이미 친구입니다.", null);
		}

		try {
			friendDAO.insertBoth(me, friendId);
		} catch (DAOException e) {
			// 두 사람이 거의 동시에 서로를 추가하면 isFriend 검사를 둘 다 통과할 수 있다.
			// 이때 나중에 저장한 쪽이 중복(1062)에 걸리므로 알아볼 수 있는 문구로 바꾼다.
			if (e.isDuplicateKey()) {
				return new FriendAddResult(false, "이미 친구입니다.", null);
			}
			throw e; // 다른 DB 오류는 그대로 Dispatcher로 올려 보낸다.
		}

		UserDTO friend = getUserOrThrow(friendId);
		UserDTO myInfo = getUserOrThrow(me);

		// 상대에게 "새 친구가 생겼어요" 알림 (내 정보를 보냄)
		notifyOne(friend.getLoginId(), Command.FRIEND_NOTI, myInfo);

		return new FriendAddResult(true, null, friend);
	}

	// 내 친구 목록 (FRIEND_LIST). 로그인 직후 UserService도 부른다.
	// 접속 중인 친구가 위, 같은 그룹 안에서는 닉네임 순 (FriendDAO.findFriends의 정렬).
	public List<UserDTO> getFriendList(long me) {
		return friendDAO.findFriends(me);
	}

	// 내 친구들의 로그인 아이디 목록. 로그인·로그아웃 때 UserService가 STATUS_NOTI를 보낼 대상으로 쓴다.
	public List<String> getFriendLoginIds(long me) {
		return friendDAO.findFriendLoginIds(me);
	}

	// 내 상태 바꾸기 (STATUS). 바뀐 상태를 친구들에게 알린다.
	public void changeStatus(long me, StatusRequest req) {
		Status status = req.getStatus();
		if (status == null) {
			throw new ServiceException("바꿀 상태를 선택해 주세요.");
		}
		if (status == Status.OFFLINE) {
			// OFFLINE은 서버만 정한다 (로그아웃·연결 끊김 때 UserService가 처리).
			throw new ServiceException("오프라인 상태는 직접 선택할 수 없습니다.");
		}

		userDAO.updateStatus(me, status);

		UserDTO myInfo = getUserOrThrow(me);
		StatusNoti notice = new StatusNoti(myInfo.getLoginId(), status);
		notifyFriends(me, Command.STATUS_NOTI, notice);
	}

	// 닉네임·상태 메시지 바꾸기 (PROFILE_UPDATE). 바뀐 정보를 친구들에게 알린다.
	public ProfileUpdateResult updateProfile(long me, ProfileUpdateRequest req) {
		String nickname = trimToNull(req.getNickname());
		if (nickname == null) {
			return new ProfileUpdateResult(false, "닉네임을 입력해 주세요.");
		}
		if (nickname.length() > Protocol.MAX_NICKNAME) {
			return new ProfileUpdateResult(false, "닉네임은 " + Protocol.MAX_NICKNAME + "자까지 쓸 수 있습니다.");
		}

		// 상태 메시지는 비워도 된다. 비우면 null로 저장한다.
		String statusMessage = trimToNull(req.getStatusMessage());
		if (statusMessage != null && statusMessage.length() > Protocol.MAX_STATUS_MESSAGE) {
			return new ProfileUpdateResult(false,
					"상태 메시지는 " + Protocol.MAX_STATUS_MESSAGE + "자까지 쓸 수 있습니다.");
		}

		userDAO.updateProfile(me, nickname, statusMessage);

		UserDTO myInfo = getUserOrThrow(me);
		notifyFriends(me, Command.PROFILE_NOTI, myInfo);

		return new ProfileUpdateResult(true, null);
	}

	// ---------------- 도우미 ----------------

	private UserDTO getUserOrThrow(long userId) {
		UserDTO user = userDAO.findById(userId);
		if (user == null) {
			throw new ServiceException("사용자를 찾을 수 없습니다.");
		}
		return user;
	}

	// 앞뒤 공백을 지우고, 비어 있으면 null을 돌려준다.
	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String stripped = value.strip();
		return stripped.isEmpty() ? null : stripped;
	}

	// 한 명에게 알림. 실패해도 요청은 성공으로 두고 로그만 남긴다.
	private void notifyOne(String loginId, Command command, Object data) {
		try {
			registry.sendTo(loginId, command, data);
		} catch (RuntimeException e) {
			LOGGER.log(Level.WARNING, command + " 전송 실패 (to=" + loginId + ")", e);
		}
	}

	// 내 친구 전체에게 알림. 실패해도 요청은 성공으로 두고 로그만 남긴다.
	private void notifyFriends(long me, Command command, Object data) {
		try {
			List<String> friendLoginIds = friendDAO.findFriendLoginIds(me);
			registry.sendToAll(friendLoginIds, command, data);
		} catch (RuntimeException e) {
			LOGGER.log(Level.WARNING, command + " 전송 실패 (userId=" + me + ")", e);
		}
	}
}