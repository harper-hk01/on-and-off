package server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dto.common.MessageDTO;
import server.exception.DAOException;

/**
 * messages 테이블 SQL만 맡는 클래스.
 *
 * - DB의 sender_id는 숫자 번호, MessageDTO.senderId는 로그인 아이디 문자열이다.
 *   그래서 꺼낼 때 users와 조인해서 login_id와 nickname을 같이 가져온다.
 * - sent_at(DATETIME)은 DATE_FORMAT으로 'yyyy-MM-dd HH:mm' 문자열로 바꿔서 꺼낸다
 *   (Protocol.TIME_FORMAT과 같은 모양).
 * - 버즈·타이핑·참여 독려는 저장하지 않는다. 일반 채팅만 저장.
 */
public class MessageDAO {

    /** 메시지를 꺼낼 때 공통으로 쓰는 SELECT 앞부분 (users와 조인) */
    private static final String SELECT_MESSAGE =
            "SELECT m.message_id, m.room_id, "
          + "       u.login_id AS sender_login_id, u.nickname AS sender_nickname, "
          + "       m.content, DATE_FORMAT(m.sent_at, '%Y-%m-%d %H:%i') AS sent_at_text "
          + "FROM messages m "
          + "JOIN users u ON u.user_id = m.sender_id ";

    /**
     * CHAT: 메시지를 저장하고, 새로 만들어진 메시지 번호(message_id)를 돌려준다.
     *
     * message_id는 AUTO_INCREMENT라서 MySQL이 정한다. 그 번호를 받으려면
     * prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)로 만들고
     * 실행 후 getGeneratedKeys()로 꺼낸다.
     * sent_at은 DB 기본값(지금 시각)이 자동으로 들어간다.
     */
    public long insert(long roomId, long senderId, String content) {
        String sql = "INSERT INTO messages (room_id, sender_id, content) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, roomId);
            ps.setLong(2, senderId);
            ps.setString(3, content);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);            // 방금 만들어진 message_id
                }
                throw new SQLException("메시지 번호를 받지 못했습니다.");
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: MessageDAO.insert", e);
        }
    }

    /**
     * 메시지 번호로 메시지 하나를 꺼낸다. 없으면 null.
     * CHAT 저장 직후 CHAT_RECV로 보낼 MessageDTO를 만들 때,
     * 공지(pinnedMessage)를 꺼낼 때 쓴다.
     */
    public MessageDTO findById(long messageId) {
        String sql = SELECT_MESSAGE + "WHERE m.message_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, messageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toMessageDTO(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: MessageDAO.findById", e);
        }
    }

    /**
     * ROOM_INFO: 그 방의 최근 메시지 limit개를 꺼낸다 (보통 Protocol.HISTORY_LIMIT = 50).
     *
     * "최근 50개"를 고르려고 번호 내림차순(DESC)으로 꺼낸 뒤,
     * 화면에는 오래된 것부터 보여야 하므로 Collections.reverse()로 뒤집어서 돌려준다.
     */
    public List<MessageDTO> findRecent(long roomId, int limit) {
        String sql = SELECT_MESSAGE
                   + "WHERE m.room_id = ? "
                   + "ORDER BY m.message_id DESC "
                   + "LIMIT ?";
        List<MessageDTO> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(toMessageDTO(rs));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: MessageDAO.findRecent", e);
        }
        Collections.reverse(list); // 최신순 → 오래된 순
        return list;
    }

    /**
     * NOTICE_SET 전에: 그 메시지가 정말 그 방의 메시지인지 확인한다.
     * 다른 방 메시지를 공지로 고정하지 못하게 막는 용도.
     */
    public boolean existsInRoom(long messageId, long roomId) {
        String sql = "SELECT COUNT(*) FROM messages WHERE message_id = ? AND room_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, messageId);
            ps.setLong(2, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: MessageDAO.existsInRoom", e);
        }
    }

    /** ResultSet의 현재 줄 → MessageDTO */
    private MessageDTO toMessageDTO(ResultSet rs) throws SQLException {
        return new MessageDTO(
                rs.getLong("message_id"),
                rs.getLong("room_id"),
                rs.getString("sender_login_id"),       // 숫자 번호가 아니라 로그인 아이디
                rs.getString("sender_nickname"),
                rs.getString("content"),
                rs.getString("sent_at_text"));         // 'yyyy-MM-dd HH:mm'
    }
}
