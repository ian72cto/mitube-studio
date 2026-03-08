# AI 썸네일 생성 기능 개발 진행 문서
> 최종 업데이트: 2026-03-09

---

## 1. 기능 개요

라이브 예약 화면(`LiveReservationActivity`)의 썸네일 영역을 탭 했을 때:
- **갤러리 직접 선택** 또는
- **AI 썸네일 자동 생성** 

두 가지 방법으로 썸네일을 설정할 수 있는 기능.

AI 생성 방식은 사용자가 레퍼런스 이미지(최대 5장)와 텍스트 프롬프트를 입력하면, Gemini API를 통해 4개의 썸네일 후보 이미지를 생성하고 그 중 하나를 선택해서 적용한다.

---

## 2. 유저 플로우

```
LiveReservationActivity
  └── 썸네일 영역 탭
        └── ThumbnailBottomSheetDialog (선택창)
              ├── 갤러리에서 선택 → pickImageLauncher → 썸네일 적용
              └── AI로 만들기     → AiThumbnailActivity
                                        ├── 레퍼런스 이미지 선택 (최대 5장)
                                        ├── 프롬프트 입력
                                        ├── "생성하기" 버튼
                                        │     └── GeminiApiManager.generateThumbnailImages()
                                        ├── 생성된 4개 썸네일 중 선택 (RecyclerView)
                                        └── "이 썸네일로 결정" → RESULT_OK로 URI 반환
                                                                  └── LiveReservationActivity에서 썸네일 적용
```

---

## 3. 신규 생성 파일 목록

| 파일 경로 | 설명 |
|---|---|
| `java/.../AiThumbnailActivity.kt` | AI 썸네일 생성 화면 (메인 로직) |
| `java/.../ThumbnailBottomSheetDialog.kt` | 갤러리/AI 선택 BottomSheet |
| `java/.../GeneratedThumbnailAdapter.kt` | 생성된 썸네일을 RecyclerView로 표시하는 Adapter |
| `java/.../BuildConfigUtil.kt` | Gemini API Key를 안전하게 읽는 유틸 |
| `java/.../api/GeminiApiManager.kt` | Gemini API HTTP 통신 클래스 (OkHttp 사용) |
| `res/layout/activity_ai_thumbnail.xml` | AI 썸네일 Activity 레이아웃 |
| `res/layout/bottom_sheet_thumbnail.xml` | 선택창 BottomSheet 레이아웃 |
| `res/layout/item_generated_thumbnail.xml` | RecyclerView에서 썸네일 아이템 레이아웃 |

---

## 4. 수정된 기존 파일 목록

| 파일 경로 | 수정 내용 |
|---|---|
| `LiveReservationActivity.kt` | 썸네일 탭 시 BottomSheet 표시로 변경, `aiThumbnailLauncher` 추가, `handleThumbnailSelected()` 공통 메서드 분리 |
| `AndroidManifest.xml` | `AiThumbnailActivity` 선언, `GEMINI_API_KEY` meta-data 추가 |
| `app/build.gradle.kts` | `local.properties`에서 `geminiApiKey` 읽어 `manifestPlaceholders`에 주입 |
| `local.properties` | `geminiApiKey=YOUR_GEMINI_API_KEY_HERE` 항목 추가 |

---

## 5. Gemini API Key 설정 방법

### 무료 API Key 발급 절차
1. [Google AI Studio](https://aistudio.google.com/) 접속 (구글 계정 로그인)
2. 좌측 메뉴 → **Get API Key** → **Create API Key**
3. 발급된 키 복사

### 로컬 개발 환경 설정
```properties
# /Users/ian72ceo/workspace/MLC/app/local.properties
# (이 파일은 .gitignore에 포함 - 절대 커밋하지 말 것)
sdk.dir=/Users/ian72ceo/Library/Android/sdk
geminiApiKey="여기에_발급받은_API_KEY_붙여넣기"
```

> **⚠️ 중요:** `local.properties`는 `.gitignore`에 포함되어 있어야 하며, 절대로 GitHub에 커밋하면 안 됩니다.

### 무료 Tier 한도
| 항목 | 한도 |
|---|---|
| 분당 요청 수 | 15 RPM |
| 일일 요청 수 | 1,500 RPD |
| 월별 요청 수 | 무제한 (무료 체험) |

---

## 6. GeminiApiManager 구조

```kotlin
// 파일: api/GeminiApiManager.kt
class GeminiApiManager(private val apiKey: String) {
    // 엔드포인트: gemini-2.5-flash (멀티모달, 빠른 응답)
    // BASE_URL = "...googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    suspend fun generateThumbnailImages(
        prompt: String,
        referenceBitmaps: List<Bitmap>,  // 유저가 선택한 레퍼런스 이미지
        count: Int = 4                    // 생성할 썸네일 개수
    ): List<Bitmap>
}
```

### ⚠️ 현재 상태 (Mock)
`AiThumbnailActivity.kt`의 `generateMockAiThumbnails()` 함수는 아직 **더미 Bitmap**(단색 + 프롬프트 텍스트)을 반환하고 있음.
실제 `GeminiApiManager`를 연결하려면 아래 **다음 할 일** 참고.

---

## 7. 다음 할 일 (Next Steps)

### ✅ 완료
- [x] BottomSheet UI (갤러리 vs AI 선택)
- [x] `AiThumbnailActivity` UI 레이아웃
- [x] 다중 이미지 선택 (`PickMultipleVisualMedia`, 최대 5장)
- [x] 프롬프트 입력 UI
- [x] 더미 이미지로 생성 플로우 시뮬레이션
- [x] `GeneratedThumbnailAdapter` (선택 기능 포함)
- [x] 선택된 썸네일을 `LiveReservationActivity`로 반환
- [x] `GeminiApiManager` 클래스 구조 작성 (OkHttp 기반)
- [x] `local.properties` + `build.gradle.kts` API Key 주입 파이프라인
- [x] `AndroidManifest.xml`에 `AiThumbnailActivity` 등록

### 🔲 남은 작업

#### 우선순위 1 - 실제 Gemini API 연결
```kotlin
// AiThumbnailActivity.kt 내 generateMockAiThumbnails() 함수를
// 아래 실제 API 호출로 교체해야 함:

private fun generateRealAiThumbnails(prompt: String) {
    val apiKey = BuildConfigUtil.getGeminiApiKey(this)
    val geminiManager = GeminiApiManager(apiKey)
    
    // 레퍼런스 이미지를 Bitmap으로 변환
    val referenceBitmaps = selectedImageUris.mapNotNull { uri ->
        // ContentResolver로 uri → Bitmap 변환
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
    }
    
    CoroutineScope(Dispatchers.Main).launch {
        val bitmaps = geminiManager.generateThumbnailImages(prompt, referenceBitmaps, 4)
        // UI 업데이트 (RecyclerView에 표시)
    }
}
```

> **⚠️ 주의:** Gemini API는 텍스트 LLM이라 **이미지 생성이 아닌 이미지 분석/설명**을 반환합니다.
> 실제 이미지 생성을 위해서는 **Imagen 3 (Vertex AI)** 또는 **Google AI Studio의 이미지 생성 기능** 필요.
> Imagen 3는 무료 티어가 없어 결제 계정이 필요할 수 있음. 대안으로 **Stable Diffusion Hugging Face API** (무료) 검토 가능.

#### 우선순위 2 - Imagen 3 연동 검토 (대안 정리)

| API | 이미지 생성 가능 | 무료 | 비고 |
|---|---|---|---|
| Gemini Flash (현재) | ❌ | ✅ | 텍스트만 반환 |
| Imagen 3 (Vertex AI) | ✅ | ❌ | 유료, 고품질 |
| Stable Diffusion (HuggingFace) | ✅ | ✅ | 무료, 오픈소스 |
| DALL-E 3 (OpenAI) | ✅ | ❌ | 유료 |

→ **무료 이미지 생성 추천:** HuggingFace Inference API (`stabilityai/stable-diffusion-xl-base-1.0`)  
→ API 키 발급: [huggingface.co/settings/tokens](https://huggingface.co/settings/tokens)

#### 우선순위 3 - 에러 처리 및 UX 개선
- [ ] API Key 미설정 시 사용자에게 안내 UI 표시
- [ ] API 호출 중 로딩 스피너 표시
- [ ] 네트워크 오류, Rate Limit 초과 시 재시도 안내
- [ ] 생성된 이미지 품질이 낮을 경우 재생성 버튼

#### 우선순위 4 - 사용자 API Key 직접 입력 UI
```
설정 화면 → Gemini API Key 입력 → SharedPreferences에 암호화 저장
```
현재는 개발용으로 `local.properties`에 저장하지만, 앱 배포 시 유저가 직접 입력하는 UI 필요.

---

## 8. 핵심 파일 빠른 참조

```
/Users/ian72ceo/workspace/MLC/app/app/src/main/
├── java/com/mitube/mlc/
│   ├── AiThumbnailActivity.kt            ← 메인 AI 썸네일 화면
│   ├── ThumbnailBottomSheetDialog.kt     ← 선택 BottomSheet
│   ├── GeneratedThumbnailAdapter.kt      ← 생성 결과 목록 Adapter
│   ├── BuildConfigUtil.kt                ← API Key 읽기 유틸
│   ├── LiveReservationActivity.kt        ← 썸네일 탭 → BottomSheet 연결
│   └── api/
│       └── GeminiApiManager.kt           ← Gemini HTTP 통신
├── res/layout/
│   ├── activity_ai_thumbnail.xml
│   ├── bottom_sheet_thumbnail.xml
│   └── item_generated_thumbnail.xml
└── AndroidManifest.xml

/Users/ian72ceo/workspace/MLC/app/
├── local.properties                      ← geminiApiKey 여기 입력 (git 제외)
└── app/build.gradle.kts                  ← manifestPlaceholders로 키 주입
```
