# MLC 개발 폴더 구조 가이드

Antigravity 에이전트가 코드를 작성할 때 준수해야 할 표준 폴더 구조입니다.

## 1. 전체 트리 구조
```text
MiTube_Live_CAM/
├── .agent/                 # 에이전트 전용 행동 지침 및 워크플로우
│   └── rules/              # 에이전트별 상세 규칙 (Agent_Hardware, Agent_Streamer 등)
├── docs/                   # 기획, 투자, 기술 사양 등 모든 문서
├── app/                    # Android 클라이언트 소스 코드
│   ├── src/main/cpp/       # 네이티브 C++ (libSRT, NDI, JNI Bridge)
│   └── src/main/java/com/mitube/mlc/
│       ├── camera/         # Triple Surface Rendering 파이프라인
│       ├── network/        # SRT/NDI 본딩 및 WebSocket CCU 통신
│       ├── hardware/       # 스마트 렌즈(4-Pin), 짐벌(HID), 펠티어 제어
│       └── ui/             # 방송용 인터페이스 및 오버레이
├── backend/                # GCP 기반 백엔드 서비스
│   ├── api-gateway/        # Node.js 인증 및 설정 API
│   ├── ccu-server/         # WebSocket 명령 중계 서버 (Go/Redis)
│   └── stt-ai/             # Python 기반 AI 음성 분석 모델
├── web/                    # 방송국용 중앙 제어 대시보드 (React)
└── hardware/               # 기구 설계 및 PCB 회로도 (CAD/Gerber)
```

## 2. 개발 지침
- **Native SDKs:** `app/libs` 및 `app/src/main/cpp`에 전문 방송용 SDK(NDI, libSRT)를 위치시킵니다.
- **Configuration:** 프로젝트 전체 설정값은 `docs/05_Infrastructure_Spec.md` 및 `app/src/main/res/values/config.xml`을 따릅니다.
- **Git Flow:** `feature/` 브랜치 전략을 사용하여 에이전트 간 코드 충돌을 방지합니다.
