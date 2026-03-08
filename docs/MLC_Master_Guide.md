# MLC (MiTube Live CAM) 프로젝트 마스터 가이드

이 문서는 MLC 프로젝트의 모든 기획, 기술 사양, 비즈니스 전략을 통합한 마스터 가이드입니다.

## 1. 프로젝트 비전
스마트폰(갤럭시 S21~S25 Ultra)의 하드웨어 잠재력을 극대화하여 전문 방송국 수준의 중계 시스템을 구축합니다. 특히 **빔포밍 무선 리피터(Beamforming Wireless Repeater)** 기술을 통해 전 세계 어디서나 기가비트급 방송이 가능한 독보적인 성능을 제공하며, 기존 ENG 카메라와 중계차를 완벽히 대체하는 것이 목표입니다.

## 2. 하드웨어 혁신 (MLC Rig & Gimbal)
- **빔포밍 무선 리피터:** 독자적인 무선 신호 추적 및 증폭 기술을 통해 중계차 없는 고화질 생중계를 실현하는 핵심 경쟁력.
- **페이로드 분산형 설계 (HPD):** 짐벌 베이스에 SSD(NVMe), 대용량 배터리, 무선 모듈을 배치하고, 10Gbps 고속 슬립링을 통해 폰과 데이터를 주고받아 모터 부하를 최소화합니다.
- **스마트 렌즈 인식 (4-Pin):** 4핀 포고 핀 접점을 통해 외장 렌즈 ID 인식 및 ISP 프로파일 자동 적용.
- **액티브 냉각:** 펠티어 소자(TEC)와 저소음 팬을 결합한 스마트 발열 관리.

## 3. 소프트웨어 및 서비스 (MLC App & Server)
- **Triple-Path Rendering:** Clean Feed, Program Feed, Operator Feed의 3중 비디오 파이프라인을 구축합니다.
- **AI-Cue System:** 감독의 음성을 STT로 변환하여 지연 시간과 독립된 시각적 큐(카운트다운, 탤리)를 현장에 증강현실(AR)로 제공합니다.
- **Direct-to-Endpoint:** 영상 데이터는 서버를 거치지 않고 목적지로 직접 송출하여 인프라 비용을 획기적으로 낮춥니다.

## 4. 비즈니스 모델
- **80/15/5 전략:** 80% 무료 사용자(기반 확보), 15% 교회(NDI 생태계), 5% 상업용(SaaS 수익화)으로 구성됩니다.
- **상업용 구독 모듈:** Pro NDI Bridge, **5.8GHz 빔포밍 제어**, AI 오토 토킹/트래킹 등 전문 중계 모듈로 수익을 창출합니다. (불안정한 셀룰러 본딩 대신 확실한 전용망 솔루션 제공)

## 5. 상세 문서 리스트
- [비즈니스 및 투자 전략](file:///Users/ian72ceo/workspace/MLC/docs/01_Business_Strategy.md)
- [마케팅 및 수익화](file:///Users/ian72ceo/workspace/MLC/docs/02_Marketing_Model.md)
- [제품 정의서 (PDR)](file:///Users/ian72ceo/workspace/MLC/docs/03_Product_Definition.md)
- [기술 설계서 (TDR)](file:///Users/ian72ceo/workspace/MLC/docs/04_Technical_Design.md)
- [인프라 및 서버 사양](file:///Users/ian72ceo/workspace/MLC/docs/05_Infrastructure_Spec.md)
- [특허 전략](file:///Users/ian72ceo/workspace/MLC/docs/06_Patent_Strategy.md)
- [예산 및 스케줄](file:///Users/ian72ceo/workspace/MLC/docs/07_Budget_Schedule.md)
- [앱 개발 가이드](file:///Users/ian72ceo/workspace/MLC/docs/10_App_Development_Guide.md)
