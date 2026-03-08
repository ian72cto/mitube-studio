package com.mitube.mlc

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope

class YouTubeAuthManager(private val context: Context) {
    private var googleSignInClient: GoogleSignInClient

    init {
        // YouTube 계정 접근 및 채널 리스트업 권한 요청 스코프 (readonly)
        val youtubeScope = Scope("https://www.googleapis.com/auth/youtube")
        val youtubeReadonlyScope = Scope("https://www.googleapis.com/auth/youtube.readonly")
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(youtubeScope, youtubeReadonlyScope)
            // .requestIdToken("구글 클라우드 콘솔의 Web Client ID를 나중에 여기에 넣어야 합니다")
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    // 로그인 창 띄우는 Intent 반환
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    // 로그인 결과 처리
    fun handleSignInResult(intent: Intent?): GoogleSignInAccount? {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            task.result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 로그아웃
    fun signOut(onComplete: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
            onComplete()
        }
    }
}
