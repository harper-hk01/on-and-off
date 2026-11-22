package server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import dto.common.*;

/**
 * chat_rooms, room_members 테이블 SQL을 맡는 클래스.
 *
 * - 1:1 방은 DAILY, 그룹방은 WORK (팀 결정). 1:1 방은 room_name이 NULL.
 * - 회의 중 여부는 DB에 없다 (MeetingService 메모리에서 관리).
 * - hasNew(NEW 배지)는 저장하지 않고 findMyRooms에서 그때그때 계산한다.
 * - pinnedMessage(공지 MessageDTO)는 이 클래스에서 채우지 않는다.
 *   RoomService가 findPinnedMessageId()로 번호를 받고 MessageDAO.findById()로 꺼내서 넣는다.
 */
public class RoomDAO {

    /**
     * ROOM_OPEN: 나와 친구가 둘 다 들어 있는 DAILY(1:1) 방 번호를 찾는다. 없으면 null.
     * room_members를 두 번(a, b) 조인해서 "나도 있고 친구도 있는 방"을 고른다.
     */
    public Long findDirectRoomId(long meId, long friendId) {
        String sql = "SELECT r.room_id "
                   + "FROM chat_rooms r "
                   + "JOIN room_members a ON a.room_id = r.room_id AND a.user_id = ? "
                   + "JOIN room_members b ON b.room_id = r.room_id AND b.user_id = ? "
                   + "WHERE r.room_type = 'DAILY' "
                   + "LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, meId);
            ps.setLong(2, friendId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("room_id");
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: RoomDAO.findDirectRoomId", e);
        }
    }

    /**
     * ROOM_CREATE(그룹방) / ROOM_OPEN에서 새 1:1 방: 방을 만들고 참여자를 넣은 뒤 새 방 번호를 돌려준다.
     *
     * @param type      DAILY(1:1) 또는 WORK(그룹)
     * @param roomName  그룹방 이름. 1:1 방이면 null
     * @param memberIds 참여자 회원 번호 목록 (나 포함!)
     *
     * 방 만들기 + 참여자 여러 줄 넣기는 "전부 성공 아니면 전부 취소"여야 하므로 트랜잭션으로 묶는다.
     * 방 번호는 AUTO_INCREMENT라서 getGeneratedKeys()로 받는다.
     */
    public long createRoom(RoomType type, String roomName, List<Long> memberIds) {
        String roomSql = "INSERT INTO chat_rooms (room_type, room_name) VALUES (?, ?)";
        String memberSql = "INSERT INTO room_members (room_id, user_id) VALUES (?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);                         // 1) 자동 저장 끄기
            try {
                // 2) 방 만들기
                long newRoomId;
                try (PreparedStatement ps = conn.prepareStatement(roomSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, type.name());              // enum → "DAILY" / "WORK"
                    ps.setString(2, roomName);                 // null이면 NULL
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("방 번호를 받지 못했습니다.");
                        }
                        newRoomId = keys.getLong(1);
                    }
                }

                // 3) 참여자마다 한 줄씩 넣기
                //    addBatch()로 모아 두었다가 executeBatch()로 한 번에 보낸다.
                try (PreparedStatement ps = conn.prepareStatement(memberSql)) {
                    for (Long userId : memberIds) {
                        ps.setLong(1, newRoomId);
                        ps.setLong(2, userId);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();                                 // 4) 모두 성공 → 저장
                return newRoomId;
            } catch (SQLException e) {
                conn.rollback();                               // 5) 하나라도 실패 → 모두 취소
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: RoomDAO.createRoom", e);
        }
    }

    /**
     * ROOM_ENTER 등: 내가 그 방의 참여자인지 확인한다. 아니면 RoomService가 ERROR(권한 없음)로 보낸다.
     */
    public boolean isMember(long roomId, long userId) {
        String sql = "SELECT COUNT(*) FROM room_members WHERE room_id = ? AND user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: RoomDAO.isMember", e);
        }
    }

    /**
     * ROOM_INFO / ROOM_INVITED: 방 하나의 정보를 ChatRoomDTO로 꺼낸다. 없으면 null.
     *
     * 채우는 것: roomId, roomType, roomName, members(참여자 목록)
     * 채우지 않는 것: pinnedMessage(null) → RoomService가 findPinnedMessageId + MessageDAO.findById로 채움
     *               hasNew(false)        → 방 하나만 볼 때는 쓰지 않음
     */
    public ChatRoomDTO findRoom(long roomId) {
        String sql = "SELECT room_id, room_type, room_name FROM chat_rooms WHERE room_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                List<UserDTO> members = findMembers(conn, roomId);   // 같은 연결로 참여자도 조회
                return new ChatRoomDTO(
                        rs.getLong("room_id"),
                        RoomType.valueOf(rs.getString("room_type")),
                        rs.getString("room_name"),
                        members,
                        null,       // pinnedMessage: RoomService가 채움
                        false);     // hasNew
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: RoomDAO.findRoom", e);
        }
    }

    /**
     * 그 방의 공지 메시지 번호를 꺼낸다. 공지가 없거나 방이 없으면 null.
     */
    public Long findPinnedMessageId(long roomId) {
        String sql = "SELECT pinned_message_id FROM chat_rooms WHERE room_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("pinned_message_id");
                    // getLong은 NULL을 0으로 돌려주므로, wasNull()로 진짜 NULL이었는지 확인한다.
                    return rs.wasNull() ? null : id;
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: RoomDAO.findPinnedMessageId", e);
        }
    }

    /**
     * 방 참여자 목록을 UserDTO로 꺼낸다 (나 포함).
     */
    public List<UserDTO> findMembers(long roomId) {
        try (Connection conn = DBConnection.getConnection()) {
            return findMembers(conn, roomId);
        } catch (SQLException e) {
            throw new DAOException("DB 오류: RoomDAO.findMembers", e);
        }
    }

    /**
     * CHAT_RECV·TYPING_NOTI 등을 방 사람들에게 보낼 때: 참여자의 로그인 아이디만 꺼낸다.
     * SessionRegistry.sendToAll(loginIds, ...)에 그대로 넘기면 된다.
     */
    public List<String> findMemberLoginIds(long roomId) {
        String sql = "SELECT u.login_id "
                   + "FROM room_members rm "
                   + "JOIN users u ON u.user_id = rm.user_id "
                   + "WHERE rm.room_id = ?";
        List<String> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("login_id"));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: RoomDAO.findMemberLoginIds", e);
        }
        return list;
    }

    /**
     * ROOM_LIST: 내가 들어간 방 목록을 hasNew까지 계산해서 꺼낸다.
     *
     * hasNew = "내가 마지막으로 읽은 번호보다 큰, 남이 보낸 메시지가 하나라도 있으면" true
     * - EXISTS (...)      : 괄호 안 조건에 맞는 줄이 하나라도 있으면 1, 없으면 0
     * - COALESCE(값, 0)   : 한 번도 안 읽어서 NULL이면 0번까지 읽은 것으로 침
     * - sender_id <> 나   : 내가 보낸 메시지는 NEW로 치지 않음
     *
     * 1:1 방은 이름이 없어서 화면에 상대 이름을 보여 줘야 하므로 members도 채운다.
     * pinnedMessage는 null (방에 들어갈 때 ROOM_INFO로 보냄).
     */
    public List<ChatRoomDTO> findMyRooms(long userId) {
        String sql = "SELECT r.room_id, r.room_type, r.room_name, "
                   + "       EXISTS ( "
                   + "         SELECT 1 FROM messages m "
                   + "         WHERE m.room_id = r.room_id "
                   + "           AND m.message_id > COALESCE(rm.last_read_message_id, 0) "
                   + "           AND m.sender_id <> rm.user_id "
                   + "       ) AS has_new "
                   + "FROM room_members rm "
                   + "JOIN chat_rooms r ON r.room_id = rm.room_id "
                   + "WHERE rm.user_id = ? "
                   + "ORDER BY r.room_id DESC";
        List<ChatRoomDTO> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long roomId = rs.getLong("room_id");
                    list.add(new ChatRoomDTO(
                            roomId,
                            RoomType.valueOf(rs.getString("room_type")),
                            rs.getString("room_name"),
                            findMembers(conn, roomId),
                            null,
                            rs.getBoolean("has_new")));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: RoomDAO.findMyRooms", e);
        }
        return list;
    }

    /**
     * ROOM_READ: 이 방에서 어디까지 읽었는지 저장한다.
     * 더 작은 번호가 늦게 도착해도 뒤로 가지 않도록 "지금보다 클 때만" 바꾼다.
     * 돌려주는 값 = 바뀐 줄 수 (0이면 이미 더 뒤까지 읽었거나 참여자가 아님).
     */
    public int updateLastRead(long roomId, long userId, long lastReadMessageId) {
        String sql = "UPDATE room_members "
                   + "SET last_read_message_id = ? "
                   + "WHERE room_id = ? AND user_id = ? "
                   + "  AND (last_read_message_id IS NULL OR last_read_message_id < ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, lastReadMessageId);
            ps.setLong(2, roomId);
            ps.setLong(3, userId);
            ps.setLong(4, lastReadMessageId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("DB 오류: RoomDAO.updateLastRead", e);
        }
    }

    /**
     * NOTICE_SET: 공지 메시지를 고정한다. messageId가 null이면 공지를 해제한다(NULL 저장).
     * 그 메시지가 이 방 것인지는 RoomService가 MessageDAO.existsInRoom()으로 먼저 확인한다.
     * 돌려주는 값 = 바뀐 줄 수.
     */
    public int updatePinnedMessage(long roomId, Long messageId) {
        String sql = "UPDATE chat_rooms SET pinned_message_id = ? WHERE room_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (messageId == null) {
                ps.setNull(1, Types.BIGINT);           // 공지 해제
            } else {
                ps.setLong(1, messageId);
            }
            ps.setLong(2, roomId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("DB 오류: RoomDAO.updatePinnedMessage", e);
        }
    }

    /**
     * 이미 열린 연결(conn)로 참여자 목록을 꺼내는 내부용 메서드.
     * findRoom, findMyRooms처럼 한 메서드 안에서 여러 번 조회할 때 연결을 새로 만들지 않으려고 둔다.
     */
    private List<UserDTO> findMembers(Connection conn, long roomId) throws SQLException {
        String sql = "SELECT u.login_id, u.nickname, u.status_message, u.status "
                   + "FROM room_members rm "
                   + "JOIN users u ON u.user_id = rm.user_id "
                   + "WHERE rm.room_id = ?";
        List<UserDTO> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new UserDTO(
                            rs.getString("login_id"),
                            rs.getString("nickname"),
                            rs.getString("status_message"),
                            Status.valueOf(rs.getString("status"))));
                }
            }
        }
        return list;
    }
}
