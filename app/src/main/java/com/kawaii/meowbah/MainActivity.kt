package com.kawaii.meowbah

import android.Manifest
import android.app.NotificationManager
import android.app.Notification // Required for Notification.VISIBILITY_PUBLIC
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets // Ensure this import is present and used
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kawaii.meowbah.ui.dialogs.WelcomeDialog
import com.kawaii.meowbah.ui.screens.FanArtScreen
import com.kawaii.meowbah.ui.screens.VideosScreen
import com.kawaii.meowbah.ui.screens.merch.MerchDetailScreen
import com.kawaii.meowbah.ui.screens.merch.MerchScreen
import com.kawaii.meowbah.ui.screens.stats.StatsScreen
import com.kawaii.meowbah.ui.screens.videodetail.VideoDetailScreen
import com.kawaii.meowbah.ui.screens.videos.VideosViewModel
import com.kawaii.meowbah.ui.theme.MeowbahTheme
import com.kawaii.meowbah.workers.YoutubeSyncWorker
import java.util.concurrent.TimeUnit

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val outlinedIcon: ImageVector
) {
    object Videos : BottomNavItem("videos_tab", "Videos", Icons.Filled.Videocam, Icons.Outlined.Videocam)
    object Art : BottomNavItem(route = "art_tab", label = "Art", Icons.Filled.FormatPaint, outlinedIcon = Icons.Outlined.FormatPaint)
    object Merch : BottomNavItem("merch_tab", "Merch", Icons.Filled.Storefront, Icons.Outlined.Storefront)
    object Stats : BottomNavItem("stats_tab", "Stats", Icons.Filled.QueryStats, Icons.Outlined.QueryStats)
}

val bottomNavItems = listOf(
    BottomNavItem.Videos,
    BottomNavItem.Art,
    BottomNavItem.Merch,
    BottomNavItem.Stats
)

class MainActivity : ComponentActivity() {

    companion object {
        const val PREFS_NAME = "MeowbahAppPreferences"
        private const val KEY_WELCOME_DIALOG_SHOWN = "welcomeDialogShown"
        const val KEY_VIDEO_NOTIFICATIONS_ENABLED = "videoNotificationsEnabled"
        private const val TAG = "MainActivity"
    }

    private var initialScreenRouteFromIntent by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent) 
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "Notification permission granted")
            } else {
                Log.d(TAG, "Notification permission denied")
            }
        }

        scheduleYoutubeSync()

        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val welcomeDialogAlreadyShown = sharedPrefs.getBoolean(KEY_WELCOME_DIALOG_SHOWN, false)

        if (!welcomeDialogAlreadyShown && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            var selectedTabRoute by rememberSaveable { mutableStateOf(BottomNavItem.Videos.route) }
            val onSelectedTabRouteChange: (String) -> Unit = remember { { newRoute ->
                selectedTabRoute = newRoute
            } }

            LaunchedEffect(initialScreenRouteFromIntent) {
                initialScreenRouteFromIntent?.let {
                    route ->
                    Log.d(TAG, "LaunchedEffect: Navigating to initial tab from intent: $route")
                    onSelectedTabRouteChange(route)
                    initialScreenRouteFromIntent = null 
                }
            }

            var showWelcomeDialog by rememberSaveable { mutableStateOf(!welcomeDialogAlreadyShown) }
            val onWelcomeDialogDismissed: () -> Unit = {
                showWelcomeDialog = false
                with(sharedPrefs.edit()) {
                    putBoolean(KEY_WELCOME_DIALOG_SHOWN, true)
                    apply()
                }
            }

            MeowbahTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(
                        selectedTabRoute = selectedTabRoute,
                        onSelectedTabRouteChange = onSelectedTabRouteChange,
                        getPendingVideoId = { getPendingVideoIdFromIntent(intent) },
                        showWelcomeDialog = showWelcomeDialog,
                        onWelcomeDialogDismissed = onWelcomeDialogDismissed
                    )
                }
            }
        }
    }

    private fun getPendingVideoIdFromIntent(intent: Intent?): String? {
        return if (intent?.action == Intent.ACTION_VIEW && intent.data?.host == "www.youtube.com") {
            intent.data?.getQueryParameter("v")
        } else {
            intent?.getStringExtra("com.kawaii.meowbah.EXTRA_VIDEO_ID")
        }
    }

    private fun scheduleYoutubeSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<YoutubeSyncWorker>(
            30, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            YoutubeSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
        Log.i(TAG, "YoutubeSyncWorker (RSS) scheduled to run every 30 minutes.")
    }
}

@Composable
fun AppNavigation(
    selectedTabRoute: String,
    onSelectedTabRouteChange: (String) -> Unit,
    getPendingVideoId: () -> String?,
    showWelcomeDialog: Boolean,
    onWelcomeDialogDismissed: () -> Unit
) {
    val TAG = "AppNavigation"
    val navController = rememberNavController()

    LaunchedEffect(navController) {
        val currentRoute = navController.currentDestination?.route
        if (currentRoute != "main_screen") {
            navController.navigate("main_screen") {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(Unit) {
        val pendingVideoId = getPendingVideoId()
        if (pendingVideoId != null) {
            Log.d(TAG, "Pending video ID found: $pendingVideoId. This will be handled by MainScreen.")
        }
    }

    NavHost(navController = navController, startDestination = "main_screen") {
        composable("main_screen") {
            MainScreen(
                selectedTabRoute = selectedTabRoute,
                onSelectedTabRouteChange = onSelectedTabRouteChange,
                getPendingVideoId = getPendingVideoId
            )
        }
    }

    if (showWelcomeDialog) {
        WelcomeDialog(onDismissRequest = onWelcomeDialogDismissed)
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainScreen(
    selectedTabRoute: String,
    onSelectedTabRouteChange: (String) -> Unit,
    getPendingVideoId: () -> String?
) {
    val TAG = "MainScreen"
    val innerNavController = rememberNavController()
    val videosViewModel: VideosViewModel = viewModel()
    val merchViewModel: com.kawaii.meowbah.ui.screens.merch.MerchViewModel = viewModel()

    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentInnerDestination = navBackStackEntry?.destination

    var selectedVideoIdForSheet by remember { mutableStateOf<String?>(null) }
    var selectedMerchIdForSheet by remember { mutableStateOf<String?>(null) }

    val showBottomBar = true

    LaunchedEffect(selectedTabRoute) {
        if (innerNavController.currentDestination?.route != selectedTabRoute) {
            innerNavController.navigate(selectedTabRoute) {
                popUpTo(innerNavController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    LaunchedEffect(Unit) {
        val videoId = getPendingVideoId()
        if (videoId != null) {
            Log.d(TAG, "Pending video ID $videoId found in MainScreen. Setting selectedVideoIdForSheet.")
            if (selectedTabRoute != BottomNavItem.Videos.route) {
                onSelectedTabRouteChange(BottomNavItem.Videos.route) 
            }
            selectedVideoIdForSheet = videoId
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentInnerDestination?.hierarchy?.any { it.route == screen.route } == true || selectedTabRoute == screen.route
                        NavigationBarItem(
                            icon = { Icon(if (isSelected) screen.icon else screen.outlinedIcon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = isSelected,
                            alwaysShowLabel = true,
                            onClick = {
                                if (currentInnerDestination?.route != screen.route) {
                                    onSelectedTabRouteChange(screen.route)
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { scaffoldPaddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(scaffoldPaddingValues)) {
            NavHost(
                navController = innerNavController,
                startDestination = selectedTabRoute,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(BottomNavItem.Videos.route) {
                    VideosScreen(
                        onVideoClick = { videoId -> selectedVideoIdForSheet = videoId },
                        viewModel = videosViewModel
                    )
                }
                composable(BottomNavItem.Art.route) {
                    FanArtScreen(navController = innerNavController)
                }
                composable(BottomNavItem.Merch.route) { 
                    MerchScreen(
                        onMerchClick = { merchId -> selectedMerchIdForSheet = merchId },
                        merchViewModel = merchViewModel
                    )
                }
                composable(BottomNavItem.Stats.route) {
                    StatsScreen()
                }
            }
        }
    }

    if (selectedVideoIdForSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedVideoIdForSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ) {
            VideoDetailScreen(
                videoId = selectedVideoIdForSheet!!,
                videosViewModel = videosViewModel,
                onDismiss = { selectedVideoIdForSheet = null }
            )
        }
    }

    if (selectedMerchIdForSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedMerchIdForSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ) {
            MerchDetailScreen(
                merchId = selectedMerchIdForSheet!!,
                merchViewModel = merchViewModel,
                onDismiss = { selectedMerchIdForSheet = null }
            )
        }
    }
}
