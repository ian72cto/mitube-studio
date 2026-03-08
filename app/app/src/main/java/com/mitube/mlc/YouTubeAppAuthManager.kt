package com.mitube.mlc

import android.content.Context
import android.content.Intent
import android.net.Uri
import net.openid.appauth.*

class YouTubeAppAuthManager(private val context: Context) {

    private val authService = AuthorizationService(context)
    private val authState = AuthState()

    companion object {
        // TODO: 구글 클라우드 콘솔의 '웹 애플리케이션' 또는 '안드로이드' Client ID를 여기에 넣으세요
        const val CLIENT_ID = "309514849051-h6nmul11p0k455t74hinl880l72he4ho.apps.googleusercontent.com"
        const val REDIRECT_URI = "com.mitube.mlc:/oauth2redirect"
        
        val AUTH_URI: Uri = Uri.parse("https://accounts.google.com/o/oauth2/v2/auth")
        val TOKEN_URI: Uri = Uri.parse("https://oauth2.googleapis.com/token")
    }

    fun getAuthIntent(): Intent {
        val config = AuthorizationServiceConfiguration(AUTH_URI, TOKEN_URI)
        
        val authRequestBuilder = AuthorizationRequest.Builder(
            config,
            CLIENT_ID,
            ResponseTypeValues.CODE,
            Uri.parse(REDIRECT_URI)
        )
        
        // 브랜드 계정은 email, profile 스코프를 포함할 경우 강제로 본계정에 토큰이 귀속되는 버그(구글 API 특성)가 있습니다.
        // 따라서 YouTube 관련 스코프만 요청해야 합니다.
        // prompt=select_account: 매 로그인 시 계정 선택 화면을 강제 표시하여 브랜드 계정을 선택할 수 있도록 합니다.
        val authRequest = authRequestBuilder
            .setScope("https://www.googleapis.com/auth/youtube https://www.googleapis.com/auth/youtube.readonly")
            .setPrompt("select_account")
            .build()
            
        return authService.getAuthorizationRequestIntent(authRequest)
    }

    fun handleAuthorizationResponse(intent: Intent, onTokenReady: (String?) -> Unit) {
        val authResponse = AuthorizationResponse.fromIntent(intent)
        val authException = AuthorizationException.fromIntent(intent)
        
        authState.update(authResponse, authException)
        
        if (authResponse != null) {
            authService.performTokenRequest(authResponse.createTokenExchangeRequest()) { response, ex ->
                authState.update(response, ex)
                if (response != null && authState.accessToken != null) {
                    onTokenReady(authState.accessToken)
                } else {
                    onTokenReady(null)
                }
            }
        } else {
            onTokenReady(null)
        }
    }
}
