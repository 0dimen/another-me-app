# 시연 빌드 검증

검증일: 2026-11-15 (KST). 실제 Pleos SDK·Pleos 전용 이미지·실차 검증 결과는 포함하지 않습니다.

| 항목 | 환경 / 방법 | 결과 |
| --- | --- | --- |
| 단위 테스트·APK | JDK 17, Gradle 8.13, `./gradlew testDebugUnitTest assembleDebug` | 성공 · 판단 엔진 25개, 실패 0 |
| 일반 Android UI | Android 14 API 34, 1920×720 가로 에뮬레이터(일반 Android 이미지, smallest width 720dp) · `connectedDebugAndroidTest` | 5개 성공, 실패 0 |
| Automotive UI | Android Automotive API 34 ext9, 1920×720 · AndroidJUnitRunner 직접 실행 | 5개 성공, 실패 0 |
| 서명·배포 사본 | `apksigner verify`, SHA-256 비교 | debug 서명 검증·APK 사본 일치 |

UI 테스트는 세 모드·두 버튼 스타일, 주행 중 입력 잠금, 통화 뒤에도 안내 중이면 대기, 제안 후 별도 동의, 입력·확인·저장·수정·Activity 재생성 후 유지·삭제, DataStore 저장소 재생성 후 조회를 검사합니다. 기기 프로세스 강제 종료·재부팅·실물 태블릿·실차 검증은 별도입니다.

## 실행 증거

- [빌드 결과](verification/build.txt)
- [단위 테스트 XML](verification/unit-tests.xml)
- [일반 Android UI 테스트 XML](verification/tablet-ui-tests.xml)
- [Automotive AndroidJUnitRunner 결과](verification/automotive-ui-tests.txt)
- [APK SHA-256](../dist/SHA256SUMS)

## 실제 앱 화면

다음은 합성 디자인이 아닌 Automotive 에뮬레이터에서 실행한 앱의 캡처입니다. **차량·내비·통화·문자는 가짜 데이터**입니다. 일반 Android Automotive 이미지이며 Pleos 화면 또는 실차 탑재 화면이 아닙니다.

| 화면 | 캡처 |
| --- | --- |
| 최소화 · 아래 AI 다이얼 / 위 바 키 / 오른쪽 검정 | [min.png](verification/screenshots/min.png) |
| 중간 · 지연 제안 / 받는 사람·내용 / 별도 확인 버튼 | [mid-alert.png](verification/screenshots/mid-alert.png) |
| 확장 · 시간 관계와 맡긴 일 | [full-alert.png](verification/screenshots/full-alert.png) |
| 입력 후 저장 전 확인 | [task-confirm.png](verification/screenshots/task-confirm.png) |
| 통화 중 기다림 | [waiting-call.png](verification/screenshots/waiting-call.png) |
| 통화 종료 후 길 안내 중 기다림 | [waiting-guidance.png](verification/screenshots/waiting-guidance.png) |

## 이 환경에서 해결한 실행 문제

- 기본 사용자 Android 설정 폴더가 읽기 전용이라 debug 키를 프로젝트의 `.local/`에 생성하도록 했습니다. Kotlin 컴파일도 데몬 없이 Gradle 프로세스 안에서 수행합니다. 원본 기획 저장소는 수정하지 않았습니다.
- 에뮬레이터는 임시 쓰기 가능 AVD 폴더에서 생성했습니다. Gradle의 UTP 임시 파일 오류는 같은 Android 설정 경로와 `-Djava.io.tmpdir=/tmp`를 지정해 해결했습니다. 기본 사용자 폴더를 쓸 수 있는 일반 개발 환경에서는 README의 기본 명령을 사용하면 됩니다.
- 이 Mac의 소프트웨어 GPU에서 부분 화면 갱신이 누락되어 에뮬레이터의 `debug.hwui.use_buffer_age=false`, `debug.hwui.use_partial_updates=false`로 검사했습니다. 테스트 캡처는 실제 디스플레이를 읽는 UiAutomation으로 수행했습니다. 이 설정을 앱 코드에 강제하지 않습니다. 실제 기기의 GPU 검증을 대체하지 않습니다.
- 실행 중 동의 문구가 실제 발송 성공으로 바뀌지 않는 것을 확인했습니다. 실제 메시지 전송 API·권한은 없습니다.
