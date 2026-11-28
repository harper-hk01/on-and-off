package server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import dto.common.Status;
import dto.common.UserDTO;
import server.exception.DAOException;

///users 테이블 SQL만 맡는 클래스.

public class UserDAO {

    ///ID_CHECK: 같은 아이디가 몇 개 있는지 센다. 0이면 사용 가능.
    public int countByLoginId(String loginId) {
        String sql = "SELECT COUNT(*) FROM users WHERE login_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loginId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();                 // COUNT(*)는 항상 한 줄이 나온다
                return rs.getInt(1);       // 첫 번째 칸의 값
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: UserDAO.countByLoginId", e);
        }
    }

    /**
     * JOIN: 새 회원을 넣는다.
     * user_id, status('OFFLINE'), created_at은 DB 기본값이 자동으로 들어간다.
     * 같은 아이디가 이미 있으면 MySQL이 거부하고 SQLException(오류 번호 1062)이 난다.
     */
    public void insert(String loginId, String password, String nickname) {
        String sql = "INSERT INTO users (login_id, password, nickname) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loginId);
            ps.setString(2, password);
            ps.setString(3, nickname);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("DB 오류: UserDAO.insert", e);
        }
    }

    /// 회원 번호(user_id)로 회원 한 명을 꺼낸다. 없으면 null.
    public UserDTO findById(long userId) {
        String sql = "SELECT login_id, nickname, status_message, status FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return toUserDTO(rs);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: UserDAO.findById", e);
        }
    }

    public Long findUserId(String loginId) {
        String sql = "SELECT user_id FROM users WHERE login_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loginId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("user_id");
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: UserDAO.findUserId", e);
        }
    }

    public String findPassword(String loginId) {
        String sql = "SELECT password FROM users WHERE login_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, loginId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password");
                }
                return null;
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: UserDAO.findPassword", e);
        }
    }

    public int updateStatus(long userId, Status status) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());   // enum → "ONLINE" 같은 문자열
            ps.setLong(2, userId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("DB 오류: UserDAO.updateStatus", e);
        }
    }

    public int updateAllStatus(Status status) {
        String sql = "UPDATE users SET status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("DB 오류: UserDAO.updateAllStatus", e);
        }
    }

    public int updateProfile(long userId, String nickname, String statusMessage) {
        String sql = "UPDATE users SET nickname = ?, status_message = ? WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nickname);
            ps.setString(2, statusMessage);   // null이면 DB에 NULL이 들어간다
            ps.setLong(3, userId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("DB 오류: UserDAO.updateProfile", e);
        }
    }

    private UserDTO toUserDTO(ResultSet rs) throws SQLException {
        return new UserDTO(
                rs.getString("login_id"),
                rs.getString("nickname"),
                rs.getString("status_message"),
                Status.valueOf(rs.getString("status")));
    }
}
