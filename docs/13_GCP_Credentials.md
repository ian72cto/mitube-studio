# Google Cloud Platform (GCP) 인증 정보

본 문서는 MLC 프로젝트가 YouTube Data API 및 Google 로그인을 위해 사용하는 인증 정보를 안전하게 보관하고 관리하기 위한 내부 문서입니다.

## 1. Android 클라이언트 ID (Local Debug Key용)
- **앱 이름:** MiTube Live Camera (또는 Android 클라이언트 1)
- **패키지 이름:** `com.mitube.mlc`
- **로컬 디버그 SHA-1:** `B8:CF:6F:AE:AE:35:35:35:DB:49:0C:6D:CE:CA:D5:E7:D2:1E:FF:2F`
- **할당된 클라이언트 ID:** `309514849051-aj3l181vq9r7mdctissqocoj82r1mab2.apps.googleusercontent.com`

---
*참고: Android 클라이언트는 앱 내부 소스 코드(Java/Kotlin)에 명시적으로 하드코딩될 필요가 없습니다. Android 운영체제의 Google Play 서비스가 패키지명과 서명(SHA-1)을 통해 자체적으로 식별하고 인증을 통과시킵니다.*

*추가 백엔드 토큰 발급이나 Web Login 연동이 필요한 경우, [웹용 클라이언트 ID]를 추가로 만들어 소스 코드 내(ex. `strings.xml`)에 `default_web_client_id`로 삽입해야 합니다.*
