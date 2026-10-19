package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.screens.CompanyAdminScreen
import com.example.ui.screens.KdsScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.MenuAdminScreen
import com.example.ui.screens.PosCashScreen
import com.example.ui.screens.StorefrontScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppMode
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = mainViewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val currentMode by viewModel.currentMode.collectAsState()
    val selectedCompany by viewModel.selectedCompany.collectAsState()
    val allCompanies by viewModel.allCompanies.collectAsState()

    val activeCompany = selectedCompany ?: allCompanies.firstOrNull()

    // Handle back button for secondary screens
    BackHandler(enabled = currentMode != AppMode.MARKETPLACE) {
        viewModel.setMode(AppMode.MARKETPLACE)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = currentMode == AppMode.MARKETPLACE,
                    onClick = { viewModel.setMode(AppMode.MARKETPLACE) },
                    icon = { Icon(Icons.Default.Storefront, contentDescription = "Catálogo") },
                    label = { Text("Catálogo", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_marketplace")
                )

                NavigationBarItem(
                    selected = currentMode == AppMode.STOREFRONT,
                    onClick = { viewModel.setMode(AppMode.STOREFRONT) },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Tienda") },
                    label = { Text("Tienda", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_storefront")
                )

                NavigationBarItem(
                    selected = currentMode == AppMode.KDS,
                    onClick = { viewModel.setMode(AppMode.KDS) },
                    icon = { Icon(Icons.Default.LocalFireDepartment, contentDescription = "Cocina KDS") },
                    label = { Text("Cocina KDS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFF9800),
                        indicatorColor = Color(0xFFFFE0B2)
                    ),
                    modifier = Modifier.testTag("nav_tab_kds")
                )

                NavigationBarItem(
                    selected = currentMode == AppMode.POS_CASH,
                    onClick = { viewModel.setMode(AppMode.POS_CASH) },
                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = "Caja & POS") },
                    label = { Text("Caja POS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF2E7D32),
                        indicatorColor = Color(0xFFE8F5E9)
                    ),
                    modifier = Modifier.testTag("nav_tab_pos")
                )

                NavigationBarItem(
                    selected = currentMode == AppMode.ADMIN_COMPANIES || currentMode == AppMode.ADMIN_MENU,
                    onClick = { viewModel.setMode(AppMode.ADMIN_COMPANIES) },
                    icon = { Icon(Icons.Default.CorporateFare, contentDescription = "Administración") },
                    label = { Text("Locales", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_admin")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentMode) {
                AppMode.MARKETPLACE -> {
                    MarketplaceScreen(viewModel = viewModel)
                }
                AppMode.STOREFRONT -> {
                    if (activeCompany != null) {
                        StorefrontScreen(
                            viewModel = viewModel,
                            company = activeCompany,
                            onBackToMarketplace = { viewModel.setMode(AppMode.MARKETPLACE) }
                        )
                    } else {
                        MarketplaceScreen(viewModel = viewModel)
                    }
                }
                AppMode.KDS -> {
                    KdsScreen(viewModel = viewModel)
                }
                AppMode.POS_CASH -> {
                    PosCashScreen(viewModel = viewModel)
                }
                AppMode.ADMIN_MENU -> {
                    MenuAdminScreen(viewModel = viewModel)
                }
                AppMode.ADMIN_COMPANIES -> {
                    CompanyAdminScreen(viewModel = viewModel)
                }
            }
        }
    }
}
