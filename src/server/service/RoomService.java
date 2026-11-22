package server.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import common.Command;
import common.Protocol;
import dto.common.ChatRoomDTO;
import dto.common.MessageDTO;
import dto.common.RoomType;
import dto.request.NoticeSetRequest;
import dto.request.RoomCreateRequest;
import dto.request.RoomEnterRequest;
import dto.request.RoomOpenRequest;
import dto.request.RoomReadRequest;
import dto.response.NoticeNoti;
import dto.response.RoomCreateResult;
import dto.response.RoomInfo;
import server.dao.MessageDAO;
import server.dao.RoomDAO;
import server.dao.UserDAO;
import server.dao.FriendDAO;
import server.session.SessionRegistry;

// 현재 프로젝트 구조에 ServiceException 클래스가 없는데 제외하신 걸까요??
// CommandDispatcher 클래스 문서 봤더니 사용하시는 것 같아 일단 활용해서 적어뒀습니다. 제외하기로 했다면 수정하겠습니다!

//설계와 달라진 부분 : 1:1 방 생성 시 친구 여부 검사가 필요할 것 같아서 FriendDAO 필드, 생성자에 추가했습니다.

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

	// 친구와의 1:1 채팅방 열기(ROOM_OPEN)
	public RoomInfo openRoom(long me, RoomOpenRequest req) {
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

	// 기존 채팅방 입장(ROOM_ENTER)
	public RoomInfo enterRoom(long me, RoomEnterRequest req) {
		return buildRoomInfo(me, req.getRoomId());
	}

	// 그룹 채팅방 생성(ROOM_CREATE)
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

				// 본인이나 이미 추가한 사용자에게 초대를 중복 전송하지 않는다.
				if (memberIds.add(userId)) {
					invitedLoginIds.add(loginId);
				}
			}
		}

		if (invitedLoginIds.isEmpty()) {
			return new RoomCreateResult(false, "초대할 사람을 선택해 주세요.", null);
		}

		long roomId = roomDAO.createRoom(RoomType.WORK, roomName, new ArrayList<>(memberIds));
		ChatRoomDTO room = roomDAO.findRoom(roomId);
		room.setMembers(roomDAO.findMembers(roomId));

		registry.sendToAll(invitedLoginIds, Command.ROOM_INVITED, room);
		return new RoomCreateResult(true, "채팅방을 생성했습니다.", room);
	}

	// 내가 참여 중인 채팅방 목록 조회(ROOM_LIST)
	public List<ChatRoomDTO> getRoomList(long me) {
		List<ChatRoomDTO> rooms = roomDAO.findMyRooms(me);

		for (ChatRoomDTO room : rooms) {
			room.setMembers(roomDAO.findMembers(room.getRoomId()));
			room.setPinnedMessage(null);
		}

		return rooms;
	}

	// 채팅방에서 마지막으로 읽은 메시지 갱신(ROOM_READ)
	public void markRead(long me, RoomReadRequest req) {
		long roomId = req.getRoomId();
		long messageId = req.getLastReadMessageId();

		if (!roomDAO.isMember(roomId, me)) {
			throw new ServiceException("채팅방에 접근할 권한이 없습니다.");
		}
		if (!messageDAO.existsInRoom(messageId, roomId)) {
			throw new ServiceException("이 채팅방의 메시지를 찾을 수 없습니다.");
		}
		// updateLastRead에서 저장된 위치보다 최신일 때만 갱신하는지 확인 필요!!
		roomDAO.updateLastRead(roomId, me, messageId);
	}

	// 업무 채팅방 공지 설정 또는 해제(NOTICE_SET)
	public void setNotice(long me, NoticeSetRequest req) {
		long roomId = req.getRoomId();

		if (!roomDAO.isMember(roomId, me)) {
			throw new ServiceException("채팅방에 접근할 권한이 없습니다.");
		}

		ChatRoomDTO room = roomDAO.findRoom(roomId);
		if (room.getRoomType() != RoomType.WORK) {
			throw new ServiceException("공지는 업무방에서만 설정할 수 있습니다.");
		}

		Long messageId = req.getMessageId();
		MessageDTO pinnedMessage = null;

		if (messageId != null) {
			if (!messageDAO.existsInRoom(messageId, roomId)) {
				throw new ServiceException("이 채팅방의 메시지를 찾을 수 없습니다.");
			}
			pinnedMessage = messageDAO.findById(messageId);
		}

		roomDAO.updatePinnedMessage(roomId, messageId);

		NoticeNoti notice = new NoticeNoti(roomId, pinnedMessage);
		List<String> memberLoginIds = roomDAO.findMemberLoginIds(roomId);
		registry.sendToAll(memberLoginIds, Command.NOTICE_NOTI, notice);
	}

	// 두 사용자의 1:1 채팅방 ID 조회 또는 생성
	private long getOrCreateDirectRoom(long me, long friendId) {
		long newRoomId;

		// 두 사용자가 동시에 열어도 같은 1:1 방을 사용하도록 조회와 생성을 묶는다.
		synchronized (this) {
			Long existingRoomId = roomDAO.findDirectRoomId(me, friendId);

			if (existingRoomId != null) {
				return existingRoomId;
			}

			newRoomId = roomDAO.createRoom(RoomType.DAILY, null, List.of(me, friendId));
		}

		// 새로 만든 경우에만 상대에게 방 정보를 보낸다.
		ChatRoomDTO room = roomDAO.findRoom(newRoomId);
		room.setMembers(roomDAO.findMembers(newRoomId));

		String friendLoginId = userDAO.findById(friendId).getLoginId();
		registry.sendTo(friendLoginId, Command.ROOM_INVITED, room);

		return newRoomId;
	}

	// 방 정보, 회의 여부, 최근 메시지를 합쳐 입장 응답 구성
	private RoomInfo buildRoomInfo(long me, long roomId) {
		if (!roomDAO.isMember(roomId, me)) {
			throw new ServiceException("채팅방에 접근할 권한이 없습니다.");
		}

		ChatRoomDTO room = roomDAO.findRoom(roomId);
		room.setMembers(roomDAO.findMembers(roomId));

		Long pinnedMessageId = roomDAO.findPinnedMessageId(roomId);

		if (pinnedMessageId != null) {
			room.setPinnedMessage(messageDAO.findById(pinnedMessageId));
		}

		List<MessageDTO> messages = new ArrayList<>(messageDAO.findRecent(roomId, Protocol.HISTORY_LIMIT));
		Collections.reverse(messages);

		boolean meeting = room.getRoomType() == RoomType.WORK && meetingService.isMeeting(roomId);

		return new RoomInfo(room, meeting, messages);
	}
}