# 작업 완료 — 커밋, 테스트, PR

아래 순서대로 수행한다. 각 단계에서 실패하면 즉시 멈추고 원인을 보고한다.

## 1. 설계 문서 확인
- Design First 규칙에 따라, 이번 작업의 설계 문서(docs/)가 존재하는지 확인한다.
- 없으면 설계 문서를 먼저 작성한다.
- 구현 내용과 문서가 일치하는지 확인하고, 변경 사항을 문서에 반영한다.

## 2. API 문서 업데이트
- docs/api.md에 새로 추가/변경된 API가 반영되었는지 확인한다.
- 누락된 내용이 있으면 업데이트한다.

## 3. 전체 테스트
- Backend: `./gradlew :backend:build` (클린 빌드 + 테스트 + ktlint + detekt)
- Web Dashboard: `cd web-dashboard && npx tsc --noEmit` (타입 체크)
- 테스트 실패 시 수정 후 재시도한다.

## 4. Git 커밋
- `git status`와 `git diff`로 변경 내용을 확인한다.
- Conventional Commits 형식으로 커밋 메시지를 작성한다.
- 관련 파일만 선택적으로 `git add`하고 커밋한다.

## 5. Git 푸시
- 현재 브랜치를 원격에 푸시한다: `git push -u origin <branch>`
- 푸시 결과를 확인한다.

## 6. CI 결과 확인
- `gh run list --branch <branch> --limit 3`으로 CI 상태를 확인한다.
- CI가 실행 중이면 `gh run watch`로 결과를 기다린다.
- CI 실패 시 원인을 분석하고 보고한다.

## 7. PR 생성
- PR 대상 브랜치: CLAUDE.md의 Git 전략에 따라 결정한다 (feature/* → develop)
- PR 제목: Conventional Commits 형식
- PR 본문: 변경 내용, 테스트 방법, 관련 문서를 포함한다.
- `gh pr create`로 PR을 생성한다.
- PR URL을 보여준다.

## 8. MEMORY.md 업데이트
- 완료된 작업을 MEMORY.md에 반영한다.
- 다음 작업 항목을 업데이트한다.
