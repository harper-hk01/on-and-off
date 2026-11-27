package server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import dto.common.*;

/**
 * friends 테이블 SQL만 맡는 클래스.
 *
 * - 친구 관계는 양방향 두 줄로 저장한다 (팀 결정).
 *   chulsoo(1)가 younghee(2)를 추가하면 (1, 2), (2, 1) 두 줄이 들어간다.
 * - findFriends는 users와 조인하지만 "친구 목록"을 위한 SQL이라서 여기에 둔다.
 * - "자기 자신을 친구로 추가"와 "없는 사람" 검사는 FriendService가 먼저 한다.
 */
public class FriendDAO {

    /**
     * FRIEND_ADD 전에: 이미 친구인지 확인한다. 이미 친구면 true.
     */
    public boolean isFriend(long meId, long friendId) {
        String sql = "SELECT COUNT(*) FROM friends WHERE user_id = ? AND friend_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, meId);
            ps.setLong(2, friendId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: FriendDAO.isFriend", e);
        }
    }

    /**
     * FRIEND_ADD: 두 줄(나→친구, 친구→나)을 한 번에 넣는다.
     *
     * 트랜잭션(묶음 처리): 두 줄 중 하나만 들어가면 "한쪽만 친구"인 이상한 상태가 된다.
     * 그래서 setAutoCommit(false)로 자동 저장을 끄고, 둘 다 성공하면 commit(),
     * 중간에 오류가 나면 rollback()으로 둘 다 취소한다.
     */
    public void insertBoth(long meId, long friendId) {
        String sql = "INSERT INTO friends (user_id, friend_id) VALUES (?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false); //자동 저장 끄기
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                // 나 → 친구
                ps.setLong(1, meId);
                ps.setLong(2, friendId);
                ps.executeUpdate();

                // 친구 → 나 (같은 PreparedStatement에 값만 바꿔서 한 번 더)
                ps.setLong(1, friendId);
                ps.setLong(2, meId);
                ps.executeUpdate();

                conn.commit(); // 둘 다 성공 → 저장
            } catch (SQLException e) {
                conn.rollback();// 하나라도 실패 → 둘 다 취소
                throw e; //    바깥 catch에서 DAOException으로 감싸서 던진다
            } finally {
                conn.setAutoCommit(true);// 원래대로 돌려 두기
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: FriendDAO.insertBoth", e);
        }
    }

    /**
     * FRIEND_LIST: 내 친구 목록을 UserDTO로 꺼낸다.
     * 접속 중(ONLINE·AWAY)인 친구가 위로, 같은 그룹 안에서는 닉네임 순.
     * (u.status = 'OFFLINE')은 오프라인이면 1, 아니면 0이라서 오름차순이면 접속 중이 먼저 나온다.
     */
    public List<UserDTO> findFriends(long meId) {
        String sql = "SELECT u.login_id, u.nickname, u.status_message, u.status "
                   + "FROM friends f "
                   + "JOIN users u ON u.user_id = f.friend_id "
                   + "WHERE f.user_id = ? "
                   + "ORDER BY (u.status = 'OFFLINE'), u.nickname";
        List<UserDTO> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, meId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) { // 친구 수만큼 반복
                    list.add(new UserDTO(
                            rs.getString("login_id"),
                            rs.getString("nickname"),
                            rs.getString("status_message"),
                            Status.valueOf(rs.getString("status"))));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: FriendDAO.findFriends", e);
        }
        return list; // 친구가 없으면 빈 목록 (null 아님)
    }

    /**
     * STATUS_NOTI·PROFILE_NOTI 보낼 때: 내 친구들의 로그인 아이디만 꺼낸다.
     * SessionRegistry.sendToAll(loginIds, ...)에 그대로 넘기면 된다.
     */
    public List<String> findFriendLoginIds(long meId) {
        String sql = "SELECT u.login_id "
                   + "FROM friends f "
                   + "JOIN users u ON u.user_id = f.friend_id "
                   + "WHERE f.user_id = ?";
        List<String> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, meId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("login_id"));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("DB 오류: FriendDAO.findFriendLoginIds", e);
        }
        return list;
    }
}
