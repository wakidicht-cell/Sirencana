package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.VillageRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReportPrintScreen
import com.example.ui.screens.RevenueScreen
import com.example.ui.screens.RkpdesScreen
import com.example.ui.screens.RpjmdesScreen
import com.example.ui.screens.SshScreen
import com.example.ui.theme.SiRencanaDesaTheme
import com.example.ui.viewmodel.UiState
import com.example.ui.viewmodel.VillageViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SiRencanaDesaTheme {
                AppNavigation()
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private enum class VillageDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    DASHBOARD("dashboard", "Beranda", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    RPJMDES("rpjmdes", "RPJMDes", Icons.Filled.DateRange, Icons.Outlined.DateRange),
    RKPDES("rkpdes", "RKPDes", Icons.Filled.Assignment, Icons.Outlined.Assignment),
    SSH("ssh", "SSH", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    PENDAPATAN("pendapatan", "Pendapatan", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    CETAK("cetak", "Cetak F4", Icons.Filled.Print, Icons.Outlined.Print),
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        key(user.uid) {
            AuthenticatedVillageApp(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: user.email ?: "Perangkat Desa",
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedVillageApp(
    currentUserId: String,
    userDisplayName: String,
) {
    val viewModel: VillageViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                VillageViewModel(VillageRepository(db), currentUserId)
            }
        },
    )

    val bundleState by viewModel.bundleState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: VillageDestination.DASHBOARD.route
    val currentDest = VillageDestination.entries.firstOrNull { it.route == currentRoute } ?: VillageDestination.DASHBOARD

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val activeBundle = (bundleState as? UiState.Success)?.data
    var yearMenuExpanded by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    title = {
                        Column {
                            Text(
                                text = currentDest.label,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = activeBundle?.profile?.desaName ?: userDisplayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    actions = {
                        if (activeBundle != null) {
                            Box {
                                TextButton(
                                    onClick = { yearMenuExpanded = true },
                                    modifier = Modifier.testTag("fiscal_year_picker"),
                                ) {
                                    Text("TA ${activeBundle.activeYear}")
                                }
                                DropdownMenu(
                                    expanded = yearMenuExpanded,
                                    onDismissRequest = { yearMenuExpanded = false },
                                ) {
                                    val start = activeBundle.profile.periodeStartYear
                                    (0..7).forEach { offset ->
                                        val yr = start + offset
                                        DropdownMenuItem(
                                            text = { Text("TA $yr (Thn ke-${offset + 1} RPJMDes)") },
                                            onClick = {
                                                yearMenuExpanded = false
                                                viewModel.setActiveFiscalYear(activeBundle.profile, yr)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                        IconButton(
                            onClick = {
                                signOut(credentialManager, scope)
                            },
                            modifier = Modifier.testTag("sign_out_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Keluar akun",
                            )
                        }
                    },
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        VillageDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                modifier = Modifier.testTag("nav_${dest.route}"),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(Modifier.weight(0.1f))
                        VillageDestination.entries.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                    )
                                },
                                label = { Text(dest.label) },
                                modifier = Modifier.testTag("rail_${dest.route}"),
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    when (val state = bundleState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "Memuat data perencanaan desa...",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }

                        is UiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.medium,
                                ) {
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(16.dp),
                                    )
                                }
                            }
                        }

                        is UiState.Success -> {
                            val bundle = state.data
                            NavHost(
                                navController = navController,
                                startDestination = VillageDestination.DASHBOARD.route,
                                modifier = Modifier
                                    .widthIn(max = 900.dp)
                                    .fillMaxSize(),
                            ) {
                                composable(VillageDestination.DASHBOARD.route) {
                                    DashboardScreen(
                                        bundle = bundle,
                                        onSaveProfile = viewModel::saveProfile,
                                        onNavigateModule = ::navigateTo,
                                    )
                                }
                                composable(VillageDestination.RPJMDES.route) {
                                    RpjmdesScreen(
                                        bundle = bundle,
                                        onSaveProfile = viewModel::saveProfile,
                                        onSaveProgram = viewModel::saveRpjmdesProgram,
                                        onDeleteProgram = viewModel::deleteRpjmdesProgram,
                                    )
                                }
                                composable(VillageDestination.RKPDES.route) {
                                    RkpdesScreen(
                                        bundle = bundle,
                                        onPullFromRpjmdes = viewModel::pullFromRpjmdes,
                                        onSaveActivity = viewModel::saveRkpdesActivity,
                                        onDeleteActivity = viewModel::deleteRkpdesActivity,
                                        onSaveMeta = viewModel::saveRkpdesMeta,
                                        onSaveRabItem = viewModel::saveRabItem,
                                        onDeleteRabItem = viewModel::deleteRabItem,
                                    )
                                }
                                composable(VillageDestination.SSH.route) {
                                    SshScreen(
                                        bundle = bundle,
                                        onSaveSsh = viewModel::saveSshItem,
                                        onDeleteSsh = viewModel::deleteSshItem,
                                        onLoadStandardSsh = viewModel::loadStandardSshCatalog,
                                    )
                                }
                                composable(VillageDestination.PENDAPATAN.route) {
                                    RevenueScreen(
                                        bundle = bundle,
                                        onSaveRevenue = viewModel::saveRevenue,
                                        onDeleteRevenue = viewModel::deleteRevenue,
                                    )
                                }
                                composable(VillageDestination.CETAK.route) {
                                    ReportPrintScreen(
                                        bundle = bundle,
                                        onNotify = { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
