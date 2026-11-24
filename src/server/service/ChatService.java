
package server.service;

import java.util.ArrayList;
import java.util.List;

import common.Command;
import dto.common.MessageDTO;
import dto.common.UserDTO;
import dto.request.ChatRequest;
import dto.request.TypingRequest;
import dto.request.BuzzRequest;
import dto.response.TypingNoti;
import dto.response.BuzzRecv;
import server.dao.MessageDAO;
import server.dao.RoomDAO;
import server.dao.UserDAO;
import server.session.SessionRegistry;

/**
 * 채팅 관련 기능을 담당하는 클래스.
 *
 * - 채팅 메시지 전송
 * - 입력 중 상태 알림
 * - 버즈 알림
 */
public class ChatService {

    private final MessageDAO messageDAO;
    private final RoomDAO roomDAO;
    private final UserDAO userDAO;
    private final MeetingService meetingService; //얘를 가진 이유는?
    private final SessionRegistry registry;

    /**
     * 생성자
     */
    
    public ChatService(MessageDAO messageDAO,
                       RoomDAO roomDAO,
                       UserDAO userDAO,
                       MeetingService meetingService,
                       SessionRegistry registry) {

        this.messageDAO = messageDAO;
        this.roomDAO = roomDAO;
        this.userDAO = userDAO;
        this.meetingService = meetingService;
        this.registry = registry;
    }

    /**
     * CHAT: 채팅 메시지 전송
     *
     * @param me 메시지를 보내는 사용자 번호 (DB에서 user_id)
     * @param req 채팅 요청 정보
     */
    public void sendChat(long me, ChatRequest req) {

        // 0(1). 요청 검증
        if (req == null) {
            throw new ServiceException("채팅 요청이 없습니다.");
        }
        
        long roomId = req.getRoomId(); //아래에 필요한 데이터들 ChatRequest안에있는거 가져오기
        String content = req.getContent();

        //0(2). 메시지 내용 검증
        validateContent(content);

        //1. 요청한 사용자가 해당 방의 멤버인지 확인  -> 진짜 필요한가?
        if (!roomDAO.isMember(roomId, me)) {
            throw new ServiceException( //잘못된 인자나 입력값이 전달됐을 때 예외를 직접 발생
                    "채팅방에 참여한 사용자만 메시지를 보낼 수 있습니다.");
        }
        
        //2. 사용자 정보 조회
        UserDTO sender = userDAO.findById(me);

        if (sender == null) {
            throw new ServiceException("사용자를 찾을 수 없습니다.");
        }
        
        //3. 메시지를 DB에 저장 + 생성된 메시지 번호 가져오기
        long messageId = messageDAO.insert(roomId, me, content);
        
        //4. MessageDAO로 저장된 메시지 정보를 조회해서 조회한 결과를 MessageDTO로 가져온다.
        MessageDTO messageDTO = messageDAO.findById(messageId);
        
        //5. 보낼 대상 정리 (메시지를 받을 채팅방 참여자들의 로그인 ID 조회 + 보낸 당사자는 제거)
        List<String> memberLoginIds = new ArrayList<>(roomDAO.findMemberLoginIds(roomId));//채팅방의 다른 사용자들 불러오기
        
        memberLoginIds.remove(sender.getLoginId());
        
        //6. SessionRegistry를 통해 채팅방의 다른 사용자들에게 메시지 전달
        registry.sendToAll(memberLoginIds, Command.CHAT_RECV, messageDTO);
    }

    /**
     * TYPING: 사용자가 메시지를 입력 중임을 알린다
     * 일상모드(1:1)에서만 일어난다.
     */
    public void typing(long me, TypingRequest req) {

        // 0. 요청 검증
        if (req == null) {
            throw new ServiceException("입력 중 알림 요청이 없습니다.");
        }

        long roomId = req.getRoomId(); //아래에서 필요한 데이터 가져오기

        // 1. 요청한 사용자가 해당 채팅방 멤버인지 확인
        if (!roomDAO.isMember(roomId, me)) {
            throw new ServiceException("채팅방 멤버가 아닙니다.");
        }

        // 2. 사용자 정보 조회
        UserDTO sender = userDAO.findById(me);

        if (sender == null) {
            throw new ServiceException("사용자 정보를 찾을 수 없습니다.");
        }

        // 3. 보낼 대상 정리 (메시지를 받을 채팅방 참여자들의 로그인 ID 조회 + 보낸 당사자는 제거)
        //일상모드의 나중의 그룹채팅의 확장을 위해 list로 처리
        List<String> memberLoginIds =
                new ArrayList<>(roomDAO.findMemberLoginIds(roomId));

        memberLoginIds.remove(sender.getLoginId());

        // 4. 입력 중 알림 데이터 생성
        TypingNoti noti = new TypingNoti(roomId, sender.getLoginId());

        // 5. 나머지 채팅방 참여자들에게 알림 전송
        registry.sendToAll(memberLoginIds, Command.TYPING_NOTI, noti);
    }

    /**
     * BUZZ: 채팅방 참여자(들)에게 버즈를 보낸다.
     * 일상모드(1:1)에서만 일어난다.
     */
	public void buzz(long me, BuzzRequest req) {

	    // 0. 요청 검증
	    if (req == null) {
	        throw new ServiceException("버즈 요청이 없습니다.");
	    }

	    long roomId = req.getRoomId(); //아래에서 필요한 데이터 가져오기

	    // 1. 요청한 사용자가 채팅방 멤버인지 확인
	    if (!roomDAO.isMember(roomId, me)) {
	        throw new ServiceException("채팅방에 참여한 사용자만 버즈를 보낼 수 있습니다.");
	    }

	    // 2. 사용자 정보 조회
	    UserDTO sender = userDAO.findById(me);

	    if (sender == null) {
	        throw new ServiceException("사용자를 찾을 수 없습니다.");
	    }

	    // 3. 보낼 대상 정리 (메시지를 받을 채팅방 참여자들의 로그인 ID 조회 + 보낸 당사자는 제거)
	    List<String> memberLoginIds =
	            new ArrayList<>(roomDAO.findMemberLoginIds(roomId));

	    memberLoginIds.remove(sender.getLoginId());

	    // 4. 버즈 알림데이터 생성
	    BuzzRecv noti = new BuzzRecv(roomId, sender.getLoginId());

	    //5. 상대방에게 버즈 알림 전송
	    registry.sendToAll(memberLoginIds, Command.BUZZ_RECV, noti);
    }


    /**
     * 메시지 내용 검증
     *
     * @param content 메시지 내용
     */
    private void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new ServiceException("메시지 내용이 비어있습니다.");
        }
    }
}
