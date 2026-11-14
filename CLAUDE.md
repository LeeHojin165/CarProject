# 출퇴근 동행 앱 (Pleos Connect 프로토타입)

## 목표
- 공모전 시연 영상용 프로토타입이다. 완성된 서비스가 아니라 화면이 흘러가는 모습과 Pleos API 실제 연동 몇 가지를 보여주는 것이 목표다.
- 배달 앱 연동(포장 주문)은 넣지 않는다.

## 기술 스택
- Android, Kotlin, Jetpack Compose (Material 3), 단일 Activity + Navigation Compose
- minSdk 28, Pleos Connect 에뮬레이터 이미지(API 34)에서 실행
- Pleos Connect SDK
  - Maven: https://nexus-playground.pleos.ai/repository/maven-releases/
  - Vehicle: ai.pleos.playground:Vehicle:2.3.2
  - TextToSpeech, SpeechToText, NaviHelper는 같은 저장소에서 버전을 확인해 추가한다.
- AAOS Car API: build.gradle에 useLibrary("android.car"), 기어는 CarPropertyManager의 GEAR_SELECTION

## 실제로 연동할 것 (Pleos API)
1. 차량 상태: Fuel / EvBattery / 주행 가능 거리 / Tire 4개 공기압 / Brake / Door / Window / Light
2. 기어 상태: P이면 주차, 그 외는 주행 중. P로 바뀌면 주차 위치 묻기 화면을 띄운다.
3. TTS: 출근 버튼을 누르면 차량 상태를 한 문장으로 읽어준다.
4. STT: 목적지와 주차 위치를 말로 받는다.
5. NaviHelper: 목적지나 경유지를 넣고 내비게이션 화면을 연다.
- SDK 호출 전에 각 도메인의 checkXXXCapabilities로 지원 여부를 확인하고, 지원하지 않으면 가짜 값으로 대체한다.

## 가짜 데이터로 처리할 것
- Slack 메시지 전송과 멘션 요약, 캘린더 일정, 마감 할인 가게, 충전소 목록, 유가, QR 로그인
- 모든 가짜 데이터는 data/fake 패키지 한 곳에 모은다. 나중에 실제 API로 바꾸기 쉽게 Repository 인터페이스 뒤에 둔다.

## 화면 규칙
- 디자인 기준은 design/ 폴더의 HTML 파일(1920×1080, 16:9)이다. 색, 글자 크기, 간격을 HTML 값 그대로 옮긴다.
- 출근 화면은 라이트 모드, 퇴근 화면은 다크 모드다.
  - 라이트: 배경 #F6F3F2, 카드 #FFFFFF, 보조 #E2DFDB, 글자 #1A1A1A, 보조 글자 #5C5955, 강조 #002C5F
  - 다크: 배경 #0A0F17, 카드 #121A26, 보조 #1C2636, 글자 #F6F3F2, 보조 글자 #A7B0BD, 링크 #00AAD2, 강조 #AACAE6(강조 위 글자 #002C5F)
- 글꼴은 Asta Sans. 버튼 높이는 최소 96dp로, 운전 중 누르기 쉬운 크기를 유지한다.
- 무엇이든 실행하기 전에는 항상 사용자에게 묻는다(메시지 전송, 공조, 창문).
- 주행 중에는 내비게이션이 화면을 차지하고, 우리 앱은 작은 팝업만 띄운다.

## 작업 방식
- 한 번에 화면 하나씩 만든다. 만든 뒤 에뮬레이터에서 빌드가 되는지 확인한다.
- 시연 버튼: 화면 구석에 숨은 디버그 메뉴를 두고, 기어나 연료 값을 바꿔 다음 장면을 강제로 띄울 수 있게 한다.
