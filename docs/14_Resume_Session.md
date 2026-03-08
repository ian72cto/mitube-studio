# MiTube Studio — 이어가기 문서
> 마지막 작업: 2026-03-09 07:03 KST  
> GitHub: https://github.com/ian72cto/mitube-studio (branch: `main`)

---

## ✅ 완료된 작업

### 1. AI 썸네일 생성 기능 (완성)
**Pollinations.ai** 무료 서비스 기반으로 구현 완료.  
API 키 불필요, YouTube 썸네일 최적화(1280×720), 최대 4장 동시 생성.

| 파일 | 상태 |
|------|------|
| `ImagenApiManager.kt` | ✅ Pollinations.ai로 교체 완료 |
| `AiThumbnailActivity.kt` | ✅ API 키 코드 전부 제거, 간소화 완료 |
| `activity_ai_thumbnail.xml` | ✅ btnSetKey / tvNoKey 제거 완료 |
| `GeneratedThumbnailAdapter.kt` | ✅ RecyclerView 어댑터 구현 완료 |

**AI 썸네일 동작 흐름:**
```
프롬프트 입력 (한글 가능)
  → buildEnhancedPrompt() (레퍼런스 이미지 스타일 힌트 추가)
  → Pollinations.ai API x4 (seed 값을 달리해 4가지 변형)
  → RecyclerView에 4장 표시 → 사용자 선택 → JPEG로 저장 후 반환
```

### 2. 빌드 오류 수정 완료
- `colors.xml` — 누락된 Material Design 색상 리소스 추가
- `themes.xml` — 나이트 모드 테마 수정
- `build.gradle.kts` — `compileSdk` DSL 문법 수정

### 3. GitHub 저장소 구성
- 저장소: `ian72cto/mitube-studio`
- 최신 커밋: `6e06a92` — Pollinations.ai 완성 커밋

---

## 🔧 현재 프로젝트 구조 (핵심 파일)

```
app/src/main/java/com/mitube/mlc/
├── MainActivity.kt                  # 메인 (채널 목록)
├── AiThumbnailActivity.kt           # ✅ AI 썸네일 생성
├── GeneratedThumbnailAdapter.kt     # ✅ 생성된 썸네일 목록
├── ThumbnailBottomSheetDialog.kt    # 썸네일 선택 바텀시트
├── LiveCameraActivity.kt            # 라이브 카메라 화면
├── LiveDefaultSettingsActivity.kt   # 라이브 기본 설정
├── LiveReservationActivity.kt       # 라이브 예약
├── ReservationListActivity.kt       # 예약 목록
├── ChannelRepository.kt             # 채널 데이터 관리
├── ApiKeyManager.kt                 # (레거시, 현재 미사용)
├── YouTubeAuthManager.kt            # Google OAuth
├── YouTubeAppAuthManager.kt         # YouTube 앱 인증
└── api/
    ├── ImagenApiManager.kt          # ✅ Pollinations.ai 이미지 생성
    ├── GeminiApiManager.kt          # Gemini 텍스트 API
    └── YouTubeLiveApiManager.kt     # YouTube Live API
```

---

## 📋 다음 세션에서 할 일

### 우선순위 A — 실 기기 테스트
- [ ] Android 기기 또는 에뮬레이터에서 `AiThumbnailActivity` 실행
- [ ] 프롬프트 입력 → 4장 생성 → 선택 → 반환 흐름 확인
- [ ] 네트워크 오류 시 에러 다이얼로그 정상 표시 확인
- [ ] 레퍼런스 이미지(최대 5장) 선택 후 생성 확인

### 우선순위 B — Java 빌드 환경 수정
```bash
# Mac에 JDK 설치 (Homebrew)
brew install --cask zulu@17
# 또는 Android Studio 번들 JDK 사용
export JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home
cd /Users/ian72ceo/workspace/MLC/app && ./gradlew assembleDebug
```

### 우선순위 C — AI 썸네일 UX 개선 (미구현)
- [ ] 생성 중 취소 버튼 추가
- [ ] 모델 선택 옵션 추가 (`flux`, `turbo`, `flux-realism`)
- [ ] 생성된 이미지 갤러리 저장 기능

### 우선순위 D — YouTube Live API 연동
- `YouTubeLiveApiManager.kt` 현재 열려있는 파일 — 추가 구현 필요
- [ ] 라이브 스트리밍 시작/정지 API 연결
- [ ] 채팅 읽기 기능 구현

---

## 🔑 주요 기술 결정 사항

| 항목 | 결정 | 이유 |
|------|------|------|
| 이미지 생성 | Pollinations.ai | Google Imagen/Gemini 무료 티어 제한 |
| 인증 | Google OAuth 2.0 | YouTube API 접근 필요 |
| 최대 이미지 | 4장 | Pollinations 동시 요청 최적값 |
| 해상도 | 1280×720 | YouTube 썸네일 표준 |

---

## 🚀 이어가기 프롬프트 (복사해서 사용)

```
MiTube Studio Android 앱 작업 이어가기.
프로젝트: /Users/ian72ceo/workspace/MLC
GitHub: ian72cto/mitube-studio

현재 상태:
- AI 썸네일 생성 기능 완성 (Pollinations.ai, API 키 불필요)
- 빌드 오류 수정 완료, GitHub main 브랜치에 푸시 완료 (commit: 6e06a92)

다음 작업: docs/14_Resume_Session.md 참고해서 우선순위 A부터 진행해줘.
```
