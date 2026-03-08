# 카메라 앱 개발 전략 및 리스크 분석 (Camera Dev Risk & Strategy)

본 문서는 MLC 앱 개발 시 고려해야 할 하드웨어별 특이사항과 카메라 개발자로서 직면하게 될 기술적 난관을 분석합니다.

## 1. 갤럭시 모델별 지원 전략

| 모델 | 광학 10배 줌 지원 방식 | 핵심 지원 전략 |
| :--- | :--- | :--- |
| **S21 Ultra** | 물리적 10배 망원 렌즈 | API 28+ Multi-Camera API를 활용한 개별 물리 ID 접근 최적화. |
| **S22 Ultra** | 물리적 10배 망원 렌즈 | **GOS 우회 전략:** Video Category 레이블링을 통해 성능 제한 해제 및 VPU 가속 활용. |
| **S23 Ultra** | 물리적 10배 망원 렌즈 | 가장 안정적인 타겟. RAW 송출 데이터의 실시간 디노이징(Denoising) 엔진 적용. |
| **S24 Ultra** | 5배 광학 + 50MP 크롭 | **하이브리드 엔진:** 5배 렌즈의 고화소 데이터를 MLC 커스텀 크롭 로직으로 10배급 화질 구현. |

## 2. 개발자 환경 및 워크플로우

### 2.1 개발 환경 (Tooling)
- **IDE:** Android Studio (Ladybug 이상 권장)
- **Low-Level Control:** `Camera2 API` (Java/Kotlin) + `NDK Camera API` (C++)
- **NDK (Native Development Kit):** 실시간 비디오 프레임 처리를 위해 C++ 기반의 성능 최적화 필수.
- **Profilers:** Android GPU Inspector 및 Memory Profiler를 통한 메모리 누수 감시.

### 2.2 개발 과정 (Workflow)
1. **Camera Discovery:** `getCameraIdList()`를 통해 기기의 모든 논리적/물리적 카메라 특성(Characteristics) 분석.
2. **Session Configuration:** RAW, YUV, Preview 스트림을 동시에 뽑아내기 위한 복잡한 세션 설정.
3. **Native Bridge (JNI):** 카메라 프레임 데이터를 Java에서 처리하면 GC(Garbage Collection)가 발생해 화면이 끊기므로, 데이터를 즉시 C++로 넘겨 처리.
4. **Thermal Watchdog:** 4K 60fps 송출 시 기기 온도를 모니터링하며 비트레이트를 유동적으로 조절하는 로직 구현.

## 3. 개발 난이도 및 리스크 (Risk Analysis)

### 3.1 기술적 난이도: ★★☆☆☆ (UI) -> ★★★★★ (Core)
- 단순 뷰파인더 노출은 쉬우나, **송출 중 렌즈 전환(Lens Switching)** 시 발생하는 Flicker(깜빡임)와 Focus/AE(노출) 값이 틀어지는 현상을 보정하는 것이 극도로 까다롭습니다.

### 3.2 핵심 리스크 리스트
1. **Flicker & Discontinuity (위험도: 상)**
   - 렌즈 전환 시 하드웨어 ISP가 재설정되면서 0.1~0.3초간 화면이 멈추거나 검게 변함.
   - **대응:** 고난도의 'Cross-fade' 합성 혹은 두 렌즈 세션을 동시에 열어두는 Warm-up 전략 필요.
2. **Thermal Throttling (위험도: 상)**
   - 4K 인코딩 + SRT 전송 + 렌즈 제어를 동시에 수행하면 10분 내로 기기 온도가 45도 초과.
   - **대응:** MLC 짐벌의 펠티어 냉각 유닛과 연동하여 하드웨어 냉각 제어 신호 송출.
3. **OEM 특정 이슈 (위험도: 중)**
   - 삼성 정품 카메라 앱과 달리 서드파티 앱에서는 특정 대역폭(예: 8K)이나 고해상도 60fps 접근이 제한될 수 있음.
   - **대응:** Samsung Camera SDK 직접 연동 및 원격 패치 지원.
4. **Memory Contention (위험도: 중)**
   - 비디오 버퍼가 메모리에 쌓일 경우 앱 강제 종료(OOM) 발생.
   - **대응:** Zero-copy 버퍼 공유 기술 적용.

## 4. 최종 결론
카메라 앱 개발은 단순히 코딩만 하는 것이 아니라 **"하드웨어 하이재킹(Hardware Hijacking)"**에 가깝습니다. 특히 줌 기능은 사용자 기기마다 렌즈의 특정 ID가 다르므로, 실물 기기 없이 개발하는 것은 **나침반 없이 바다를 건너는 것**과 같습니다. 

따라서 초기 설계 단계에서부터 **하드웨어 추상화 레이어(HAL Wrapper)**를 견고하게 설계하여 기기별 파편화 리스크를 최소화해야 합니다.
