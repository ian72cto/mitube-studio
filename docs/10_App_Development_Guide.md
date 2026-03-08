# MLC 앱 개발 가이드 (App Development Guide)

## 1. 개요
MLC 앱은 안드로이드(Kotlin) 기반으로 작성되며, 스마트폰의 하드웨어 성능을 최대로 끌어올리기 위한 저지연 비디오 처리 엔진과 원격 제어 인터페이스를 핵심으로 합니다.

## 2. 기술 스택 (Tech Stack)
- **Language:** Kotlin (UI/Business Logic), C++ (Video Engine/NDK)
- **Framework:** Jetpack Compose (Modern UI)
- **API Level:** Android 11 (API 30) 이상 (Camera2 API 최적화)
- **Concurrency:** Kotlin Coroutines & Flow
- **Dependency Injection:** Hilt
- **Networking:** Ktor (WebSocket), Lib-SRT (NDK), NDI SDK for Android

## 3. 앱 아키텍처 (Architecture)
### 3.1 레이어 구조
- **Presentation Layer:** Jetpack Compose 기반 UI. 짐벌 제어, 오버레이 편집, 프리뷰 화면 처리.
- **Domain Layer:** 핵심 비즈니스 로직. 송출 경로 결정, 페일오버 자동화 시스템.
- **Data Layer:** Camera2 API 연동, SSD 저장 처리, 원격 CCU 명령 수신 및 처리.
- **Engine Layer (Native):** MediaCodec 고속 인코딩 및 SRT/NDI 캡슐화 처리 (C++ 기반 NDK).

### 3.2 핵심 모듈 (Key Modules)
- `:core-video`: Camera2 API 및 MediaCodec 관리. 3Path Rendering(Clean/Prog/Oper) 처리.
- `:core-network`: 빔포밍 무선망 최적화 및 셀룰러 바이패스 로직.
    - **Native Driver (PnP):** RTL8153 네이티브 드라이버를 통한 이더넷 연결 보장.
    - **Modular Bridge Integration:** Comfast CF-E313AC(AC1200/867Mbps) 등 입증된 실외용 무선 브릿지(CPE) 모듈을 짐벌 시스템에 물리적으로 통합.
    - **Hardware Interface:** 짐벌 내장 USB PD에서 PoE(48V) 승압 회로를 거쳐 브릿지에 전원을 공급하고, RTL8153과 이더넷으로 데이터 통신.
    - **Back-end Agnostic:** 5.8GHz 지향성 전용망을 Primary로 사용하되, 필요 시 유선 LAN이나 타사 무선 모듈로 즉각 교체 가능한 구조.
- `:feature-control`: WebSocket 기반 원격 제어 및 톡백 처리.
- `:feature-ai`: STT 분석을 통한 시각 큐 증강현실 오버레이 처리.

## 4. 핵심 개발 태스크 (Core Development Tasks)
### 4.1 3-Path 비디오 처리 로직
- **Clean Path:** 센서 원본 데이터를 그대로 MediaCodec으로 넘겨 SRT 송출.
- **Program Path:** OpenGLES를 활용해 HTML5/텍스트 오버레이를 실시간 믹싱한 후 인코딩.
- **Operator Path:** 레이턴시를 최소화한 저화질 프리뷰를 짐벌 핸들에 연결된 태블릿 혹은 폰 화면에 표시.

### 4.2 빔포밍 전용망 최적화
- 이더넷 포트(USB-C to Ethernet) 우선순위 할당.
- 네트워크 품질 실시간 모니터링을 통한 무순단 경로 전환(Bypass) 로직 구현.

### 4.3 원격 제어 (Control Plane)
- 짐벌 모터 제어(PAN/TILT)를 위한 HID 프로토콜 통신.
- CCU 서버로부터 수신된 Tally 신호에 따른 UI 색상 변경 로직.

## 4. 핵심 기능 로드맵 (Software Roadmap)

### 4.1 Phase 1 (MVP: 'Quick-Start' Model)
- **Galaxy-Specific Imaging (Hardware Direct):** 
    - **Multi-Camera API:** 갤럭시 S24 Ultra 등 멀티 렌즈 장치의 개별 카메라 ID에 직접 접근. 
    - **Optical Zoom Logic:** 송출 중 끊김 없는 렌즈 스위칭(Ultra Wide <-> Wide <-> Tele) 및 물리 광학줌 배율 최적화 연동.
    - **Full Manual SDK:** 화이트밸런스(K단위), 셔터스피드, ISO 수동 고정 기능.
- **Focus & Zoom:** 하이브리드 포커스(Auto/Manual) 및 핀치 줌을 통한 물리적 광학줌 엔진 구동.
- **YouTube Studio Engine:** 라이브 대시보드 및 방송 관리 UI.
    - **OAuth Login:** Google 계정 기반 채널 선택 및 권한 획득.
    - **Stream Control:** 방송 예약(Schedule), 제목/설정 변경, 썸네일 업로드 UI 제공.
    - **Live Dashboard:** 동시 시청자 수, 네트워크 헬스(Bitrate/Framedrop) 시각화.
- **Non-Destructive Overlays:** 
    - **Live Chat:** HTML WebView를 활용한 실시간 채팅창 오버레이.
    - **Tally & Status:** 3색 탤리 프레임 및 상태 정보 표시.
    - *Note: 모든 UI 요소는 송출 영상에 포함되지 않는 독립 레이어로 구현.*
- **Quick-Setup:** 웹 관리자 페이지 QR 스캔 또는 OAuth 연동을 통한 즉시 세팅.
- **Device Auth:** 기기 식별값(Serial/UID) 기반 가입자 기기 관리.

### 4.2 Phase 2 (Professional: 'Blackmagic Reference' Model)
- **Advanced Scopes:** 히스토그램, 폴스 컬러(False Color), 포커스 피킹(Focus Peaking) 등 전문 영상 보조 스코프 제공.
- **Clock Sync:** CAM1을 마스터로 하여 네트워크 기반 **타임코드(Timecode)** 동기화 (멀티캠 편집 효율 극대화).
- **Pro Audio:** 블루투스 마이크 1채널 믹싱 및 Rig를 통한 멀티채널(XLR/UAC) 입력 지원.
- **Production Comms:** 실시간 Tally 신호 연동 및 SRT/NDI 기반 양방향 톡백(Talkback) 활성화.

### 4.3 Phase 3 (Hardware Integration: 'Rig-Agnostic' Model)
- **External Display:** 리그 통합 허브의 HDMI Alt-mode를 활용한 외부 모니터 터치 미러링 제어.
- **Direct SSD Recording:** USB-C 인터페이스 기반 NVMe SSD 초고속 RAW/Log 기록 지원.
## 5. 개발 리스크 및 전략 분석 (Risks & Strategy)
카메라 앱 개발은 하드웨어 종속성이 매우 강하여 에뮬레이터만으로는 한계가 명확합니다. 상세한 모델별 지원 전략과 개발 시 주의해야 할 리스크(발열, 프레임 드랍, 렌즈 전환 Flicker 등)는 다음 문서를 참조하십시오.

- **상세 문서:** [11_Camera_Dev_Risk_Strategy.md](file:///Users/ian72ceo/workspace/MLC/docs/11_Camera_Dev_Risk_Strategy.md)
