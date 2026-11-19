package dto.response;

/**
 * NUDGE_NOTI (S→C): 참여 독려 알림.
 */
public class NudgeNoti {

    private long roomId;      // 방 번호
    private String targetId;  // 독려 대상의 로그인 아이디

    public NudgeNoti() {
    }

    public NudgeNoti(long roomId, String targetId) {
        this.roomId = roomId;
        this.targetId = targetId;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    @Override
    public String toString() {
        return "NudgeNoti{roomId=" + roomId
                + ", targetId=" + targetId + "}";
    }
}
