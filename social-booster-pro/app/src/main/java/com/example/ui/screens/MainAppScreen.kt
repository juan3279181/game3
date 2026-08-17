package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.DailyBonusDialog
import com.example.ui.components.LuckyWheelDialog
import com.example.ui.components.PayPalCheckoutSheet
import com.example.ui.components.TaskVerificationDialog
import com.example.ui.components.TopCoinBar
import com.example.ui.viewmodel.FollowMeViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class AppNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    EARN("Earn", Icons.Filled.MonetizationOn, Icons.Outlined.MonetizationOn),
    CREATE("Create", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline),
    CAMPAIGNS("Campaigns", Icons.Filled.RocketLaunch, Icons.Outlined.RocketLaunch),
    SHOP("Store", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun MainAppScreen(
    viewModel: FollowMeViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by remember { mutableStateOf(AppNavTab.EARN) }

    // Collect VM state
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val campaigns by viewModel.allCampaigns.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val filteredTasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val selectedPlatform by viewModel.selectedPlatform.collectAsStateWithLifecycle()
    val selectedCampaignType by viewModel.selectedCampaignType.collectAsStateWithLifecycle()
    val isAdLoading by viewModel.isAdLoading.collectAsStateWithLifecycle()
    val isCloudConnected by viewModel.isCloudConnected.collectAsStateWithLifecycle()
    val liveCommunityCount by viewModel.liveCommunityCount.collectAsStateWithLifecycle()

    // Modals state
    val activeTask by viewModel.activeVerificationTask.collectAsStateWithLifecycle()
    val verificationSeconds by viewModel.verificationSecondsRemaining.collectAsStateWithLifecycle()
    val isLuckyWheelOpen by viewModel.isLuckyWheelOpen.collectAsStateWithLifecycle()
    val isDailyBonusOpen by viewModel.isDailyBonusOpen.collectAsStateWithLifecycle()
    val selectedPayPalPackage by viewModel.selectedPayPalPackage.collectAsStateWithLifecycle()

    // Listen to notification events
    LaunchedEffect(Unit) {
        viewModel.messageEvents.collectLatest { event ->
            snackbarHostState.showSnackbar(event.message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopCoinBar(
                coinBalance = profile?.coinsBalance ?: 0,
                dailyStreak = profile?.dailyStreak ?: 1,
                freeSpins = profile?.freeSpinsLeft ?: 0,
                onBuyCoinsClick = { currentTab = AppNavTab.SHOP },
                onDailyStreakClick = { viewModel.openDailyBonus() },
                onLuckySpinClick = { viewModel.openLuckyWheel() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                AppNavTab.values().forEach { tab ->
                    val isSelected = tab == currentTab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppNavTab.EARN -> {
                    EarnScreen(
                        tasks = filteredTasks,
                        selectedPlatform = selectedPlatform,
                        selectedCampaignType = selectedCampaignType,
                        isAdLoading = isAdLoading,
                        isCloudConnected = isCloudConnected,
                        liveCommunityCount = liveCommunityCount,
                        onSelectPlatform = { viewModel.setPlatformFilter(it) },
                        onSelectCampaignType = { viewModel.setCampaignTypeFilter(it) },
                        onStartTask = { task -> viewModel.startTask(task, context) },
                        onWatchRewardedAd = {
                            activity?.let { viewModel.watchRewardedAd(it) }
                        },
                        onOpenLuckyWheel = { viewModel.openLuckyWheel() },
                        onOpenDailyBonus = { viewModel.openDailyBonus() }
                    )
                }

                AppNavTab.CREATE -> {
                    CreateCampaignScreen(
                        userCoins = profile?.coinsBalance ?: 0,
                        onLaunchCampaign = { platform, type, title, url, cost, quantity ->
                            viewModel.createCampaign(
                                platform = platform,
                                campaignType = type,
                                title = title,
                                targetUrl = url,
                                costPerAction = cost,
                                targetQuantity = quantity,
                                onSuccess = { currentTab = AppNavTab.CAMPAIGNS }
                            )
                        },
                        onNavigateToCoinShop = { currentTab = AppNavTab.SHOP }
                    )
                }

                AppNavTab.CAMPAIGNS -> {
                    MyCampaignsScreen(
                        campaigns = campaigns,
                        onToggleCampaign = { viewModel.toggleCampaign(it) },
                        onCancelCampaign = { viewModel.cancelCampaign(it) },
                        onCreateNewCampaign = { currentTab = AppNavTab.CREATE }
                    )
                }

                AppNavTab.SHOP -> {
                    CoinShopScreen(
                        onSelectPackage = { pack -> viewModel.openPayPalPurchase(pack) },
                        onWatchAdForCoins = {
                            activity?.let { viewModel.watchRewardedAd(it) }
                        }
                    )
                }

                AppNavTab.PROFILE -> {
                    ProfileScreen(
                        profile = profile,
                        transactions = transactions,
                        onCopyAdsTxt = { msg ->
                            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                }
            }

            // Task Verification Dialog
            activeTask?.let { task ->
                TaskVerificationDialog(
                    task = task,
                    secondsRemaining = verificationSeconds,
                    onClaimReward = { viewModel.claimTaskReward() },
                    onCancel = { viewModel.cancelTaskVerification() },
                    onReopenLink = { viewModel.startTask(task, context) }
                )
            }

            // Lucky Fortune Wheel Dialog
            if (isLuckyWheelOpen) {
                LuckyWheelDialog(
                    freeSpinsLeft = profile?.freeSpinsLeft ?: 0,
                    isAdLoading = isAdLoading,
                    onDismiss = { viewModel.closeLuckyWheel() },
                    onSpinWin = { coins -> viewModel.spinWheelResult(coins) },
                    onWatchAdForSpins = {
                        activity?.let { viewModel.watchAdForSpins(it) }
                    }
                )
            }

            // Daily Bonus Dialog
            if (isDailyBonusOpen) {
                DailyBonusDialog(
                    currentStreak = profile?.dailyStreak ?: 1,
                    onDismiss = { viewModel.closeDailyBonus() },
                    onClaim = { viewModel.claimDailyBonus() }
                )
            }

            // PayPal Checkout Sheet
            selectedPayPalPackage?.let { pack ->
                PayPalCheckoutSheet(
                    pack = pack,
                    onDismiss = { viewModel.closePayPalPurchase() },
                    onConfirmPurchase = { p, txId ->
                        viewModel.confirmPayPalPayment(p, txId)
                    }
                )
            }
        }
    }
}
