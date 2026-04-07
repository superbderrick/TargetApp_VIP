package com.example.washhands_android_googlemarket

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TargetApp_VIPTheme {
                val navController = rememberNavController()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        // 리퍼러 추적은 공통으로 한 번 실행
                        ReferrerTracker()
                        
                        AppNavHost(navController = navController)
                    }
                }
            }
        }
    }
}

@Composable
fun AppNavHost(navController: NavHostController) {
    val uri = "myapp://open"

    NavHost(
        navController = navController,
        startDestination = "screen1"
    ) {
        composable(
            route = "screen1",
            deepLinks = listOf(navDeepLink { uriPattern = "$uri/screen1" })
        ) {
            ScreenContent(title = "첫 번째 화면 (Screen 1)", color = "Blue") {
                navController.navigate("screen2")
            }
        }
        composable(
            route = "screen2",
            deepLinks = listOf(navDeepLink { uriPattern = "$uri/screen2" })
        ) {
            ScreenContent(title = "두 번째 화면 (Screen 2)", color = "Green") {
                navController.navigate("screen3")
            }
        }
        composable(
            route = "screen3",
            deepLinks = listOf(navDeepLink { uriPattern = "$uri/screen3" })
        ) {
            ScreenContent(title = "세 번째 화면 (Screen 3)", color = "Red") {
                navController.navigate("screen1")
            }
        }
    }
}

@Composable
fun ScreenContent(title: String, color: String, onNext: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 24.sp)
        Text(text = "Theme: $color", modifier = Modifier.padding(8.dp))
        Button(onClick = onNext, modifier = Modifier.padding(top = 16.dp)) {
            Text("다음 화면으로 이동")
        }
    }
}

@Composable
fun ReferrerTracker() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val referrerClient = InstallReferrerClient.newBuilder(context).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                    try {
                        val referrerUrl = referrerClient.installReferrer.installReferrer
                        Log.d("INSTALL_REFERRER", "성공적으로 읽어온 데이터: $referrerUrl")
                        
                        val firebaseAnalytics = com.google.firebase.analytics.FirebaseAnalytics.getInstance(context)
                        val params = Bundle().apply { putString("referrer_url", referrerUrl) }
                        firebaseAnalytics.logEvent("app_open_from_unity", params)
                        
                        referrerClient.endConnection()
                    } catch (e: Exception) {
                        Log.e("INSTALL_REFERRER", "데이터 읽기 실패: ${e.message}")
                    }
                }
            }
            override fun onInstallReferrerServiceDisconnected() {}
        })
    }
}
