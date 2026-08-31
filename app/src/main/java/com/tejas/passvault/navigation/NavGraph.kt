package com.tejas.passvault.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tejas.passvault.VaultViewModel
import com.tejas.passvault.data.Credential
import kotlinx.coroutines.launch
import com.tejas.passvault.ui.login.ForgotPatternScreen
import com.tejas.passvault.ui.login.LoginScreen
import com.tejas.passvault.ui.onboarding.BiometricOptInScreen
import com.tejas.passvault.ui.onboarding.CreatePatternScreen
import com.tejas.passvault.ui.onboarding.SecurityQuestionsSetupScreen
import com.tejas.passvault.ui.onboarding.WelcomeScreen
import com.tejas.passvault.ui.settings.BackupExportScreen
import com.tejas.passvault.ui.settings.BackupImportScreen
import com.tejas.passvault.ui.settings.ChangePatternScreen
import com.tejas.passvault.ui.settings.LoginActivityScreen
import com.tejas.passvault.ui.settings.SettingsScreen
import com.tejas.passvault.ui.photos.PhotoVaultScreen
import com.tejas.passvault.ui.vault.AddEditEntryScreen
import com.tejas.passvault.ui.vault.EntryDetailScreen
import com.tejas.passvault.ui.vault.VaultListScreen

private const val WELCOME = "welcome"
private const val CREATE_PATTERN = "create_pattern"
private const val SECURITY_QUESTIONS_SETUP = "security_questions_setup"
private const val BIOMETRIC_OPTIN = "biometric_optin"
private const val LOGIN = "login"
private const val FORGOT_PATTERN = "forgot_pattern"
private const val VAULT_LIST = "vault_list"
private const val ENTRY_DETAIL = "entry_detail/{id}"
private const val ENTRY_FORM = "entry_form"
private const val ENTRY_FORM_EDIT = "entry_form?id={id}"
private const val SETTINGS = "settings"
private const val CHANGE_PATTERN = "change_pattern"
private const val MANAGE_SECURITY_QUESTIONS = "manage_security_questions"
private const val BACKUP_EXPORT = "backup_export"
private const val BACKUP_IMPORT = "backup_import"
private const val LOGIN_ACTIVITY = "login_activity"
private const val PHOTO_VAULT = "photo_vault"

@Composable
fun PassVaultNavGraph(vm: VaultViewModel) {
    val navController = rememberNavController()
    val isUnlocked by vm.isUnlocked.collectAsState()

    // Auto-lock: if the app was backgrounded longer than the configured timeout, lock it.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var backgroundedAtMillis: Long? = null
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> backgroundedAtMillis = System.currentTimeMillis()
                Lifecycle.Event.ON_START -> {
                    val since = backgroundedAtMillis
                    backgroundedAtMillis = null
                    if (since != null && vm.vaultRepository.isUnlocked) {
                        val elapsedSeconds = (System.currentTimeMillis() - since) / 1000
                        if (elapsedSeconds >= vm.authPrefs.autoLockSeconds) {
                            vm.lock()
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Single source of truth: whenever the vault becomes locked (auto-lock or the lock
    // button), send the user back to the login screen, clearing the protected back stack.
    LaunchedEffect(isUnlocked) {
        if (!isUnlocked && vm.isOnboarded) {
            val current = navController.currentDestination?.route
            val protectedRoutes = setOf(
                VAULT_LIST, ENTRY_DETAIL, ENTRY_FORM_EDIT, SETTINGS,
                CHANGE_PATTERN, MANAGE_SECURITY_QUESTIONS, BACKUP_EXPORT, BACKUP_IMPORT, LOGIN_ACTIVITY,
                PHOTO_VAULT
            )
            if (current in protectedRoutes) {
                navController.navigate(LOGIN) { popUpTo(0) { inclusive = true } }
            }
        }
    }

    val startDestination = if (vm.isOnboarded) LOGIN else WELCOME

    NavHost(navController = navController, startDestination = startDestination) {
        composable(WELCOME) {
            WelcomeScreen(onGetStarted = { navController.navigate(CREATE_PATTERN) })
        }
        composable(CREATE_PATTERN) {
            val scope = rememberCoroutineScope()
            CreatePatternScreen(onPatternConfirmed = { pattern ->
                scope.launch {
                    vm.beginOnboardingWithPattern(pattern)
                    navController.navigate(SECURITY_QUESTIONS_SETUP)
                }
            })
        }
        composable(SECURITY_QUESTIONS_SETUP) {
            val scope = rememberCoroutineScope()
            SecurityQuestionsSetupScreen(onDone = { questions, answers ->
                scope.launch {
                    vm.finishOnboardingSecurityQuestions(questions, answers)
                    navController.navigate(BIOMETRIC_OPTIN)
                }
            })
        }
        composable(BIOMETRIC_OPTIN) {
            BiometricOptInScreen(
                biometricAvailable = vm.isBiometricAvailable,
                onFinish = { enableBiometric ->
                    vm.finishOnboarding(enableBiometric)
                    navController.navigate(VAULT_LIST) { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable(LOGIN) {
            LoginScreen(
                vm = vm,
                onLoginSuccess = {
                    navController.navigate(VAULT_LIST) { popUpTo(0) { inclusive = true } }
                },
                onForgotPattern = { navController.navigate(FORGOT_PATTERN) }
            )
        }
        composable(FORGOT_PATTERN) {
            ForgotPatternScreen(
                vm = vm,
                onRecovered = {
                    navController.navigate(VAULT_LIST) { popUpTo(0) { inclusive = true } }
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(VAULT_LIST) {
            VaultListScreen(
                vm = vm,
                onAddEntry = { navController.navigate(ENTRY_FORM) },
                onOpenEntry = { id -> navController.navigate(ENTRY_DETAIL.replace("{id}", id)) },
                onOpenSettings = { navController.navigate(SETTINGS) },
                onViewLoginActivity = { navController.navigate(LOGIN_ACTIVITY) },
                onOpenPhotoVault = { navController.navigate(PHOTO_VAULT) },
                onLock = { vm.lock() }
            )
        }
        composable(
            ENTRY_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")
            val credentials by vm.credentials.collectAsState()
            val credential = credentials.firstOrNull { it.id == id }
            if (credential == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                EntryDetailScreen(
                    credential = credential,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("$ENTRY_FORM?id=$id") },
                    onDelete = {
                        vm.deleteCredential(credential.id)
                        navController.popBackStack()
                    }
                )
            }
        }
        composable(
            ENTRY_FORM_EDIT,
            arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")
            val credentials by vm.credentials.collectAsState()
            val existing: Credential? = credentials.firstOrNull { it.id == id }
            AddEditEntryScreen(
                existing = existing,
                onSave = { credential ->
                    vm.saveCredential(credential)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(SETTINGS) {
            SettingsScreen(
                vm = vm,
                onBack = { navController.popBackStack() },
                onChangePattern = { navController.navigate(CHANGE_PATTERN) },
                onChangeSecurityQuestions = { navController.navigate(MANAGE_SECURITY_QUESTIONS) },
                onExportBackup = { navController.navigate(BACKUP_EXPORT) },
                onImportBackup = { navController.navigate(BACKUP_IMPORT) },
                onOpenLoginActivity = { navController.navigate(LOGIN_ACTIVITY) },
                onErased = {
                    navController.navigate(WELCOME) { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable(CHANGE_PATTERN) {
            ChangePatternScreen(
                vm = vm,
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(MANAGE_SECURITY_QUESTIONS) {
            val scope = rememberCoroutineScope()
            SecurityQuestionsSetupScreen(onDone = { questions, answers ->
                scope.launch {
                    vm.changeSecurityQuestions(questions, answers)
                    navController.popBackStack()
                }
            })
        }
        composable(BACKUP_EXPORT) {
            BackupExportScreen(
                vm = vm,
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(BACKUP_IMPORT) {
            BackupImportScreen(
                vm = vm,
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(LOGIN_ACTIVITY) {
            LoginActivityScreen(
                events = vm.loginEvents(),
                onBack = { navController.popBackStack() }
            )
        }
        composable(PHOTO_VAULT) {
            PhotoVaultScreen(
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
