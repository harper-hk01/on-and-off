package server.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import common.Command;
import common.Protocol;
import dto.common.ChatRoomDTO;
import dto.common.MessageDTO;
import dto.common.RoomType;
import dto.common.UserDTO;
import dto.request.NoticeSetRequest;
import dto.request.RoomCreateRequest;
import dto.request.RoomEnterRequest;
import dto.request.RoomOpenRequest;
import dto.request.RoomReadRequest;
import dto.response.NoticeNoti;
import dto.response.RoomCreateResult;
import dto.response.RoomInfo;
import server.dao.FriendDAO;
import server.dao.MessageDAO;
import server.dao.RoomDAO;
import server.dao.UserDAO;
import server.session.SessionRegistry;

// 초기 설계와 달라진 부분 : 1:1방 생성 시 친구 여부 검사하기 위해서 필드, 생성자에 FriendDAO 추가


public class RoomService {

	private final RoomDAO roomDAO;
	private final UserDAO userDAO;
	private final MessageDAO messageDAO;
	private final MeetingService meetingService;
	private final SessionRegistry registry;
	private final FriendDAO friendDAO;

	public RoomService(RoomDAO roomDAO, UserDAO userDAO, MessageDAO messageDAO, MeetingService meetingService,
			SessionRegistry registry, FriendDAO friendDAO) {
		this.roomDAO = roomDAO;
		this.userDAO = userDAO;
		this.messageDAO = messageDAO;
		this.meetingService = meetingService;
		this.registry = registry;
		this.friendDAO = friendDAO;
	}

	// 친구와의 1:1 채팅방 열기 (ROOM_OPEN)
	public RoomInfo openRoom(long me, RoomOpenRequest req){
		Long friendId = userDAO.findUserId(req.getLoginId());

		if (friendId == null) {
			throw new ServiceException("사용자를 찾을 수 없습니다.");
		}
		if (friendId == me) {
			throw new ServiceException("자기 자신과는 1:1 방을 열 수 없습니다.");
		}
		if (!friendDAO.isFriend(me, friendId)) {
			throw new ServiceException("친구와만 1:1 채팅방을 열 수 있습니다.");
		}

		long roomId = getOrCreateDirectRoom(me, friendId);
		return buildRoomInfo(me, roomId);
	}

	// 기존 채팅방 입장 (ROOM_ENTER)
	public RoomInfo enterRoom(long me, RoomEnterRequest req) {
		return buildRoomInfo(me, req.getRoomId());
	}

	// 그룹 채팅방 생성 (ROOM_CREATE)
	public RoomCreateResult createRoom(long me, RoomCreateRequest req) {
		String roomName = req.getRoomName();

		if (roomName == null || roomName.isBlank()) {
			return new RoomCreateResult(false, "채팅방 이름을 입력해 주세요.", null);
		}

		roomName = roomName.strip();
		if (roomName.length() > Protocol.MAX_ROOM_NAME) {
			return new RoomCreateResult(false, "채팅방 이름이 너무 깁니다.", null);
		}

		Set<Long> memberIds = new LinkedHashSet<>();
		List<String> invitedLoginIds = new ArrayList<>();
		memberIds.add(me);

		if (req.getMemberIds() != null) {
			for (String loginId : req.getMemberIds()) {
				Long userId = userDAO.findUserId(loginId);

				if (userId == null) {
					return new RoomCreateResult(false, "초대할 사용자를 찾을 수 없습니다.", null);
				}

				// 새 참여자만 추가해 본인과 중복 선택한 사용자에게 알림이 가지 않도록 한다.
				if (memberIds.add(userId)) {
					invitedLoginIds.add(loginId);
				}
			}
		}

		if (invitedLoginIds.isEmpty()) {
			return new RoomCreateResult(false, "초대할 사람을 선택해 주세요.", null);
		}

		long roomId = roomDAO.createRoom(RoomType.WORK, roomName, new ArrayList<>(memberIds));
		ChatRoomDTO room = getRoomOrThrow(roomId);

		registry.sendToAll(invitedLoginIds, Command.ROOM_INVITED, room);
		return new RoomCreateResult(true, "채팅방을 생성했습니다.", room);
	}

	// 내가 참여 중인 채팅방 목록 조회 (ROOM_LIST)
	public List<ChatRoomDTO> getRoomList(long me) {
		return roomDAO.findMyRooms(me);
	}

	// 마지막으로 읽은 메시지 갱신 (ROOM_READ)
	public void markRead(long me, RoomReadRequest req)  {
		long roomId = req.getRoomId();
		long messageId = req.getLastReadMessageId();

		requireMember(roomId, me);

		if (!messageDAO.existsInRoom(messageId, roomId)) {
			throw new ServiceException("이 채팅방의 메시지를 찾을 수 없습니다.");
		}

		// DAO의 UPDATE 조건이 기존 읽음 위치보다 큰 번호만 저장한다.
		roomDAO.updateLastRead(roomId, me, messageId);
	}

	// 업무 채팅방 공지 설정 또는 해제 (NOTICE_SET)
	public void setNotice(long me, NoticeSetRequest req)   {
		long roomId = req.getRoomId();
		requireMember(roomId, me);

		ChatRoomDTO room = getRoomOrThrow(roomId);
		if (room.getRoomType() != RoomType.WORK) {
			throw new ServiceException("공지는 업무방에서만 설정할 수 있습니다.");
		}

		Long messageId = req.getMessageId();
		MessageDTO pinnedMessage = null;

		// messageId가 null이면 공지를 해제한다.
		if (messageId != null) {
			if (!messageDAO.existsInRoom(messageId, roomId)) {
				throw new ServiceException("이 채팅방의 메시지를 찾을 수 없습니다.");
			}

			pinnedMessage = messageDAO.findById(messageId);
			if (pinnedMessage == null) {
				throw new ServiceException("공지로 설정할 메시지를 찾을 수 없습니다.");
			}
		}

		roomDAO.updatePinnedMessage(roomId, messageId);

		NoticeNoti notice = new NoticeNoti(roomId, pinnedMessage);
		List<String> memberLoginIds = roomDAO.findMemberLoginIds(roomId);
		registry.sendToAll(memberLoginIds, Command.NOTICE_NOTI, notice);
	}

	// 기존 1:1 방이 있으면 재사용하고, 새로 만들었을 때만 상대에게 알린다.
	private long getOrCreateDirectRoom(long me, long friendId)   {
		long newRoomId;

		// 같은 RoomService를 공유하는 두 요청이 동시에 방을 만들지 않도록 한다.
		synchronized (this) {
			Long existingRoomId = roomDAO.findDirectRoomId(me, friendId);
			if (existingRoomId != null) {
				return existingRoomId;
			}

			newRoomId = roomDAO.createRoom(RoomType.DAILY, null, List.of(me, friendId));
		}

		ChatRoomDTO room = getRoomOrThrow(newRoomId);
		UserDTO friend = userDAO.findById(friendId);
		if (friend == null) {
			throw new ServiceException("사용자를 찾을 수 없습니다.");
		}

		registry.sendTo(friend.getLoginId(), Command.ROOM_INVITED, room);
		return newRoomId;
	}

	// 방 정보, 공지, 회의 여부, 최근 메시지를 합쳐 입장 응답을 만든다.
	private RoomInfo buildRoomInfo(long me, long roomId)   {
		requireMember(roomId, me);

		ChatRoomDTO room = getRoomOrThrow(roomId);
		Long pinnedMessageId = roomDAO.findPinnedMessageId(roomId);
		if (pinnedMessageId != null) {
			room.setPinnedMessage(messageDAO.findById(pinnedMessageId));
		}

		// findRecent()는 최근 메시지를 오래된 것부터 정렬해 반환한다.
		List<MessageDTO> messages = messageDAO.findRecent(roomId, Protocol.HISTORY_LIMIT);
		boolean meeting = room.getRoomType() == RoomType.WORK && meetingService.isMeeting(roomId);

		return new RoomInfo(room, meeting, messages);
	}

	private void requireMember(long roomId, long userId)   {
		if (!roomDAO.isMember(roomId, userId)) {
			throw new ServiceException("채팅방에 접근할 권한이 없습니다.");
		}
	}

	// findRoom()은 참여자 목록까지 채워서 반환하고, 방이 없으면 null을 반환한다.
	private ChatRoomDTO getRoomOrThrow(long roomId)   {
		ChatRoomDTO room = roomDAO.findRoom(roomId);
		if (room == null) {
			throw new ServiceException("채팅방을 찾을 수 없습니다.");
		}
		return room;
	}
}
