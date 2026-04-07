package com.example.washhands_android_googlemarket // <-- 본인의 패키지명으로 꼭 수정하세요!

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.example.washhands_android_googlemarket.ui.theme.TargetApp_VIPTheme // <-- 본인 테마명으로 수정

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TargetApp_VIPTheme {
                // 상단바/하단바 영역을 고려한 기본 레이아웃
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        // 1. 리퍼러 추적 로직 실행
                        ReferrerTracker()

                        // 2. 화면에 표시될 내용
                        Greeting("Target App VIP")
                    }
                }
            }
        }
    }
}

@Composable
fun ReferrerTracker() {
    val context = LocalContext.current

    // LaunchedEffect(Unit)은 이 컴포저블이 처음 생성될 때 한 번만 실행됩니다.
    LaunchedEffect(Unit) {
        val referrerClient = InstallReferrerClient.newBuilder(context).build()

        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        try {
                            val response = referrerClient.installReferrer
                            val referrerUrl = response.installReferrer

                            // [결과 확인] Logcat에서 'INSTALL_REFERRER'로 필터링해서 보세요.
                            // 유니티에서 보낸 utm_source=... 데이터가 여기 찍힙니다.
                            Log.d("INSTALL_REFERRER", "성공적으로 읽어온 데이터: $referrerUrl")

                            // TODO: 여기에 Firebase 로그 기록 코드를 넣으세요.
                            // 예: FirebaseAnalytics.getInstance(context).logEvent(...)

                            // 연결 종료 (필수)

                            // Firebase Analytics 객체 가져오기
                            val firebaseAnalytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)

                            // 유니티에서 온 데이터를 번들에 담기
                            val params = android.os.Bundle().apply {
                                putString("referrer_url", referrerUrl)
                            }

                            // 'app_open_from_unity'라는 이름의 커스텀 이벤트로 전송
                            firebaseAnalytics.logEvent("app_open_from_unity", params)


                            referrerClient.endConnection()
                        } catch (e: Exception) {
                            Log.e("INSTALL_REFERRER", "데이터 읽기 실패: ${e.message}")
                        }
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        Log.d("INSTALL_REFERRER", "이 기기는 리퍼러 기능을 지원하지 않습니다.")
                    }
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                        Log.d("INSTALL_REFERRER", "구글 플레이 서비스에 연결할 수 없습니다.")
                    }
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                Log.d("INSTALL_REFERRER", "서비스 연결이 끊어졌습니다.")
            }
        })
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!\nWaiting for Referrer...",
        modifier = modifier
    )
}