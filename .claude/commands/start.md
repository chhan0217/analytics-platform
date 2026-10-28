# 세션 시작

아래 순서대로 수행한다.

## 1. 문서 확인
- CLAUDE.md를 다시 읽고 규칙을 숙지한다.
- memory/MEMORY.md를 읽고 프로젝트 현재 상태, 의사결정 사항을 확인한다.

## 2. 어제 한 일 확인
- `git log --oneline -20`으로 최근 커밋 이력을 확인한다.
- 변경된 파일과 내용을 요약한다.

## 3. 오늘 할 일 정리
- MEMORY.md의 "다음 작업" 항목과 docs/feature-roadmap.md를 참고하여 오늘 할 작업 목록을 정리한다.
- 우선순위와 함께 테이블로 보여준다.

## 4. 서버 구동
- 기존 8080, 5173 포트 프로세스를 종료한다: `lsof -ti:8080 | xargs kill -9 2>/dev/null`, `lsof -ti:5173 | xargs kill -9 2>/dev/null`
- Backend 서버를 백그라운드로 실행한다: `./gradlew :backend:run`
- Web Dashboard를 백그라운드로 실행한다: `cd web-dashboard && npm run dev`
- 서버가 정상 구동되었는지 `/health` 엔드포인트로 확인한다.

## 5. 준비 완료 보고
- 오늘 할 일, 서버 상태를 요약하여 보여준다.
- 첫 번째 작업을 시작할 준비가 되었음을 알린다.
