package dto.response;

import dto.common.MessageDTO;

/**
 * NOTICE_NOTI (S→C): 공지가 바뀌었어요.
 */
public class NoticeNoti {

    private long roomId;               // 방 번호
    private MessageDTO pinnedMessage;  // 새 공지. 해제되었으면 null

    public NoticeNoti() {
    }

    public NoticeNoti(long roomId, MessageDTO pinnedMessage) {
        this.roomId = roomId;
        this.pinnedMessage = pinnedMessage;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public MessageDTO getPinnedMessage() {
        return pinnedMessage;
    }

    public void setPinnedMessage(MessageDTO pinnedMessage) {
        this.pinnedMessage = pinnedMessage;
    }

    @Override
    public String toString() {
        return "NoticeNoti{roomId=" + roomId
                + ", pinnedMessage=" + pinnedMessage + "}";
    }
}
