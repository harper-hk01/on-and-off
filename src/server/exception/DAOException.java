package server.exception;

import java.sql.SQLException;

/**
 * DAO에서 DB 오류(SQLException)가 났을 때 던지는 예외.
 *
 * 왜 만들었나?
 * - SQLException은 "반드시 처리해야 하는 예외(checked exception)"라서,
 *   그대로 던지면 DAO → Service → CommandDispatcher까지 모든 메서드에 throws SQLException을 붙여야 한다.
 * - DAOException은 RuntimeException의 자식(unchecked exception)이라서 throws를 붙이지 않아도 된다.
 *   Service는 DB 오류를 신경 쓰지 않고 자기 일만 하면 되고,
 *   CommandDispatcher가 catch (DAOException e) 한 곳에서 "DB 오류"로 처리하면 된다.
 * - 일반 RuntimeException으로 감싸지 않고 따로 만든 이유:
 *   catch (DAOException e)로 잡으면 NullPointerException 같은 코드 버그와 DB 오류를 구분할 수 있다.
 *   (그래도 RuntimeException의 자식이므로 catch (RuntimeException e)에도 잡힌다.)
 *
 * 원래 SQLException은 cause(원인)로 들어 있으므로 e.getCause()나 e.printStackTrace()로 자세한 내용을 볼 수 있다.
 */
public class DAOException extends RuntimeException {

    private static final long serialVersionUID = 1L;  //경고 방지

    /** MySQL 오류 번호: 같은 값이 이미 있음 (UNIQUE·PRIMARY KEY 중복) */
    public static final int DUPLICATE_KEY = 1062;

    public DAOException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 원인이 된 SQLException의 오류 번호. 원인이 SQLException이 아니면 -1.
     * 예) 회원가입에서 같은 아이디가 동시에 들어와 UNIQUE에 걸리면 1062.
     */
    public int getErrorCode() {
        if (getCause() instanceof SQLException) {
            return ((SQLException) getCause()).getErrorCode();
        }
        return -1;
    }

    /** 중복(1062) 때문에 실패했는지 */
    public boolean isDuplicateKey() {
        return getErrorCode() == DUPLICATE_KEY;
    }
}
