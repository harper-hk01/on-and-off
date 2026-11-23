package server.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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

	private static final long NUDGE_AFTER_MS = TimeUnit.MINUTES.toMillis(10);
	private static final long CHECK_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);

	private final RoomDAO roomDAO;
	private final UserDAO userDAO;
	private final SessionRegistry registry;

	// 방 번호 → 참여자 로그인 아이디 → 마지막 발언 시각
	private final ConcurrentHashMap<Long, Map<String, Long>> lastSpokenAt = new ConcurrentHashMap<>();

	// 방 번호 → 참여자 로그인 아이디 → 마지막 독려 시각
	private final ConcurrentHashMap<Long, Map<String, Long>> lastNudgedAt = new ConcurrentHashMap<>();

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
	public void startMeeting(long me, MeetingStartRequest req)  {
		long roomId = req.getRoomId();
		requireWorkRoomMember(roomId, me);

		String loginId = getLoginId(me);
		List<String> members = roomDAO.findMemberLoginIds(roomId);

		synchronized (this) {
			if (isMeeting(roomId)) {
				throw new ServiceException("이미 회의가 진행 중입니다.");
			}

			// 아직 발언하지 않은 사람은 회의 시작 시각부터 10분을 센다.
			long now = System.currentTimeMillis();
			Map<String, Long> spoken = new HashMap<>();
			for (String member : members) {
				spoken.put(member, now);
			}

			lastNudgedAt.put(roomId, new HashMap<>());
			lastSpokenAt.put(roomId, spoken);

			MeetingNoti notice = new MeetingNoti(roomId, MeetingState.START, loginId);
			registry.sendToAll(members, Command.MEETING_NOTI, notice);
		}
	}

	// 업무방 회의 종료 (MEETING_END)
	public void endMeeting(long me, MeetingEndRequest req)  {
		long roomId = req.getRoomId();
		requireWorkRoomMember(roomId, me);

		String loginId = getLoginId(me);
		List<String> members = roomDAO.findMemberLoginIds(roomId);

		synchronized (this) {
			if (!isMeeting(roomId)) {
				throw new ServiceException("진행 중인 회의가 없습니다.");
			}

			lastSpokenAt.remove(roomId);
			lastNudgedAt.remove(roomId);

			MeetingNoti notice = new MeetingNoti(roomId, MeetingState.END, loginId);
			registry.sendToAll(members, Command.MEETING_NOTI, notice);
		}
	}

	// RoomService가 ROOM_INFO를 만들 때 호출한다.
	public boolean isMeeting(long roomId) {
		return lastSpokenAt.containsKey(roomId);
	}

	// ChatService가 메시지를 DB에 저장한 뒤 호출한다.
	public void recordSpeech(long roomId, long userId)  {
		Map<String, Long> spoken = lastSpokenAt.get(roomId);
		if (spoken == null) {
			return;
		}

		String loginId = getLoginId(userId);

		synchronized (this) {
			// 사용자 조회 중 회의가 종료되거나 새 회의가 시작됐으면 건너뛴다.
			if (lastSpokenAt.get(roomId) == spoken) {
				spoken.replace(loginId, System.currentTimeMillis());
			}
		}
	}

	// ChatServer에서 서버 시작 시 한 번 호출한다.
	//불필요한 중복검사와 필드 제거
	public void startScheduler() {
    scheduler.scheduleWithFixedDelay(new Runnable() {
        @Override
        public void run() {
            checkSilence();
        }
    }, CHECK_INTERVAL_MS, CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
}

	// 마지막 발언 또는 독려 후 10분이 지난 접속자에게 독려 알림을 보낸다.
	private synchronized void checkSilence() {
		long now = System.currentTimeMillis();

		for (Map.Entry<Long, Map<String, Long>> room : lastSpokenAt.entrySet()) {
			long roomId = room.getKey();
			Map<String, Long> nudged = lastNudgedAt.get(roomId);

			for (Map.Entry<String, Long> member : room.getValue().entrySet()) {
				String loginId = member.getKey();
				long lastSpoken = member.getValue();
				long lastNudged = nudged.getOrDefault(loginId, 0L);

				if (now - Math.max(lastSpoken, lastNudged) < NUDGE_AFTER_MS) {
					continue;
				}

				try {
					if (registry.isOnline(loginId)) {
						NudgeNoti notice = new NudgeNoti(roomId, loginId);
						registry.sendTo(loginId, Command.NUDGE_NOTI, notice);
						nudged.put(loginId, now);
					}
				} catch (RuntimeException e) {
					//로그 방식에서 콘솔에만 남기는 방식으로 변경(설계와 필드 불일치를 고려하지 못함)
					//Dispatcher가 응답하는 오류가 아니므로 try-catch 방식 사용
   	 				System.err.println("회의 독려 전송 실패 (roomId=" + roomId
           				 + ", loginId=" + loginId + "): " + e.getMessage());
				}
			}
		}
	}

	private void requireWorkRoomMember(long roomId, long userId)  {
		if (!roomDAO.isMember(roomId, userId)) {
			throw new ServiceException("채팅방에 접근할 권한이 없습니다.");
		}

		ChatRoomDTO room = roomDAO.findRoom(roomId);
		if (room == null || room.getRoomType() != RoomType.WORK) {
			throw new ServiceException("회의는 업무방에서만 사용할 수 있습니다.");
		}
	}

	private String getLoginId(long userId)  {
		UserDTO user = userDAO.findById(userId);
		if (user == null) {
			throw new ServiceException("사용자를 찾을 수 없습니다.");
		}
		return user.getLoginId();
	}
}
