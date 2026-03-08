# MLC 인프라 및 서버 사양서

## 1. GCP 서버 아키텍처

| 서버명 | 서비스 / 인스턴스 | 스팩 | 용도 |
| :--- | :--- | :--- | :--- |
| **MLC-CCU** | GKE / Compute Engine | n2-standard-4 (Auto-scaling) | WebSocket 명령 중계 및 상태 관리 |
| **MLC-STT** | Compute Engine | g2-standard-4 (L4 GPU) | 실시간 감독 음성 -> STT 변환 |
| **MLC-Auth** | Cloud Run | e2-medium | 사용자 인증 및 QR 토큰 발행 |
| **MLC-Storage** | Cloud Storage | - | 영상 메타데이터 및 렌즈 프로필 저장 |
| **MLC-Realtime** | Cloud Memorystore | Redis 7.0 | 탤리, 톡백, 실시간 접속자 트래킹 |

### 2.1 네트워크 및 보안
- **Zero Video Traffic:** 영상 데이터는 서버를 거치지 않습니다 (Direct-to-Endpoint). 중계 서버는 오직 제어 신호(텍스트)만 처리합니다.
- **Premium Tier Network:** 제어 신호의 안정성을 위해 구글 전용 글로벌 백본망을 사용합니다.
- **Encryption:** 모든 제어 평면 통신은 AES-256 및 TLS 1.3으로 암호화됩니다.

### 2.2 네트워크 및 전송 장비
- **Beamforming Wireless Link:** 5.8GHz/60GHz 비인가 대역 기반 독자망 장비.
    - **Interface:** USB 3.0 (USB-C to Ethernet RTL8153 연동).
    - **SoC:** MAC/Phy 통합형 시스템 반도체 탑재 (능동형 빔포밍/수신율 최적화).
- **Pro NDI Bridge:** 대규모 지역 간 송출을 위한 지점 간 터널링 서버.

## 3. 비용 절감 모델
- **Cost Efficiency:** 영상 데이터를 직접 송출하므로 사용자 수가 증가해도 서버 비용 상승폭이 극히 낮습니다.
- **CDN Caching:** HTML5 오버레이 등 정적 자산은 Cloud CDN을 통해 배포하여 응답 속도 향상 및 비용 절감을 도모합니다.
