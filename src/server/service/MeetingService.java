package server.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import common.Command;
import dto.common.ChatRoomDTO;
import dto.common.MeetingState;
import dto.common.RoomType;
import dto.common.UserDTO;
import dto.request.MeetingEndRequest;
import dto.request.MeetingStartRequest;
import dto.response.MeetingNoti;
import dto.response.NudgeNoti;
import server.dao.RoomDAO;
import server.dao.UserDAO;
import server.session.SessionRegistry;

public class MeetingService {
	
	//시연용으로 MINUTES->SECONDS로 변경
	private static final long NUDGE_AFTER_MS = TimeUnit.SECONDS.toMillis(10);
	private static final long CHECK_INTERVAL_MS = TimeUnit.SECONDS.toMillis(1);

	private final RoomDAO roomDAO;
	private final UserDAO userDAO;
	private final SessionRegistry registry;

	// 필드: 방 번호 → 로그인 아이디 → 다음 독려를 계산할 기준 시각(마지막 발언 or 마지막 독려)
	private final ConcurrentHashMap<Long, Map<String, Long>> nudgeBaseTimes = new ConcurrentHashMap<>();

	// 스레드 팩토리(팩토리 패턴) 사용하여 단일 스레드 생성, 스레드의 이름/데몬 여부 설정 위해 사용함
	// 보조 기능 스레드이므로 데몬 스레드로 작성하기 위해 newThread 재정의 (독려 스레드가 JVM 종료를 막지 않도록 하기 위함)
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable task) {
			Thread thread = new Thread(task, "meeting-silence-check");
			thread.setDaemon(true);
			return thread;
		}
	});

	public MeetingService(RoomDAO roomDAO, UserDAO userDAO, SessionRegistry registry) {
		this.roomDAO = roomDAO;
		this.userDAO = userDAO;
		this.registry = registry;
	}

	// 업무방 회의 시작 (MEETING_START)
	public void startMeeting(long me, MeetingStartRequest req) {
		long roomId = req.getRoomId();
		
		List<String> memberLoginIds = collectMemberLoginIds(roomId, me);
		String requesterLoginId = getLoginId(me);

		synchronized (this) {
			if (isMeeting(roomId)) {
				throw new ServiceException("이미 회의가 진행 중입니다.");
			}

			// 첫 발언 전에는 회의 시작 시각을 독려 기준으로 사용한다.
			long now = System.currentTimeMillis();
			Map<String, Long> memberTimes = new HashMap<>();
			for (String member : memberLoginIds) {
				memberTimes.put(member, now);
			}

			nudgeBaseTimes.put(roomId, memberTimes);

			MeetingNoti notice = new MeetingNoti(roomId, MeetingState.START, requesterLoginId);
			registry.sendToAll(memberLoginIds, Command.MEETING_NOTI, notice);
		}
	}

	// 업무방 회의 종료 (MEETING_END)
	public void endMeeting(long me, MeetingEndRequest req) {
		long roomId = req.getRoomId();
		
		List<String> memberLoginIds = collectMemberLoginIds(roomId, me);
		String requesterLoginId = getLoginId(me);
		
		synchronized (this) {
			if (!isMeeting(roomId)) {
				throw new ServiceException("진행 중인 회의가 없습니다.");
			}

			nudgeBaseTimes.remove(roomId);

			MeetingNoti notice = new MeetingNoti(roomId, MeetingState.END, requesterLoginId);
			registry.sendToAll(memberLoginIds, Command.MEETING_NOTI, notice);
		}
	}

	// RoomService가 ROOM_INFO를 만들 때 호출한다.
	public boolean isMeeting(long roomId) {
		return nudgeBaseTimes.containsKey(roomId);
	}

	// ChatService가 메시지를 DB에 저장한 뒤 호출한다.
	public void recordSpeech(long roomId, long userId) {
		Map<String, Long> memberTimes = nudgeBaseTimes.get(roomId);
		if (memberTimes == null) {
			return;
		}

		String loginId = getLoginId(userId);
		synchronized (this) {
			if (nudgeBaseTimes.get(roomId) == memberTimes) {
				memberTimes.replace(loginId, System.currentTimeMillis());
			}
		}
	}

	// ChatServer에서 서버 시작 시 한 번 호출한다.
	// 불필요한 중복검사와 필드 제거
	public void startScheduler() {
		scheduler.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				checkSilence();
			}
		}, CHECK_INTERVAL_MS, CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
	}

	// 마지막 발언 또는 독려 이후 기준 시간(10분)이 지난 온라인 참여자에게 알린다.
	private synchronized void checkSilence() {
		long now = System.currentTimeMillis();

		for (Map.Entry<Long, Map<String, Long>> room : nudgeBaseTimes.entrySet()) {
			long roomId = room.getKey();

			for (Map.Entry<String, Long> member : room.getValue().entrySet()) {
				String loginId = member.getKey();
				if (now - member.getValue() < NUDGE_AFTER_MS) {
					continue;
				}

				try {
					if (registry.isOnline(loginId)) {
						NudgeNoti notice = new NudgeNoti(roomId, loginId);
						registry.sendTo(loginId, Command.NUDGE_NOTI, notice);
						member.setValue(now);
					}
				} catch (RuntimeException e) {
					System.err
							.println("회의 독려 전송 실패 (roomId=" + roomId + ", loginId=" + loginId + "): " + e.getMessage());
				}
			}
		}
	}

	private ChatRoomDTO requireWorkRoomMember(long roomId, long userId) {
		if (!roomDAO.isMember(roomId, userId)) {
			throw new ServiceException("채팅방에 접근할 권한이 없습니다.");
		}

		ChatRoomDTO room = roomDAO.findRoom(roomId);
		if (room == null || room.getRoomType() != RoomType.WORK) {
			throw new ServiceException("회의는 업무방에서만 사용할 수 있습니다.");
		}
		return room;
	}

	// DTO로 옮겨도 될지? 옮긴다면 같은 동작 사용하는 RoomService에서도 사용 가능
	private List<String> collectMemberLoginIds(long roomId, long userId) {
		ChatRoomDTO room = requireWorkRoomMember(roomId, userId);
		List<String> recipients = new ArrayList<>();

		for (UserDTO member : room.getMembers()) {
			recipients.add(member.getLoginId());
		}
		return recipients;
	}

	private String getLoginId(long userId) {
		UserDTO user = userDAO.findById(userId);
		if (user == null) {
			throw new ServiceException("사용자를 찾을 수 없습니다.");
		}
		return user.getLoginId();
	}
}
