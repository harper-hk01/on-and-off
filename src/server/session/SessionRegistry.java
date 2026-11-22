package server.session;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import common.Command;

public class SessionRegistry {
	// private final 밖에서 명단을 직접 수정하거나 명단 자체가 다른 명단으로 교체도는 것을 막는다 
	private final ConcurrentHashMap<String, ClientSession> sessions = new ConcurrentHashMap<>();
	
	/*명부에 세션 등록*/
	public boolean register(String loginId, ClientSession session) {
		return sessions.putIfAbsent(loginId, session) == null; // 이미 접속 중이 아닐 때만
	}
	/*명부에서 세션 제거*/
	public void unregister(String loginId, ClientSession session) {
		sessions.remove(loginId, session); // 키와 밸류 값이 일치할 때 세션삭제
	}
	/*접속 중인지 명부에서 확인*/
	public boolean isOnline(String loginId) {
		return sessions.containsKey(loginId);
	}
	/*한 명에게 보내기*/
	public void sendTo(String loginId, Command type, Object data){
		ClientSession session = sessions.get(loginId);
		if ( session != null) {
			session.send(type,data);
		}
	}
	/*여러명에게 보내기*/
	public void sendToAll(List<String> loginIds, Command type, Object data) {
		for (String user : loginIds)
			sendTo(user, type, data);
	}
}