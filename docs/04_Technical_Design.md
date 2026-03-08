# MLC 기술 설계서 (TDR: Technical Design Document)

## 1. 비디오 파이프라인 (Triple-Path Rendering)
- **Path 1 (Clean Feed):** Camera2 API -> MediaCodec -> SRT/NDI 직접 송출. 오버레이가 없는 순수 영상 데이터입니다.
- **Path 2 (Program Feed):** Clean Feed + 송출용 HTML5 레이어 합성 -> Encoder. 최종 시청자가 보게 되는 화면입니다.
- **Path 3 (Operator Feed):** Clean Feed + 프리뷰 전용 정보(탤리 신호, 감독 지우, 오디오 레벨 등) 합성 -> 디바이스 디스플레이.

## 2. 핵심 기술 및 오픈소스 연동
- **Galaxy Native Bridge (Core Advantage):** 안드로이드 표준 API를 넘어 삼성 카메라 SDK 및 Multi-Camera Extension을 깊게 연동합니다.
    - **Reference Open Source:** `pedroSG94/RootEncoder` (RTMP/SRT/RTSP 표준 라이브러리)를 베이스로 하되, 내부 카메라 제어 모듈을 MLC 전용 `GalaxyPhysicalCameraManager`로 교체함.
    - **Physical Lens Selection:** `CameraManager.getCameraCharacteristics()`를 통해 기기 내 모든 물리 렌즈 ID를 추출하고, 사용자 또는 원격 제어 명령에 따라 `CaptureRequest.setPhysicalCameraId()`를 동적으로 호출함. (참조: `AntumDeluge/Open_Camera`)
    - **Stream Continuity:** `RootEncoder`의 오디오/비디오 동기화 메커니즘을 유지하면서, 카메라 리인스턴스(Re-initialization) 없이 렌즈 세션만 교체하는 'Fast-Switch' 로직을 적용함.
- **YouTube Direct Control (Zero-Studio Setup):**
    - **OAuth 2.0 Integration:** 채널 선택 및 방송 관리 권한 획득.
    - **Automated Workflow:** `LiveStreams.insert` 및 `LiveBroadcasts.bind`를 통해 앱 내에서 즉시 RTMP URL/Key를 생성하고 방송을 시작함. (유튜브 스튜디오 웹페이지 접속 불필요)

## 3. 원격 제어 및 동기화
- **WebSocket Control Plane:** 영상 데이터와 분리된 제어 채널로 탤리, 톡백, 짐벌 무빙 명령을 실시간 전송합니다. 관리자 웹페이지와 앱 간의 양방향 소켓 연결을 통해 구현됩니다.
- **Unique Device Identification:** 폰의 하드웨어 고유 식별자(Unique ID)를 서버 데이터베이스에 가입자별로 그룹화하여 관리합니다. 이는 원격 제어 시 장치를 오인하지 않도록 보장하는 핵심 보안 레이어입니다.
- **AI-Cue System:** 감독 음성 STT 분석을 통한 시각 큐 생성. SRT 영상의 네트워크 지연 시간과 상관없이 즉각적인 타이밍 가이드를 제공합니다.

## 4. 네트워크 전략 (Scenario-Based Connectivity)
- **Hybrid Path Management:** Public Cellular(LTE/5G), Internal Mesh(WiFi), CPE Dedicated Link(5.8GHz)를 상황에 따라 최적으로 전환.
- **Direct-to-Endpoint:** 모든 경로에서 중계 서버 없는 직결 송출로 초저지연 구현.
