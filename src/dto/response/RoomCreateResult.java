package dto.response;

import dto.common.ChatRoomDTO;
import dto.common.ResultDTO;

/**
 * ROOM_CREATE_RESULT (S→C): 만들어진 방.
 * success, message는 ResultDTO에서 물려받는다. 실패하면 room은 null.
 */
public class RoomCreateResult extends ResultDTO {

    private ChatRoomDTO room;  // 만들어진 방

    public RoomCreateResult() {
    }

    public RoomCreateResult(boolean success, String message, ChatRoomDTO room) {
        super(success, message);
        this.room = room;
    }

    public ChatRoomDTO getRoom() {
        return room;
    }

    public void setRoom(ChatRoomDTO room) {
        this.room = room;
    }

    @Override
    public String toString() {
        return "RoomCreateResult{success=" + isSuccess()
                + ", message=" + getMessage()
                + ", room=" + room + "}";
    }
}
