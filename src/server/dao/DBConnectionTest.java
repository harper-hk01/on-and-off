package server.dao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * DB 연결 확인용 프로그램
 *
 * 실행 전 준비
 *   1) db/schema.sql 실행 (필요하면 db/sample_data.sql도)
 *   2) 프로젝트 맨 위에 db.properties 만들기 (db.properties.example 참고)
 *
 * Eclipse: 이 파일에서 오른쪽 클릭 → Run As → Java Application
 */
public class DBConnectionTest {

    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("DB 연결 성공! (" + conn.getCatalog() + ")");
        } catch (SQLException e) {
            System.out.println("DB 연결 실패: " + e.getMessage());
            System.out.println("- MySQL이 켜져 있는지");
            System.out.println("- db.properties의 비밀번호가 맞는지");
            System.out.println("- db/schema.sql을 실행했는지 확인하세요.");
        }
    }
}
