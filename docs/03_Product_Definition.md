# MLC 제품 정의서 (PDR: Product Definition Document)

## 1. 3대 서비스 시나리오 (Target Scenarios)

### 1.1 1인 크리에이터 모드 (Solo Creator)
- **네트워크:** LTE / 5G 공용망 직결.
- **특징:** 별도 장비 없이 스마트폰과 짐벌만으로 기동성 극대화. 야외 브이로그, 인터뷰 중계에 최적화.

### 1.2 종교 시설 전용 모드 (Church Edition)
- **네트워크:** 교회 내부 WiFi Mesh Network (갤럭시 WiFi 최적화).
- **특징:** 넓은 대예배당 및 부속 건물 간 이동 촬영 시 WiFi 핸드오버를 통한 끊김 없는 송출. 기존 인프라 활용으로 도입 비용 최소화.

### 1.3 프로 상업 방송 모드 (Commercial Pro)
- **네트워크:** 5G/LTE 기본 + 군중 밀집 시 CPE(Wireless Bridge) 우회.
- **특징:** 경기장, 공연장 등 기지국 마비 지역에서 건물 옥상의 CPE 허브와 지향성 링크 형성. 전문 방송급 Gbps 대역폭 및 초저지연 보장.

## 1. 핵심 사용자 경험 (UX)
- **Zero-Config Setup:** QR 코드 스캔 또는 **YouTube OAuth 로그인**을 통해 채널 정보 및 방송 설정을 즉시 동기화합니다.
- **Integrated YouTube Studio:** 앱 내에서 직접 방송 예약, 제목 수정, 썸네일 업로드가 가능하며 스트림 상태를 실시간 모니터링합니다.
- **Native-Level Camera Control:** 삼성 갤럭시의 순정 카메라 앱과 동일한 수준의 하드웨어 접근권을 확보합니다.
    - **[1단계 핵심 관문] Seamless Optical Zoom:** 초광각, 광각, 망원 렌즈 간의 부드러운 전환과 하드웨어 광학줌을 송출 중에도 프레임 드랍 없이 완벽하게 제어합니다. (프로젝트의 핵심 본질)
    - **Full Manual Mode:** ISO, 셔터스피드, 화이트밸런스, 포커스를 센서 레벨에서 직접 조절하여 영화적 룩(Cinematic Look)을 구현합니다.
- **Non-Destructive Chat Overlay:** HTML 기반 라이브 채팅창을 화면에 띄워 촬영자가 독자와 소통할 수 있게 하되, 이 오버레이는 송출되는 영상(Clean Feed)에는 합성되지 않습니다.
- **Director-Led Production:** 촬영자는 화면에 띄워진 AI 시각 큐(카운트다운, 탤리)만 보고 리딩하며, 본부에서는 원격으로 모든 앵글을 통제합니다.
- **Blackmagic Standard Scopes:** 히스토그램, 폴스 컬러, 지브라 등 전문가용 분석 툴을 직관적인 UI로 제공합니다.
- **Master Timecode Sync:** 여러 대의 갤럭시 폰이 CAM1의 타임코드를 기준으로 1프레임 오차 없이 동기화되어 소스 관리 효율을 극대화합니다.
- **3-Color Tally System:** 
    - **Grey (No Select):** 대기 상태.
    - **Green (Preview):** 다음 송출 대기 중 (준비).
    - **Red (Program):** 실시간 방송 송출 중 (On-Air).
- **Subscriber Device Management:** 스마트폰 유니크 ID(ANDROID_ID / Serial)를 기반으로 가입자 계정에 기기를 자동 등록하여 원격 관리 및 제어권을 할당합니다.
- **Clean & Program Feed:** 촬영자 화면에는 각종 수치가 보이지만, 송출 영상은 순수 원본(Clean)이거나 최종 합성본(Program)으로 구분됩니다.

## 2. 기능 요구사항 사양
- **3중 비디오 파이프라인:** `MediaCodec`을 활용한 저지연 Clean, Program, Operator 피드 생성.
- **원격 인터뷰 동기화:** 오디오 파이프링 파형 분석을 통한 실시간 싱크 매칭 엔진.
- **빔포밍 무선 리피터:** 독자적인 무선 신호 수신 품질을 물리적으로 개선하는 능동형 리피터 기술 탑재. (가장 강력한 제품 경쟁력)
- **Cellular Bypass & Failover:** 공용 셀룰러 망 포화 시 전용 빔포밍 링크로 즉시 전환하여 끊김 없는 무순단 중계 보장.
- **스마트 렌즈 인식:** 4핀 접점을 통해 외장 광학 배율 및 왜곡 자동 보정.

## 3. 하드웨어 요구사항 (Hardware Spec)

### 3.1 짐벌 하드웨어 (Gimbal Spec)
- **소재:** PA66 + 탄소섬유 엔지니어링 플라스틱 (초경량/고강도).
- **슬립링:** Pan/Tilt 축 내장 10Gbps 데이터 슬립링.
- **냉각:** 후면 펠티어 소자 및 저소음 배기 팬.
- **배터리:** 핸들 내장 21700 셀 기반 USB PD 65W 출력.

### 3.2 지원 대상 스마트폰 (Supported Devices)
- **광학 10배 줌 공식 지원 모델:** 
    - **Galaxy S21 Ultra:** 최초의 듀얼 망원(3x/10x) 광학 렌즈 탑재.
    - **Galaxy S22 Ultra:** MLC의 GOS 우회 및 하드웨어 가속 전략으로 최적화 가능.
    - **Galaxy S23 Ultra:** 가장 안정적인 10x 고정 광학 성능 제공.
- **참고 사항:** S24 Ultra는 5x 광학 + 50MP 고화소 크롭 방식으로 하이이브리드 10배를 구현하므로, MLC 앱에서 별도의 보정 로직을 적용하여 대응함.

## 4. 품질 지표 (QA Metrics)
- **지연 시간:** SRT 송출 지연 500ms 이내 (Global Network 기준).
- **안정성:** 4K 60fps 연속 송출 10시간 이상 (발열 셧다운 제로).
- **인식 정밀도:** 외장 렌즈 결합 시 인식 성공률 99.9%.
