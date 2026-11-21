package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.data.model.CenterCatalog
import com.example.data.preferences.CenterSession
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    isCloudConnected: Boolean,
    onCloudAuthSuccess: () -> Unit,
    onLoginWithCredentials: suspend (role: String, username: String, password: String) -> Result<CenterSession>,
    onRegisterParent: suspend (
        parentName: String,
        childName: String,
        ageYears: Int,
        phone: String,
        service: String,
        specialist: String,
        username: String,
        password: String,
        goals: String
    ) -> Result<CenterSession>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var selectedRoleTab by remember { mutableIntStateOf(0) } // 0 = parent, 1 = admin
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRegisterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isCloudConnected) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = onCloudAuthSuccess,
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    fun performCredentialLogin() {
        val role = if (selectedRoleTab == 1) "admin" else "parent"
        val cleanUser = username.trim()
        val cleanPass = password.trim()
        if (cleanUser.length < 2 || cleanPass.length < 4) {
            errorMessage = "يرجى إدخال اسم المستخدم وكلمة المرور."
            return
        }
        errorMessage = null
        isLoading = true

        fun runCenterLogin() {
            scope.launch {
                val result = onLoginWithCredentials(role, cleanUser, cleanPass)
                isLoading = false
                result.onFailure {
                    errorMessage = it.localizedMessage ?: "تعذر التحقق من بيانات الدخول"
                }
            }
        }

        if (isCloudConnected) {
            runCenterLogin()
        } else {
            onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    onCloudAuthSuccess()
                    runCenterLogin()
                },
                onAuthError = { msg ->
                    isLoading = false
                    errorMessage = msg
                },
                scope = scope,
                onAuthCancelled = {
                    isLoading = false
                    errorMessage = "يرجى إتمام تفعيل الاتصال السحابي للمتابعة."
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                    )
                )
            )
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .size(112.dp)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_fekr),
                    contentDescription = stringResource(id = R.string.cd_center_logo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = CenterCatalog.CENTER_NAME,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondary
            ) {
                Text(
                    text = "« ${CenterCatalog.CENTER_SLOGAN} »",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CenterCatalog.services.take(4).forEach { service ->
                    AssistChip(
                        onClick = {},
                        label = { Text(service.title, style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            labelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("دخول ولي الأمر", "دخول الإدارة").forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = selectedRoleTab == index,
                                onClick = {
                                    selectedRoleTab = index
                                    errorMessage = null
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 2)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("اسم المستخدم") },
                        placeholder = {
                            Text(if (selectedRoleTab == 1) "اسم مستخدم الإدارة" else "مثال: fekr_1042")
                        },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "إظهار أو إخفاء كلمة المرور"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { performCredentialLogin() },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_submit_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedRoleTab == 1) "دخول لوحة الإدارة" else "دخول حساب ولي الأمر",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    if (!isCloudConnected) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onCloudAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    }
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            Icon(Icons.Filled.CloudDone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تفعيل الاتصال السحابي بالجهاز (مرة واحدة)")
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Filled.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "الاتصال السحابي بالمركز مفعل",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (selectedRoleTab == 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = { showRegisterSheet = true },
                            modifier = Modifier.testTag("open_parent_register_button")
                        ) {
                            Text("ولي أمر جديد؟ إنشاء اسم مستخدم وكلمة مرور")
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "للاستفسار وطلب بيانات الدخول من الإدارة: ${CenterCatalog.CENTER_PHONE}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
    }

    if (showRegisterSheet) {
        ParentRegisterModalSheet(
            onDismiss = { showRegisterSheet = false },
            onRegister = { parentName, childName, age, phone, srv, spec, user, pass, goals ->
                errorMessage = null
                isLoading = true
                fun runReg() {
                    scope.launch {
                        val res = onRegisterParent(parentName, childName, age, phone, srv, spec, user, pass, goals)
                        isLoading = false
                        res.onSuccess {
                            showRegisterSheet = false
                        }.onFailure {
                            errorMessage = it.localizedMessage ?: "تعذر إنشاء الحساب"
                        }
                    }
                }
                if (isCloudConnected) {
                    runReg()
                } else {
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            onCloudAuthSuccess()
                            runReg()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = { isLoading = false }
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentRegisterModalSheet(
    onDismiss: () -> Unit,
    onRegister: (String, String, Int, String, String, String, String, String, String) -> Unit
) {
    var parentName by remember { mutableStateOf("") }
    var childName by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("5") }
    var phone by remember { mutableStateOf("") }
    var username by remember { mutableStateOf(CenterCatalog.generateClientUsername()) }
    var password by remember { mutableStateOf(CenterCatalog.generateClientPassword()) }
    var selectedService by remember { mutableStateOf(CenterCatalog.services.first().title) }
    var selectedSpecialist by remember { mutableStateOf(CenterCatalog.specialists.first().name) }
    var goals by remember { mutableStateOf("") }

    val valid = parentName.isNotBlank() && childName.isNotBlank() && username.trim().length >= 3 && password.trim().length >= 4

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("حساب ولي أمر جديد", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = parentName, onValueChange = { parentName = it }, label = { Text("اسم ولي الأمر") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = childName, onValueChange = { childName = it }, label = { Text("اسم الطفل") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("العمر") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(15) },
                    label = { Text("رقم الهاتف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1.6f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("اسم المستخدم للدخول") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    username = CenterCatalog.generateClientUsername()
                    password = CenterCatalog.generateClientPassword()
                }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "توليد تلقائي")
                }
            }
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("كلمة المرور للدخول") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("البرنامج المطلوب", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CenterCatalog.services) { srv ->
                    FilterChip(selected = selectedService == srv.title, onClick = { selectedService = srv.title }, label = { Text(srv.title) })
                }
            }

            Text("الأخصائي المفضل", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CenterCatalog.specialists) { sp ->
                    FilterChip(selected = selectedSpecialist == sp.name, onClick = { selectedSpecialist = sp.name }, label = { Text(sp.name) })
                }
            }

            OutlinedTextField(value = goals, onValueChange = { goals = it }, label = { Text("الهدف العلاجي (اختياري)") }, modifier = Modifier.fillMaxWidth())

            Button(
                onClick = {
                    onRegister(
                        parentName,
                        childName,
                        ageText.toIntOrNull()?.coerceIn(1, 25) ?: 5,
                        phone,
                        selectedService,
                        selectedSpecialist,
                        username,
                        password,
                        goals
                    )
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("إنشاء الحساب والدخول")
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
