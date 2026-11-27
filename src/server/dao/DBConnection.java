package server.dao;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DB 연결을 빌려주는 도구 클래스
 *
 * - DB 주소·계정·비밀번호는 코드에 직접 쓰지 않고 프로젝트 폴더의 db.properties 파일에서 읽는다.
 *   (db.properties는 .gitignore에 들어 있어서 각자 자기 PC의 MySQL 비밀번호를 적어 두면 된다.)
 * - 모든 DAO(UserDAO, FriendDAO, RoomDAO, MessageDAO)는 getConnection()으로 연결을 받아 쓴다.
 * - 객체를 만들 필요가 없어서 생성자를 private으로 막고, 메서드는 모두 static이다.
 *
 * db.properties 예시 (프로젝트 폴더 맨 위에 둔다):
 *   db.url=jdbc:mysql://localhost:3306/messenger?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
 *   db.user=root
 *   db.password=비밀번호
 */
public class DBConnection {

    private static final Properties props = new Properties();

    // static 블록: 클래스가 처음 로딩될 때 딱 한 번 실행된다.
    static {
        // 1) 프로젝트 폴더(실행 위치)에 있는 db.properties를 먼저 찾는다.
        try (InputStream in = new FileInputStream("db.properties")) {
            props.load(in);
        } catch (IOException e) {
            // 2) 없으면 클래스패스(src 폴더 등)에서 찾아본다.
            try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (in != null) {
                    props.load(in);
                } else {
                    System.err.println("[DBConnection] db.properties 파일을 찾을 수 없습니다. 프로젝트 폴더에 만들어 주세요.");
                }
            } catch (IOException e2) {
                System.err.println("[DBConnection] db.properties를 읽지 못했습니다: " + e2.getMessage());
            }
        }
    }

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");
        if (url == null) {
            throw new SQLException("db.properties에 db.url이 없습니다.");
        }
        return DriverManager.getConnection(url, user, password);
    }
    
    public static void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        if (rs != null) {
            try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
        close(conn, ps);
    }

    public static void close(Connection conn, PreparedStatement ps) {
        if (ps != null) {
            try { ps.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
        if (conn != null) {
            try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    
    /// DB 연결 확인용 main 
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("DB 연결 성공! (" + conn.getCatalog() + ")");
            System.out.println("MySQL 버전: " + conn.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            System.out.println("DB 연결 실패: " + e.getMessage());
            System.out.println("- MySQL이 켜져 있는지");
            System.out.println("- db.properties의 비밀번호가 맞는지");
            System.out.println("- db/schema.sql을 실행했는지 확인하세요.");
        }
    }
}
