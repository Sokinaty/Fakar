package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.signOut
import com.example.data.model.Appointment
import com.example.data.model.CenterServiceItem
import com.example.data.model.UiState
import com.example.data.preferences.CenterSession
import com.example.data.preferences.CenterSessionPreferences
import com.example.data.preferences.ReminderPreferences
import com.example.data.repository.FekrRepository
import com.example.ui.components.StatusPill
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ConnectScreen
import com.example.ui.screens.EvaluationsAndPaymentsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotificationsModalSheet
import com.example.ui.screens.ScheduleAndCasesScreen
import com.example.ui.viewmodel.FekrViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

const val ROUTE_HOME = "home"
const val ROUTE_SCHEDULE = "schedule"
const val ROUTE_EVAL_PAY = "eval_pay"
const val ROUTE_CONNECT = "connect"
const val ROUTE_ADMIN = "admin"

private data class NavTabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionPrefs = remember { CenterSessionPreferences(context.applicationContext) }
    val session by sessionPrefs.sessionFlow.collectAsStateWithLifecycle(initialValue = CenterSession())
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null || !session.isLoggedIn) {
        val authRepo = remember(user?.uid) {
            if (user != null) FekrRepository(context.applicationContext) else null
        }
        AuthScreen(
            isCloudConnected = user != null,
            onCloudAuthSuccess = { currentUser = auth.currentUser },
            onLoginWithCredentials = { role, username, password ->
                val repo = authRepo ?: FekrRepository(context.applicationContext)
                val res = repo.loginWithUsernamePassword(role, username, password)
                res.onSuccess { newSession ->
                    sessionPrefs.saveSession(newSession)
                }
                res
            },
            onRegisterParent = { pName, cName, age, phone, srv, spec, username, password, goals ->
                val repo = authRepo ?: FekrRepository(context.applicationContext)
                val res = repo.registerParentWithCredentials(
                    parentName = pName,
                    childName = cName,
                    ageYears = age,
                    parentPhone = phone,
                    primaryService = srv,
                    specialistName = spec,
                    username = username,
                    password = password,
                    goals = goals
                )
                res.onSuccess { newSession ->
                    sessionPrefs.saveSession(newSession)
                }
                res
            }
        )
    } else {
        val uid = user.uid
        val sessionKey = "${uid}_${session.role}_${session.username}_${session.studentId}"
        key(sessionKey) {
            AuthenticatedFekrApp(
                currentUserId = uid,
                sessionKey = sessionKey,
                session = session,
                onSwitchCenterAccount = {
                    scope.launch { sessionPrefs.clearSession() }
                },
                onFullDeviceSignOut = {
                    scope.launch { sessionPrefs.clearSession() }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedFekrApp(
    currentUserId: String,
    sessionKey: String,
    session: CenterSession,
    onSwitchCenterAccount: () -> Unit,
    onFullDeviceSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: FekrViewModel = viewModel(
        key = sessionKey,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FekrViewModel(
                    repository = FekrRepository(db),
                    reminderPreferences = ReminderPreferences(app),
                    currentUserId = currentUserId,
                    userDisplayName = session.displayName.ifBlank { "ولي أمر" },
                    isAdminSession = session.role == "admin",
                    activeStudentId = session.studentId
                )
            }
        }
    )

    val isAdmin = viewModel.isAdminSession
    val studentsState by viewModel.studentsState.collectAsStateWithLifecycle()
    val appointmentsState by viewModel.appointmentsState.collectAsStateWithLifecycle()
    val evaluationsState by viewModel.evaluationsState.collectAsStateWithLifecycle()
    val paymentsState by viewModel.paymentsState.collectAsStateWithLifecycle()
    val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
    val chatMessagesState by viewModel.chatMessagesState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val caseFilesState by viewModel.caseFilesState.collectAsStateWithLifecycle()
    val reminderSettings by viewModel.reminderSettings.collectAsStateWithLifecycle()
    val centerConfig by viewModel.centerConfig.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val lastCreatedCredentials by viewModel.lastCreatedCredentials.collectAsStateWithLifecycle()

    var currentRoute by remember { mutableStateOf(if (isAdmin) ROUTE_ADMIN else ROUTE_HOME) }
    var scheduleSubTab by remember { mutableIntStateOf(0) }
    var preselectedService by remember { mutableStateOf<CenterServiceItem?>(null) }
    var openBookingTrigger by remember { mutableStateOf(false) }
    var evalPaySubTab by remember { mutableIntStateOf(0) }
    var pendingPaymentAppt by remember { mutableStateOf<Appointment?>(null) }
    var connectSubTab by remember { mutableIntStateOf(0) }
    var selectedSpecialistId by remember { mutableStateOf(centerConfig.specialists.firstOrNull()?.id ?: "spec_1") }
    var selectedSpecialistName by remember { mutableStateOf(centerConfig.specialists.firstOrNull()?.name ?: "د. أحمد عوف") }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(centerConfig.specialists) {
        val current = centerConfig.specialists.firstOrNull { it.id == selectedSpecialistId }
        if (current != null) {
            selectedSpecialistName = current.name
        } else {
            centerConfig.specialists.firstOrNull()?.let { first ->
                selectedSpecialistId = first.id
                selectedSpecialistName = first.name
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    if (currentRoute != ROUTE_HOME) {
        BackHandler { currentRoute = ROUTE_HOME }
    }

    val unreadCount = (notificationsState as? UiState.Success)?.data?.count { !it.read } ?: 0

    val navItems = buildList {
        add(NavTabItem(ROUTE_HOME, "الرئيسية", Icons.Filled.Home))
        add(NavTabItem(ROUTE_SCHEDULE, "المواعيد", Icons.Filled.CalendarMonth))
        add(NavTabItem(ROUTE_EVAL_PAY, "التقييم والدفع", Icons.Filled.Assessment))
        add(NavTabItem(ROUTE_CONNECT, "تواصل وخريطة", Icons.AutoMirrored.Filled.Chat))
        if (isAdmin) {
            add(NavTabItem(ROUTE_ADMIN, "الإدارة", Icons.Filled.AdminPanelSettings))
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    navigationIcon = {
                        IconButton(onClick = { showProfileDialog = true }) {
                            Image(
                                painter = painterResource(id = R.drawable.logo_fekr),
                                contentDescription = stringResource(id = R.string.cd_center_logo),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                            )
                        }
                    },
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = centerConfig.centerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isAdmin) "الإدارة (@${session.username})" else "ولي الأمر (@${session.username})",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAdmin) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                connectSubTab = 1
                                currentRoute = ROUTE_CONNECT
                            }
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = "خريطة المركز")
                        }
                        IconButton(onClick = { showNotificationsSheet = true }) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge { Text("$unreadCount") }
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Notifications, contentDescription = "الإشعارات الذكية")
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        navItems.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick = { currentRoute = item.route },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = {
                                    Text(
                                        item.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        Spacer(modifier = Modifier.height(12.dp))
                        navItems.forEach { item ->
                            NavigationRailItem(
                                selected = currentRoute == item.route,
                                onClick = { currentRoute = item.route },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }

                when (currentRoute) {
                    ROUTE_HOME -> HomeScreen(
                        userName = viewModel.userDisplayName,
                        centerConfig = centerConfig,
                        studentsState = studentsState,
                        appointmentsState = appointmentsState,
                        evaluationsState = evaluationsState,
                        caseFilesState = caseFilesState,
                        onBookServiceClick = { service ->
                            preselectedService = service
                            openBookingTrigger = true
                            scheduleSubTab = 0
                            currentRoute = ROUTE_SCHEDULE
                        },
                        onOpenScheduleTab = {
                            scheduleSubTab = 0
                            currentRoute = ROUTE_SCHEDULE
                        },
                        onOpenCasesTab = {
                            scheduleSubTab = 1
                            currentRoute = ROUTE_SCHEDULE
                        },
                        onOpenEvaluationsTab = {
                            evalPaySubTab = 0
                            currentRoute = ROUTE_EVAL_PAY
                        },
                        onOpenPaymentsTab = {
                            evalPaySubTab = 1
                            currentRoute = ROUTE_EVAL_PAY
                        },
                        onOpenChatWithSpecialist = { specId, specName ->
                            selectedSpecialistId = specId
                            selectedSpecialistName = specName
                            connectSubTab = 0
                            currentRoute = ROUTE_CONNECT
                        },
                        onOpenMapTab = {
                            connectSubTab = 1
                            currentRoute = ROUTE_CONNECT
                        },
                        onOpenNotifications = { showNotificationsSheet = true },
                        onShowToast = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )

                    ROUTE_SCHEDULE -> ScheduleAndCasesScreen(
                        centerConfig = centerConfig,
                        studentsState = studentsState,
                        appointmentsState = appointmentsState,
                        caseFilesState = caseFilesState,
                        reminderSettings = reminderSettings,
                        initialSubTab = scheduleSubTab,
                        preselectedService = preselectedService,
                        openBookingInitially = openBookingTrigger,
                        onClearBookingTrigger = {
                            openBookingTrigger = false
                            preselectedService = null
                        },
                        onCreateStudent = { name, age, phone, srv, spec, goals ->
                            viewModel.createStudent(name, age, phone, srv, spec, goals)
                        },
                        onBookAppointment = { stu, srv, fee, spec, date, slot, epoch, notes, lead, cb ->
                            viewModel.bookAppointment(stu, srv, fee, spec, date, slot, epoch, notes, lead, cb)
                        },
                        onCancelAppointment = { viewModel.cancelAppointmentParent(it) },
                        onPayAppointment = { appt ->
                            pendingPaymentAppt = appt
                            evalPaySubTab = 1
                            currentRoute = ROUTE_EVAL_PAY
                        },
                        onReminderScheduled = { title, body, label ->
                            viewModel.logSmartReminder(title, body, label)
                        },
                        onShowToast = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )

                    ROUTE_EVAL_PAY -> EvaluationsAndPaymentsScreen(
                        initialSubTab = evalPaySubTab,
                        appointmentsState = appointmentsState,
                        evaluationsState = evaluationsState,
                        paymentsState = paymentsState,
                        pendingPaymentAppointment = pendingPaymentAppt,
                        onClearPendingPayment = { pendingPaymentAppt = null },
                        onAcknowledgeEvaluation = { viewModel.acknowledgeEvaluation(it) },
                        onPay = { appt, child, desc, amt, method, cb ->
                            viewModel.payForSessionOrPackage(appt, child, desc, amt, method, cb)
                        }
                    )

                    ROUTE_CONNECT -> ConnectScreen(
                        initialSubTab = connectSubTab,
                        centerConfig = centerConfig,
                        currentUserId = currentUserId,
                        userDisplayName = viewModel.userDisplayName,
                        isAdmin = isAdmin,
                        chatsState = chatsState,
                        chatMessagesState = chatMessagesState,
                        selectedSpecialistId = selectedSpecialistId,
                        selectedSpecialistName = selectedSpecialistName,
                        onSelectChatThread = { chatId, _, _, _, _ ->
                            viewModel.selectChat(chatId)
                        },
                        onSendMessage = { chatId, pUid, pName, sId, sName, text ->
                            viewModel.sendChatMessage(chatId, pUid, pName, sId, sName, text)
                        }
                    )

                    ROUTE_ADMIN -> if (isAdmin) {
                        AdminDashboardScreen(
                            centerConfig = centerConfig,
                            studentsState = studentsState,
                            appointmentsState = appointmentsState,
                            paymentsState = paymentsState,
                            caseFilesState = caseFilesState,
                            lastCreatedCredentials = lastCreatedCredentials,
                            onDismissCreatedCredentials = { viewModel.clearCreatedCredentials() },
                            onCreateStudentAdmin = { child, age, pName, phone, srv, spec, goals, uName, pass ->
                                viewModel.createStudentAdmin(child, age, pName, phone, srv, spec, goals, uName, pass)
                            },
                            onCreateAdminAccount = { user, pass, name ->
                                viewModel.createAdminAccount(user, pass, name)
                            },
                            onUpdateAttendance = { appt, status ->
                                viewModel.updateAppointmentStatusAdmin(appt, status)
                            },
                            onSendFeeNotice = { stu, appt, desc, amount, notes ->
                                viewModel.sendFeeNoticeAdmin(stu, appt, desc, amount, notes)
                            },
                            onUpdateStudent = { id, status, prog, spec, notes, goals ->
                                viewModel.updateStudentAdmin(id, status, prog, spec, notes, goals)
                            },
                            onPublishEvaluation = { stu, month, spec, s1, s2, s3, s4, s5, summary, homeRec ->
                                viewModel.publishMonthlyEvaluation(stu, month, spec, s1, s2, s3, s4, s5, summary, homeRec)
                            },
                            onAddCaseFile = { stu, title, cat, notes, fName, mime, fSize, b64, url ->
                                viewModel.addCaseFileAdmin(stu, title, cat, notes, fName, mime, fSize, b64, url)
                            },
                            onDeleteCaseFile = { fileId ->
                                viewModel.deleteCaseFileAdmin(fileId)
                            },
                            onSaveCenterConfig = { cfg, msg ->
                                viewModel.saveCenterConfigAdmin(cfg, msg)
                            },
                            onResetCenterConfig = {
                                viewModel.resetCenterConfigAdmin()
                            },
                            onShowToast = { msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showNotificationsSheet) {
        NotificationsModalSheet(
            notificationsState = notificationsState,
            reminderSettings = reminderSettings,
            onDismiss = { showNotificationsSheet = false },
            onUpdateSettings = { en, mins, tip -> viewModel.updateReminderSettings(en, mins, tip) },
            onMarkRead = { viewModel.markNotificationRead(it) },
            onLogTestNotification = { t, b, l -> viewModel.logSmartReminder(t, b, l) }
        )
    }

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            title = { Text(session.displayName.ifBlank { "حساب المركز" }) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اسم المستخدم: @${session.username}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    StatusPill(
                        label = if (isAdmin) "حساب إدارة / أخصائي" else "حساب ولي أمر",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "${centerConfig.centerName}\nهاتف: ${centerConfig.centerPhone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showProfileDialog = false
                        onSwitchCenterAccount()
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("خروج من الحساب", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            showProfileDialog = false
                            onFullDeviceSignOut()
                            signOut(credentialManager = credentialManager, scope = scope)
                        }
                    ) {
                        Text("تبديل الجهاز", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { showProfileDialog = false }) { Text("إغلاق") }
                }
            }
        )
    }
}
