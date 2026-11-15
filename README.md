# 카링 (메이터팀)

**출퇴근길을 먼저 챙겨 주는 차량용 동행 앱** — 출발 전 차량 점검부터 지각 알림, 주차 위치 기억, 퇴근길 충전·마감 할인 픽업까지, Pleos Connect 차량 신호와 음성으로 운전자에게 먼저 묻고 대신 처리합니다.

## APK 파일 위치

- [`apk/메이터팀_카링_release.apk`](apk/메이터팀_카링_release.apk) (release 빌드, 서명됨)

## 실행 방법

### 1. APK 설치

Pleos Connect 에뮬레이터를 켠 뒤:

```bash
adb install apk/메이터팀_카링_release.apk
```

### 2. 필요한 설정 (한 번만)

```bash
# 주행 중 내비 위에 팝업을 띄우는 권한 (앱은 user 10에서 실행됨)
adb shell appops set --user 10 com.example.carproject SYSTEM_ALERT_WINDOW allow

# 음성 인식(STT)에 Mac/PC 마이크를 연결 (에뮬레이터를 다시 켤 때마다 필요)
adb emu avd hostmicon
```

- 내비게이션 경로 안내(NaviHelper)에는 에뮬레이터 인터넷 연결이 필요합니다. 연결이 끊기면 에뮬레이터를 cold boot(`-no-snapshot-load`)로 다시 켜 주세요.

### 3. 앱 실행

앱 목록에서 앱을 실행하거나:

```bash
adb shell am start --user 10 -n com.example.carproject/.MainActivity
```

첫 화면은 QR 로그인입니다. 실제 휴대폰 연동은 데모 데이터로 처리하므로 **"나중에 할게요"**를 누르거나, 아래 시연 명령으로 로그인을 완료할 수 있습니다.

### 4. 시연 메뉴 (장면 넘기기)

앱 영역 **왼쪽 위 구석을 길게 누르면** 숨은 시연 메뉴가 열립니다. 시간대(아침 8시/저녁 6시), 내비 상황(+22분 정체, 가게 500m/300m), 데모 데이터(오늘 일정, Slack 멘션, 바깥 3도), 차량 신호(기어 P/D, 배터리 14%, 창문 열림), 차량 신호 띠 표시를 바꿀 수 있습니다.

같은 동작을 adb로도 할 수 있습니다:

```bash
D="adb shell am broadcast -a com.example.carproject.DEMO -p com.example.carproject"
$D --es login phone      # QR 로그인 완료
$D --es mode morning     # 아침 8시(출근) / evening: 저녁 6시(퇴근)
$D --es gear D           # 기어 D / P
$D --es popup late       # 지각 알림 (+22분 정체)
$D --es data mention     # Slack 멘션 1개 / meeting: 오늘 일정 / reset: 처음 실행
$D --es battery low      # 배터리 14%
$D --es window open      # 조수석 창문 열림
$D --es hud on           # 실시간 차량 신호 띠
```

## 테스트 환경

| 항목 | 값 |
| --- | --- |
| 에뮬레이터 | **Pleos Connect v2.1.0** (AVD `Pleos_Connect_v2.1.0`, android-automotive, `release_connect_v2.1.0`) |
| OS | Android 14 (API 34), arm64-v8a |
| 화면 규격 | **2560 × 1440, 160dpi** (서드파티 앱 영역 약 1680 × 1210) |
| 디자인 기준 | 1920 × 1080(16:9) 시안을 앱 영역에 맞춰 확대·축소 |
| 개발 PC | macOS (Apple Silicon), Android Studio |

## 빌드 환경

| 항목 | 값 |
| --- | --- |
| 언어 | Kotlin 2.2.10 (Jetpack Compose, Material 3) |
| Gradle / AGP | Gradle 9.6.0 / Android Gradle Plugin 9.5.1 |
| minSdk / targetSdk | 28 / 37 |
| 구조 | MVVM + Repository, 단일 Activity + Navigation Compose |

```bash
./gradlew :app:assembleDebug      # 디버그
./gradlew :app:assembleRelease    # 릴리스 (서명하려면 루트에 keystore.properties 필요)
```

## 사용한 Pleos Connect / AAOS API

| SDK·API | 버전 | 쓰임 |
| --- | --- | --- |
| Pleos TextToSpeech | 3.3.1 | 차량 상태·멘션·지각 질문·픽업 번호 읽어 주기 |
| Pleos SpeechToText | 3.3.1 | 목적지·주차 위치·"보내줘" 음성 답 받기 |
| Pleos NaviHelper | 3.0.5 | 길안내 시작, 충전소·픽업 가게 경유지 추가 |
| AAOS Car API (CarPropertyManager) | API 34 | `GEAR_SELECTION`으로 주차(P)·주행 판단 |

Slack, 캘린더, 마감 할인 가게, 충전소, 유가, QR 로그인, 차량 상태(배터리·타이어·창문 등)는 데모 데이터(`data/fake`)로 동작하며, Repository 인터페이스 뒤에 있어 실제 API로 교체할 수 있습니다.
