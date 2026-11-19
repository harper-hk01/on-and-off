package dto.response;

import dto.common.MeetingState;

/**
 * MEETING_NOTI (S→C): 이 사람이 회의를 시작/종료했어요.
 */
public class MeetingNoti {

    private long roomId;         // 방 번호
    private MeetingState state;  // START(시작) 또는 END(종료)
    private String loginId;      // 회의를 시작/종료한 사람의 로그인 아이디

    public MeetingNoti() {
    }

    public MeetingNoti(long roomId, MeetingState state, String loginId) {
        this.roomId = roomId;
        this.state = state;
        this.loginId = loginId;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public MeetingState getState() {
        return state;
    }

    public void setState(MeetingState state) {
        this.state = state;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    @Override
    public String toString() {
        return "MeetingNoti{roomId=" + roomId
                + ", state=" + state
                + ", loginId=" + loginId + "}";
    }
}
