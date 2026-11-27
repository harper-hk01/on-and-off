package dto.common;

/**
 * 접속 상태.
 * OFFLINE은 서버만 정한다. 클라이언트가 STATUS로 OFFLINE을 보내면 서버가 막아야 한다.
 */
public enum Status {
    ONLINE,   // 접속 중
    AWAY,     // 자리 비움
    OFFLINE   // 접속 안 함
}
