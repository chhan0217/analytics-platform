# KMP SDK CI 실행 시간 개선 제안

> 상태: 검토 초안 (미적용)
>
> 작성일: 2026-10-22
>
> 대상 워크플로: `.github/workflows/ci-sdk-kmp.yml`

## 1. 목적

KMP SDK CI는 Android뿐 아니라 Kotlin/Native 기반 iOS 타깃과 Compose 어댑터까지 한 번에 검증한다. 검증 범위는 적절하지만, PR 피드백을 받기까지 약 7~10분이 필요해 개발 흐름이 느려질 수 있다.

이 문서는 현재 검증 수준을 유지하면서 다음 목표를 달성할 수 있는 개선안을 정리한다.

- PR에서 빠르게 실패 여부 확인
- `develop`과 `main`에서는 Android/iOS 전체 호환성 유지
- 불필요하거나 중복된 CI 실행 제거
- Gradle 및 Kotlin/Native 캐시 효율 개선
- 변경 전후 시간을 측정해 효과가 확인된 항목만 유지

## 2. 현재 구성

현재 KMP SDK CI는 다음 조건과 명령으로 실행된다.

- 실행 환경: `macos-latest`
- 검증 모듈: `analytics-kmp`, `analytics-compose`
- KMP 타깃: Android, iOS x64, iOS ARM64, iOS Simulator ARM64
- 캐시 대상: Gradle caches/wrapper, Kotlin/Native `~/.konan`
- 실행 명령:

```bash
./gradlew clean :sdk:analytics-kmp:build :sdk:analytics-compose:build
```

최근 성공한 실행은 약 7분 17초~9분 37초가 걸렸다. 확인한 실행 중 하나는 다음과 같이 소요됐다.

| 단계 | 소요 시간 |
|---|---:|
| Gradle/Konan 캐시 복원 | 약 1분 24초 |
| KMP SDK + Compose 클린 빌드 | 약 5분 52초 |
| 전체 작업 | 약 7분 31초 |

따라서 현재 실행은 멈춘 것이 아니라, macOS에서 Android와 Kotlin/Native iOS 타깃 전체를 빌드하는 데 대부분의 시간을 사용하고 있다.

## 3. 주요 원인

### 3.1 모든 PR에서 전체 플랫폼을 검증

`build`는 KMP 모듈에 선언된 Android와 iOS 타깃을 폭넓게 검증한다. Kotlin/Native 컴파일과 관련 의존성 준비는 일반 JVM 빌드보다 무겁다.

### 3.2 Android와 iOS를 단일 Job에서 순차 처리

현재 하나의 Gradle 명령이 모든 검증을 담당하므로 Android 검증이 끝나도 iOS 검증이 완료될 때까지 결과를 받을 수 없다.

### 3.3 캐시 복원 자체의 비용

`~/.gradle/caches`, `~/.gradle/wrapper`, `~/.konan` 전체를 복원하므로 캐시가 커질수록 다운로드와 압축 해제 시간이 늘어난다. 현재 캐시 복원에 약 1분 이상이 사용되는 실행이 확인됐다.

### 3.4 캐시 키의 범위가 넓음

현재 키는 저장소 전체의 `**/*.gradle.kts`를 해시한다. Backend나 샘플 앱처럼 KMP SDK와 직접 관계없는 Gradle 파일이 변경돼도 새 KMP 캐시 키가 생성될 수 있다.

### 3.5 `clean` 사용

GitHub-hosted runner는 기본적으로 매 실행마다 새 작업 디렉터리를 사용하므로 `clean`은 대체로 불필요하다. 다만 현재 캐시는 주로 사용자 Gradle/Konan 디렉터리이고 프로젝트의 `build/` 결과물은 복원하지 않으므로, `clean` 제거만으로 얻는 단축 효과는 제한적일 수 있다.

## 4. 개선안

### 4.1 PR 빠른 검증과 전체 플랫폼 검증 분리 — 효과 가장 큼

PR에서는 공통 로직, Android SDK, Compose 어댑터를 우선 검증하고 iOS 전체 검증은 `develop`/`main` push에서 수행한다.

```text
Pull Request
├── common 테스트
├── Android SDK 빌드
└── Compose 어댑터 빌드

develop/main push
├── PR과 동일한 빠른 검증
└── iOS 전체 타깃 컴파일 및 테스트
```

장점:

- PR 피드백 시간이 크게 줄어들 가능성이 높다.
- 병합 브랜치에서는 iOS 호환성을 계속 보장한다.
- Android 전용 변경에서 불필요한 iOS 전체 빌드를 줄일 수 있다.

주의점:

- iOS 관련 오류가 PR 단계가 아니라 병합 후 발견될 수 있다.
- `iosMain`, 공통 공개 API, Ktor Darwin 관련 변경에는 PR에서도 iOS 검증을 실행하는 조건부 정책을 검토해야 한다.

권장 정책:

- `commonMain` 또는 `iosMain` 변경: PR에서 iOS 검증 실행
- `androidMain` 또는 `analytics-compose`만 변경: 빠른 Android 검증만 실행
- `develop`/`main`: 항상 전체 검증

### 4.2 Android와 iOS Job 병렬화

빠른 검증과 iOS 검증을 별도 Job으로 분리한다.

```text
android-and-common ─┐
                    ├─ 병렬 실행
ios                 ─┘
```

장점:

- 전체 완료 시간이 두 Job의 합이 아니라 더 오래 걸린 Job의 시간에 가까워진다.
- 실패한 플랫폼을 즉시 구분할 수 있다.

주의점:

- macOS runner 사용 시간이 늘어날 수 있다.
- Android Job은 가능하면 비용과 시작 속도가 유리한 `ubuntu-latest` 사용을 검토한다.
- 두 Job에서 중복되는 Gradle 구성 및 의존성 준비 비용이 발생할 수 있다.

### 4.3 동일 브랜치의 이전 실행 자동 취소 — 즉시 적용 후보

연속 push 시 이전 커밋의 실행을 자동 취소한다.

```yaml
concurrency:
  group: kmp-ci-${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

장점:

- 이미 오래된 커밋의 결과를 기다리지 않아도 된다.
- macOS runner 사용량과 대기열을 줄인다.
- 검증 범위나 신뢰도에 영향을 주지 않는다.

### 4.4 Gradle 전용 캐시 액션으로 전환

직접 `actions/cache`로 Gradle 디렉터리를 관리하는 대신 다음 액션 사용을 검토한다.

```yaml
- uses: gradle/actions/setup-gradle@v4
```

Kotlin/Native `~/.konan` 캐시는 실제 적중률과 용량을 확인한 뒤 별도로 유지하거나 조정한다.

기대 효과:

- Gradle User Home과 빌드 캐시를 Gradle에 맞는 방식으로 관리
- 캐시 진단 정보 개선
- 직접 관리해야 하는 캐시 설정 감소

### 4.5 캐시 키 범위 축소

KMP CI와 직접 관련된 설정만 캐시 키에 포함한다.

```yaml
${{ hashFiles(
  'settings.gradle.kts',
  'build.gradle.kts',
  'gradle/libs.versions.toml',
  'gradle/wrapper/gradle-wrapper.properties',
  'sdk/analytics-kmp/*.gradle.kts',
  'sdk/analytics-compose/*.gradle.kts'
) }}
```

캐시 키를 지나치게 좁히면 실제 빌드 구성 변경을 놓칠 수 있으므로 루트 플러그인 설정과 Version Catalog는 반드시 포함한다.

### 4.6 `clean` 제거 — 안전하지만 단독 효과는 작을 수 있음

다음처럼 변경한다.

```bash
./gradlew :sdk:analytics-kmp:build :sdk:analytics-compose:build
```

장점:

- 불필요한 삭제 작업 제거
- 향후 프로젝트 빌드 결과 캐시를 도입할 때 재사용 가능성 향상

주의점:

- 새 GitHub-hosted runner에서는 작업 디렉터리가 이미 비어 있으므로 단독 적용 효과가 작을 수 있다.
- 로컬 재현 명령과 CI 명령을 구분해서 문서화해야 한다.

### 4.7 실제 필요한 Gradle Task만 실행

`build`는 여러 검증 작업을 묶은 생명주기 task다. PR에서 필요한 컴파일·테스트·린트 task만 명시하면 작업량을 줄일 수 있다.

적용 전에 다음을 확인해야 한다.

```bash
./gradlew :sdk:analytics-kmp:tasks --all
./gradlew :sdk:analytics-compose:tasks --all
```

확인 항목:

- 공통 테스트가 실제로 실행되는 task
- Android unit test와 Android library assemble task
- iOS 각 타깃의 compile/link/test task
- ktlint/detekt가 `check` 또는 `build`에 연결되는지 여부

정확한 task 이름은 현재 Kotlin/AGP 버전에서 조회한 뒤 확정하며, 이름을 추정해 워크플로에 바로 넣지 않는다.

## 5. 권장 적용 순서

### 1단계 — 검증 의미가 바뀌지 않는 저위험 개선

- `concurrency.cancel-in-progress` 추가
- `clean` 제거
- Gradle 전용 캐시 액션 도입 검토
- 캐시 크기와 복원 시간 측정

### 2단계 — PR 피드백 최적화

- PR용 빠른 Android/common/Compose Job 생성
- iOS 전체 검증은 별도 Job으로 분리
- Android Job의 `ubuntu-latest` 실행 가능 여부 확인

### 3단계 — 변경 경로별 조건부 iOS 검증

- `commonMain`, `iosMain`, 공통 Gradle 설정 변경 시 iOS Job 실행
- Android/Compose 전용 변경은 iOS Job 생략
- `develop`/`main`에서는 경로와 관계없이 필요한 전체 검증 유지

## 6. 권장 최종 정책

| 상황 | 검증 범위 |
|---|---|
| PR — Android/Compose 전용 변경 | common 테스트 + Android + Compose |
| PR — commonMain/iOS/공통 빌드 설정 변경 | common 테스트 + Android + Compose + iOS |
| `develop` push | 전체 Android/iOS 검증 |
| `main` push | 전체 Android/iOS 검증 |
| 동일 브랜치 연속 push | 이전 실행 자동 취소 |

Phase 1의 CI 완료 조건과 KMP의 iOS 구조 유지 원칙을 충족하기 위해 `develop`과 `main`의 전체 검증은 유지한다.

## 7. 검증 방법

변경 전 최근 성공 실행 5회의 중앙값을 기준선으로 기록하고, 변경 후 동일하게 5회 이상 측정한다.

측정 항목:

- 전체 workflow 시간
- 캐시 복원 시간
- Android/common Job 시간
- iOS Job 시간
- cache hit 여부
- PR 첫 실패 결과가 표시되기까지의 시간
- 간헐적 실패 여부

성공 기준 제안:

- 일반 Android/Compose PR의 피드백 시간이 기존보다 유의미하게 감소
- `develop`/`main` 전체 검증 성공
- 공통 테스트, Android 빌드, Compose 빌드, iOS 컴파일 중 누락 없음
- 캐시 관련 간헐적 실패가 발생하지 않음

## 8. 적용 체크리스트

- [ ] 변경 전 CI 최근 5회 시간 기록
- [ ] 현재 Gradle task 목록과 실제 실행 task 확인
- [ ] `clean` 제거 후 전체 검증
- [ ] concurrency 자동 취소 설정
- [ ] Gradle 캐시 방식 변경 및 cache hit 확인
- [ ] Android/common과 iOS Job 분리
- [ ] PR 경로 조건 검증
- [ ] `develop` push 전체 검증
- [ ] `main` 정책 검증
- [ ] 변경 후 CI 5회 시간 비교
- [ ] README 또는 진행 문서에 최종 정책과 결과 반영

## 9. 롤백 기준

다음 문제가 발생하면 해당 최적화를 되돌린다.

- PR에서 통과했지만 `develop`에서 반복적으로 플랫폼 오류 발생
- 캐시 변경 후 빌드 결과가 불안정하거나 간헐적 실패 증가
- iOS 검증이 의도치 않게 생략됨
- 전체 실행 시간이 오히려 증가함

워크플로 변경은 작은 커밋으로 나눠 적용해 각 단계의 효과와 문제를 독립적으로 판단한다.

## 10. 검토 시 결정할 사항

1. PR에서 iOS 전체 검증을 생략할 수 있는 변경 범위를 어디까지로 할지
2. iOS 검증을 모든 `develop` push에서 실행할지, 야간 스케줄도 병행할지
3. Android Job을 `ubuntu-latest`로 분리할지
4. `~/.konan` 캐시를 유지할지, 용량과 복원 시간을 측정한 뒤 조정할지
5. 목표 PR 피드백 시간을 몇 분으로 정할지

