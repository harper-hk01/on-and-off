## 협업 규칙

### 브랜치
- `main`: 발표·점검용 완성본 / `develop`: 통합 브랜치 (PR은 항상 **develop**으로)
- 작업 브랜치: `타입/작업이름` (소문자, 하이픈) 예) `feat/user-service`, `fix/logout-unregister`
- develop 최신을 pull 받은 뒤 새 브랜치 만들기 → 작업 → PR → merge 후 브랜치 삭제

### 커밋·PR 제목
`[타입] 한 일` 예) `[feat] UserService 구현`, `[fix] logout 명부 해제 누락`

| 타입 | 언제 |
|---|---|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `style` | 코드 스타일 수정 (기능 변경 없음) |
| `refactor` | 코드 구조 개선 |
| `test` | 테스트 코드 추가 |
| `chore` | 기타 작업 (빌드 설정, .gitignore 등) |

### 주의
- `db.properties`(DB 비밀번호)는 올리지 않기
