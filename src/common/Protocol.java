package common;

/// 서버와 클라이언트가 함께 지키는 약속 모음. 
public final class Protocol {
  private Protocol() { }

  // 1. 기본 통신 설정
  public static final String HOST = "10.240.149.3"; //나중에 서버 PC의 IP로 바꾸기
  public static final int PORT = 50023;
  public static final String CHARSET = "UTF-8";


  // 2. 입력 규칙 (화면 / 서버 모두 검사, DB 칸 크기와 같음)
  public static final int MAX_LOGIN_ID = 20;
  public static final int MAX_PASSWORD = 20;
  public static final int MAX_NICKNAME = 20;
  public static final int MAX_STATUS_MESSAGE = 100;
  public static final int MAX_ROOM_NAME = 50;

  /* 아이디는 영문/숫자만 허용 */
  public static final String LOGIN_ID_PATTERN = "^[A-Za-z0-9]+$";
  /* 비밀번호는 숫자만 허용 */
  public static final String PASSWORD_PATTERN = "^[0-9]+$";
  
  // 3. 데이터 형식  
  /* 시각 표기: MessageDTO.sentAt 등 */
  public static final String TIME_FORMAT = "yyyy-MM-dd HH:mm";
  /* 방을 열 때 보내는 이전 대화 수 (ROOM_INFO.messages) */
  public static final int HISTORY_LIMIT = 50;
  
}

