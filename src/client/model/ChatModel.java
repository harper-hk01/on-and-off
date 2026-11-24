package client.model;

import dto.common.ChatRoomDTO;
import dto.common.MessageDTO;
import dto.common.Status;
import dto.common.UserDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultListModel;

public class ChatModel {
	private UserDTO me;
	private final DefaultListModel<UserDTO> friends = new DefaultListModel<>();
	private final DefaultListModel<ChatRoomDTO> rooms = new DefaultListModel<>();
	private final Map<Long, List<MessageDTO>> messages = new HashMap<>();
	private final Set<Long> meetingRooms = new HashSet<>();

	public void setMe(UserDTO me) {
		this.me = me;
	}

	public UserDTO getMe() {
		return me;
	}

	// 친구 목록

	public void setFriends(List<UserDTO> list) {
		friends.clear();
		friends.addAll(list);
	}

	public void addFriend(UserDTO user) {
		friends.addElement(user);
	}

	public void updateFriend(UserDTO user) {
		int index = friendIndex(user.getLoginId());
		if (index >= 0) {
			friends.set(index, user);
		}
	}

	public void updateFriendStatus(String loginId, Status status) {
		int index = friendIndex(loginId);
		if (index >= 0) {
			UserDTO friend = friends.get(index);
			friend.setStatus(status);

			// 목록에 변경 이벤트를 발생시켜 JList에 상태 변경을 반영한다.
			friends.set(index, friend);
		}
	}

	public DefaultListModel<UserDTO> getFriends() {
		return friends;
	}

	// 로그인 ID로 친구 목록의 인덱스를 찾는 반복문을 모듈화함
	private int friendIndex(String loginId) {
		for (int i = 0; i < friends.size(); i++) {
			if (loginId.equals(friends.get(i).getLoginId())) {
				return i;
			}
		}
		return -1;
	}

	// 채팅방 목록

	public void setRooms(List<ChatRoomDTO> list) {
		rooms.clear();
		rooms.addAll(list);
	}

	public void addRoom(ChatRoomDTO room) {
		rooms.addElement(room);
	}

	public void setRoomNew(long roomId, boolean hasNew) {
		int index = roomIndex(roomId);
		if (index >= 0) {
			ChatRoomDTO room = rooms.get(index);
			room.setHasNew(hasNew);
			rooms.set(index, room);
		}
	}

	public DefaultListModel<ChatRoomDTO> getRooms() {
		return rooms;
	}

	public ChatRoomDTO getRoom(long roomId) {
		int index = roomIndex(roomId);

		if (index < 0) {
			return null;
		}

		return rooms.get(index);
	}

	// 방 번호로 채팅방 목록의 인덱스를 찾는 반복문을 모듈화함
	private int roomIndex(long roomId) {
		for (int i = 0; i < rooms.size(); i++) {
			if (rooms.get(i).getRoomId() == roomId) {
				return i;
			}
		}
		return -1;
	}

	// 방별 메시지와 공지

	public void setMessages(long roomId, List<MessageDTO> list) {
		messages.put(roomId, new ArrayList<>(list));
	}

	public void addMessage(MessageDTO msg) {
		long roomId = msg.getRoomId();
		List<MessageDTO> history = messages.get(roomId);

		// 이 방의 첫 메시지를 받으면 대화 기록을 만든다.
		if (history == null) {
			history = new ArrayList<>();
			messages.put(roomId, history);
		}

		history.add(msg);
	}

	public List<MessageDTO> getMessages(long roomId) {
		List<MessageDTO> history = messages.get(roomId);

		if (history == null) {
			return new ArrayList<>();
		}

		return new ArrayList<>(history);
	}

	public void setPinnedMessage(long roomId, MessageDTO msg) {
		int index = roomIndex(roomId);
		if (index >= 0) {
			ChatRoomDTO room = rooms.get(index);
			room.setPinnedMessage(msg);
			rooms.set(index, room);
		}
	}

	// 회의 상태

	public void setMeetingActive(long roomId, boolean active) {
		if (active) {
			meetingRooms.add(roomId);
		} else {
			meetingRooms.remove(roomId);
		}
	}

	public boolean isMeetingActive(long roomId) {
		return meetingRooms.contains(roomId);
	}

	public void clear() {
		me = null;
		friends.clear();
		rooms.clear();
		messages.clear();
		meetingRooms.clear();
	}
}