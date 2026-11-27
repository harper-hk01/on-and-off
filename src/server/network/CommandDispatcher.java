package server.network;

import common.Command;
import common.Message;
import dto.common.ResultDTO;
import dto.request.BuzzRequest;
import dto.request.ChatRequest;
import dto.request.FriendAddRequest;
import dto.request.IdCheckRequest;
import dto.request.JoinRequest;
import dto.request.LoginRequest;
import dto.request.MeetingEndRequest;
import dto.request.MeetingStartRequest;
import dto.request.NoticeSetRequest;
import dto.request.ProfileUpdateRequest;
import dto.request.RoomCreateRequest;
import dto.request.RoomEnterRequest;
import dto.request.RoomOpenRequest;
import dto.request.RoomReadRequest;
import dto.request.SearchRequest;
import dto.request.StatusRequest;
import dto.request.TypingRequest;
import dto.response.ErrorDTO;
import dto.response.FriendAddResult;
import dto.response.IdCheckResult;
import dto.response.JoinResult;
import dto.response.LoginResult;
import dto.response.ProfileUpdateResult;
import dto.response.RoomCreateResult;
import dto.response.RoomInfo;
import dto.response.SearchResult;
import server.dao.DAOException;
import server.service.ChatService;
import server.service.FriendService;
import server.service.LoginInfo;
import server.service.MeetingService;
import server.service.RoomService;
import server.service.ServiceException;
import server.service.UserService;

public class CommandDispatcher {
	private final UserService userService;
	private final FriendService friendService;
	private final ChatService chatService;
	private final RoomService roomService;
	private final MeetingService meetingService;

	public CommandDispatcher(UserService userService, FriendService friendService, ChatService chatService,
			RoomService roomService, MeetingService meetingService) {
		this.userService = userService;
		this.friendService = friendService;
		this.chatService = chatService;
		this.roomService = roomService;
		this.meetingService = meetingService;
	}

	public void dispatch(ClientHandler handler, Message msg) {
		Command type = msg.getType();

		// 로그인 전이고, 요청이 ID_CHECK도 아니고 JOIN도 아니고 LOGIN도 아니면 → ERROR 보내고 끝
		if (!handler.isLoggedIn() && type != Command.ID_CHECK && type != Command.JOIN && type != Command.LOGIN) {
			sendError(handler, type, "로그인이 필요합니다.");
			return;
		}

		long me = handler.getUserId();

		try {
			switch (type) { // 나중에 1줄로 고치기
			// UserService
			case ID_CHECK: {
				IdCheckRequest req = (IdCheckRequest) msg.getData(); // 꺼내기
				IdCheckResult result = userService.checkId(req); // 처리
				handler.send(Command.ID_CHECK_RESULT, result); // 응답
				break;
			}
			case JOIN: {
				JoinRequest req = (JoinRequest) msg.getData();
				JoinResult result = userService.join(req);
				handler.send(Command.JOIN_RESULT, result);
				break;
			}
			case LOGIN: {
				LoginRequest req = (LoginRequest) msg.getData();
				handleLogin(handler, req);
				break;
			}
			case LOGOUT: {
				userService.logout(me, handler.getLoginId(), handler);
				handler.clearLoginUser(); // 연결은 유지, 로그인 정보만 지우기
				break;
			}
			// FriendService
			case FRIEND_ADD: {
				FriendAddRequest req = (FriendAddRequest) msg.getData();
				FriendAddResult result = friendService.addFriend(me, req);
				handler.send(Command.FRIEND_ADD_RESULT, result);
				break;
			}
			case STATUS: {
				StatusRequest req = (StatusRequest) msg.getData();
				friendService.changeStatus(me, req);
				break;
			}
			case PROFILE_UPDATE: {
				ProfileUpdateRequest req = (ProfileUpdateRequest) msg.getData();
				ProfileUpdateResult result = friendService.updateProfile(me, req);
				handler.send(Command.PROFILE_UPDATE_RESULT, result);
				break;
			}

			// RoomService
			case ROOM_OPEN: {
				RoomOpenRequest req = (RoomOpenRequest) msg.getData();
				RoomInfo info = roomService.openRoom(me, req);
				handler.send(Command.ROOM_INFO, info);
				break;
			}
			case ROOM_ENTER: {
				RoomEnterRequest req = (RoomEnterRequest) msg.getData();
				RoomInfo info = roomService.enterRoom(me, req);
				handler.send(Command.ROOM_INFO, info);
				break;
			}
			case ROOM_CREATE: {
				RoomCreateRequest req = (RoomCreateRequest) msg.getData();
				RoomCreateResult result = roomService.createRoom(me, req);
				handler.send(Command.ROOM_CREATE_RESULT, result);
				break;
			}
			case ROOM_READ: {
				RoomReadRequest req = (RoomReadRequest) msg.getData();
				roomService.markRead(me, req);
				break;
			}
			case NOTICE_SET: {
				NoticeSetRequest req = (NoticeSetRequest) msg.getData();
				roomService.setNotice(me, req);
				break;
			}
			// ChatService
			case CHAT: {
				ChatRequest req = (ChatRequest) msg.getData();
				chatService.sendChat(me, req);
				break;
			}
			case TYPING: {
				TypingRequest req = (TypingRequest) msg.getData();
				chatService.typing(me, req);
				break;
			}
			case BUZZ: {
				BuzzRequest req = (BuzzRequest) msg.getData();
				chatService.buzz(me, req);
				break;
			}
			// MeetingService
			case MEETING_START: {
				MeetingStartRequest req = (MeetingStartRequest) msg.getData();
				meetingService.startMeeting(me, req);
				break;
			}
			case MEETING_END: {
				MeetingEndRequest req = (MeetingEndRequest) msg.getData();
				meetingService.endMeeting(me, req);
				break;
			}
			default:
				sendError(handler, type, "처리할 수 없는 명령어입니다.");
			}
		} catch (ServiceException e) {
			// 예상한 실패 (Service가 잡은 중복 포함)
			sendFail(handler, type, e.getMessage());
		} catch (DAOException e) {
			// Service가 못 잡은 (빈도가 낮은) 중복 - 통일 문구
			if (e.isDuplicateKey()) {
				System.out.println("[중복 키] " + type + " : " + e.getMessage());
				sendFail(handler, type, "이미 등록된 정보입니다. 다시 확인해 주세요.");
			} else {
				// 그 밖의 DB 오류
				System.out.println("[DB 오류] " + type);
				e.printStackTrace();
				sendError(handler, type, "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."); // 사용자용
			}
		} catch (Exception e) {
			System.out.println("[코드 오류] " + type);
			e.printStackTrace();
			sendError(handler, type, "서버 오류가 발생했습니다.");
		}

	}

	private Command resultTypeOf(Command type) { // switch -> try catch로 변경
		try {
			return Command.valueOf(type.name() + "_RESULT"); // "SEARCH" + "_RESULT" → SEARCH_RESULT
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	// 연결이 끊긴 경우 로그인 상태였다면 logout을 불러 OFFLINE처리, 친구 알림, 명부 해제
	public void disconnect(ClientHandler handler) {
		if (handler.isLoggedIn()) {
			try {
				userService.logout(handler.getUserId(), handler.getLoginId(), handler);
			} catch (RuntimeException e) {
				System.out.println("로그아웃 처리 중 오류: " + e.getMessage());
			}
		}
	}

	private void handleLogin(ClientHandler handler, LoginRequest req) {
		LoginInfo info = userService.login(req, handler); // 로그인 (실패면 예외)
		handler.setLoginUser(info.getUserId(), req.getLoginId()); // 연결에 이름표 붙이기
		handler.send(Command.LOGIN_RESULT, new LoginResult(true, "로그인 성공", info.getUser())); // 결과 응답
		handler.send(Command.FRIEND_LIST, friendService.getFriendList(info.getUserId())); // 로그인한 사람에게 친구 목록 보내기
		handler.send(Command.ROOM_LIST, roomService.getRoomList(info.getUserId())); // 로그인한 사람에게 대화방 목록 보내기
	}

	// 실패 시 ERROR + ErrorDTO 응답
	private void sendError(ClientHandler handler, Command type, String message) {
		String requestType = (type == null) ? null : type.name();
		handler.send(Command.ERROR, new ErrorDTO(requestType, message));
	}

	// _RESULT가 있으면 success:false, 없으면 ERROR
	private void sendFail(ClientHandler handler, Command type, String message) {
		Command resultType = resultTypeOf(type);
		if (resultType != null) {
			handler.send(resultType, new ResultDTO(false, message));
		} else {
			sendError(handler, type, message);
		}

	}

}
