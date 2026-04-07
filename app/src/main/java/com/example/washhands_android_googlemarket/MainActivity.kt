package com.example.washhands_android_googlemarket

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.example.washhands_android_googlemarket.ui.theme.TargetApp_VIPTheme

class MainActivity : ComponentActivity() {
    private lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TargetApp_VIPTheme {
                navController = rememberNavController()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        ReferrerTracker(navController)
                        AppNavHost(navController = navController)
                    }
                }
            }
        }
    }

    // 앱이 실행 중일 때 딥링크가 들어오면 호출됨
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // 새로운 인텐트로 교체
        intent.data?.let { uri ->
            Log.d("DEEP_LINK", "새로운 딥링크 수신: $uri")
            // NavController가 딥링크를 처리하도록 함
            navController.handleDeepLink(intent)
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController) {
    // 유니티에서 보낸 "myapp://open/screen1?utm_..." 패턴을 모두 수용하기 위해 
    // uriPattern 뒤에 쿼리 파라미터 와일드카드(?.*)를 고려하거나 파라미터를 정의해야 합니다.
    val baseUri = "myapp://open"

    NavHost(
        navController = navController,
        startDestination = "screen1"
    ) {
        composable(
            route = "screen1",
            // 쿼리 파라미터가 있어도 매칭되도록 {args} 구조를 활용하거나 패턴을 유연하게 잡습니다.
            deepLinks = listOf(navDeepLink { uriPattern = "$baseUri/screen1?utm_source={s}&utm_medium={m}&utm_campaign={c}&target_screen={t}" },
                               navDeepLink { uriPattern = "$baseUri/screen1" })
        ) {
            ScreenContent("1번 화면 (파랑)", Color(0xFF2196F3), Color.White) {
                navController.navigate("screen2")
            }
        }
        composable(
            route = "screen2",
            deepLinks = listOf(navDeepLink { uriPattern = "$baseUri/screen2?utm_source={s}&utm_medium={m}&utm_campaign={c}&target_screen={t}" },
                               navDeepLink { uriPattern = "$baseUri/screen2" })
        ) {
            ScreenContent("2번 화면 (초록)", Color(0xFF4CAF50), Color.White) {
                navController.navigate("screen3")
            }
        }
        composable(
            route = "screen3",
            deepLinks = listOf(navDeepLink { uriPattern = "$baseUri/screen3?utm_source={s}&utm_medium={m}&utm_campaign={c}&target_screen={t}" },
                               navDeepLink { uriPattern = "$baseUri/screen3" })
        ) {
            ScreenContent("3번 화면 (빨강)", Color(0xFFF44336), Color.White) {
                navController.navigate("screen1")
            }
        }
    }
}

@Composable
fun ScreenContent(title: String, backgroundColor: Color, contentColor: Color, onNext: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = backgroundColor, contentColor = contentColor) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(30.dp))
            Button(onClick = onNext, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = backgroundColor)) {
                Text("다음 화면으로 이동", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun ReferrerTracker(navController: NavHostController) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val referrerClient = InstallReferrerClient.newBuilder(context).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                    try {
                        val referrerUrl = referrerClient.installReferrer.installReferrer
                        Log.d("INSTALL_REFERRER", "데이터: $referrerUrl")
                        
                        val firebaseAnalytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
                        firebaseAnalytics.logEvent("app_open_from_unity", Bundle().apply { putString("referrer_url", referrerUrl) })
                        
                        if (referrerUrl != null && referrerUrl.contains("target_screen=")) {
                            val screenName = referrerUrl.split("target_screen=")[1].split("&")[0]
                            if (screenName.isNotEmpty()) {
                                navController.navigate(screenName) {
                                    popUpTo("screen1") { saveState = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                        referrerClient.endConnection()
                    } catch (e: Exception) { Log.e("INSTALL_REFERRER", "실패: ${e.message}") }
                }
            }
            override fun onInstallReferrerServiceDisconnected() {}
        })
    }
}
