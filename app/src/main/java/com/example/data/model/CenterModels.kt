package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.security.MessageDigest
import kotlin.random.Random

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class CenterServiceItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val sessionDurationMin: Int,
    val feeEgp: Int,
    val categoryTag: String,
    val imageKey: String = ""
)

data class SpecialistProfile(
    val id: String,
    val name: String,
    val roleTitle: String,
    val specialties: List<String>,
    val experienceYears: Int
)

data class CenterPackageItem(
    val id: String,
    val title: String,
    val priceEgp: Int,
    val savingText: String
)

data class CustomActionButton(
    val id: String,
    val label: String,
    val actionType: String, // "url" | "phone" | "whatsapp" | "book" | "chat" | "payments" | "map"
    val targetValue: String,
    val style: String = "secondary" // "primary" | "secondary" | "outlined"
)

data class CenterConfig(
    val centerName: String = CenterCatalog.CENTER_NAME,
    val centerSlogan: String = CenterCatalog.CENTER_SLOGAN,
    val centerPhone: String = CenterCatalog.CENTER_PHONE,
    val centerAddress: String = CenterCatalog.CENTER_ADDRESS,
    val centerHours: String = CenterCatalog.CENTER_HOURS,
    val centerLat: Double = CenterCatalog.CENTER_LAT,
    val centerLng: Double = CenterCatalog.CENTER_LNG,
    val welcomeNote: String = "",
    val bookButtonLabel: String = "احجز جلسة",
    val posterBannerTitle: String = "رؤيتنا: ابنك معانا بطل",
    val showPosterBanner: Boolean = true,
    val services: List<CenterServiceItem> = CenterCatalog.services,
    val specialists: List<SpecialistProfile> = CenterCatalog.specialists,
    val packages: List<CenterPackageItem> = CenterCatalog.packages,
    val timeSlots: List<String> = CenterCatalog.timeSlots,
    val customButtons: List<CustomActionButton> = CenterCatalog.defaultCustomButtons
)

data class CreatedClientCredentials(
    val studentId: String,
    val childName: String,
    val parentName: String,
    val username: String,
    val password: String
)

object CenterCatalog {
    const val CENTER_NAME = "مركز فكر للقياس والتقويم النفسي"
    const val CENTER_SLOGAN = "ابنك معانا بطل"
    const val CENTER_PHONE = "01006478010"
    const val CENTER_ADDRESS = "شارع التحرير الرئيسي، برج الأطباء - الدور الثالث"
    const val CENTER_HOURS = "السبت - الخميس: 10:00 صباحاً - 9:00 مساءً"
    const val CENTER_LAT = 30.0444
    const val CENTER_LNG = 31.2357
    const val BOOTSTRAP_ADMIN_EMAIL = "amadkor8686@gmail.com"
    const val BOOTSTRAP_ADMIN_HASH = "30abc011a5732e2ef1238986671a09d8f13421cdeef7d1f1b615a3f2860e98c4"

    fun normalizeUsername(raw: String): String =
        raw.trim().lowercase().replace(Regex("\\s+"), "_")

    fun sha256Hex(username: String, password: String): String {
        val normalized = "${normalizeUsername(username)}:${password.trim()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun generateClientUsername(): String =
        "fekr_${Random.nextInt(1000, 9999)}"

    fun generateClientPassword(): String =
        Random.nextInt(100000, 999999).toString()

    val services: List<CenterServiceItem> = listOf(
        CenterServiceItem(
            id = "srv_speech",
            title = "تخاطب",
            subtitle = "تأهيل النطق واللغة",
            description = "جلسات فردية متخصصة لعلاج تأخر الكلام، التلعثم، اللدغات، واضطرابات التواصل اللفظي واللغوي.",
            sessionDurationMin = 45,
            feeEgp = 0,
            categoryTag = "أساسي",
            imageKey = "speech"
        ),
        CenterServiceItem(
            id = "srv_psych_test",
            title = "قياس نفسي",
            subtitle = "اختبارات الذكاء والتقييم",
            description = "تطبيق مقاييس ستانفورد بينيه، فايلاند للسلوك التكيفي، ومقاييس فرط الحركة وتشتت الانتباه.",
            sessionDurationMin = 60,
            feeEgp = 0,
            categoryTag = "تشخيصي",
            imageKey = "psych"
        ),
        CenterServiceItem(
            id = "srv_behavior",
            title = "تعديل سلوك",
            subtitle = "بناء السلوك الإيجابي",
            description = "برامج علمية لمعالجة العناد، فرط الحركة، العدوانية، والقلق لدى الأطفال والمراهقين.",
            sessionDurationMin = 45,
            feeEgp = 0,
            categoryTag = "سلوكي",
            imageKey = "behavior"
        ),
        CenterServiceItem(
            id = "srv_mental_math",
            title = "حساب ذهني",
            subtitle = "تنشيط الذاكرة والتركيز",
            description = "تدريب الأطفال على العمليات الحسابية الذهنية السريعة لتقوية الانتباه وسرعة البديهة.",
            sessionDurationMin = 50,
            feeEgp = 0,
            categoryTag = "مهاري",
            imageKey = "math"
        ),
        CenterServiceItem(
            id = "srv_guidance",
            title = "إرشاد ديني ومعرفي",
            subtitle = "تنمية القيم والوعي",
            description = "غرس القيم الأخلاقية والسلوكية السليمة وبناء الثقة بالنفس عبر أساليب تربوية محببة للأطفال.",
            sessionDurationMin = 45,
            feeEgp = 0,
            categoryTag = "تربوي",
            imageKey = "guidance"
        ),
        CenterServiceItem(
            id = "srv_learning",
            title = "صعوبات التعلم",
            subtitle = "القراءة والكتابة والإملاء",
            description = "خطط علاجية فردية لصعوبات القراءة (الديسلكسيا)، الكتابة، والحساب وتشتت الانتباه الدراسي.",
            sessionDurationMin = 50,
            feeEgp = 0,
            categoryTag = "أكاديمي",
            imageKey = "learning"
        ),
        CenterServiceItem(
            id = "srv_early",
            title = "تدخل مبكر",
            subtitle = "من عمر سنتين إلى 5 سنوات",
            description = "برامج تحفيز نمائية مكثفة لاكتشاف وعلاج التأخر النمائي في المراحل العمرية المبكرة.",
            sessionDurationMin = 45,
            feeEgp = 0,
            categoryTag = "نمائي",
            imageKey = "early"
        ),
        CenterServiceItem(
            id = "srv_skills",
            title = "تنمية مهارات",
            subtitle = "المهارات الحركية والإدراكية",
            description = "تطوير التآزر البصري الحركي، المهارات الدقيقة، والقدرات الاجتماعية والتفاعلية للطفل.",
            sessionDurationMin = 45,
            feeEgp = 0,
            categoryTag = "مهاري",
            imageKey = "skills"
        )
    )

    val specialists: List<SpecialistProfile> = listOf(
        SpecialistProfile(
            id = "spec_ahmad",
            name = "أ / أحمد عوف",
            roleTitle = "مدير المركز وأخصائي القياس النفسي والتخاطب",
            specialties = listOf("تخاطب", "قياس نفسي", "تعديل سلوك"),
            experienceYears = 12
        ),
        SpecialistProfile(
            id = "spec_sara",
            name = "د / سارة محمود",
            roleTitle = "أخصائية التخاطب والتدخل المبكر",
            specialties = listOf("تخاطب", "تدخل مبكر", "تنمية مهارات"),
            experienceYears = 8
        ),
        SpecialistProfile(
            id = "spec_karim",
            name = "أ / كريم حسن",
            roleTitle = "أخصائي صعوبات التعلم والحساب الذهني",
            specialties = listOf("صعوبات التعلم", "حساب ذهني", "إرشاد ديني ومعرفي"),
            experienceYears = 7
        )
    )

    val packages: List<CenterPackageItem> = listOf(
        CenterPackageItem(
            id = "pkg_4",
            title = "باقة 4 جلسات تخاطب",
            priceEgp = 900,
            savingText = "وفر 100 ج.م"
        ),
        CenterPackageItem(
            id = "pkg_8",
            title = "باقة 8 جلسات شاملة",
            priceEgp = 1750,
            savingText = "وفر 250 ج.م"
        )
    )

    val timeSlots: List<String> = listOf(
        "10:00 ص",
        "12:00 م",
        "02:00 م",
        "04:00 م",
        "06:00 م",
        "08:00 م"
    )

    val defaultCustomButtons: List<CustomActionButton> = listOf(
        CustomActionButton(
            id = "btn_whatsapp",
            label = "تواصل واتساب",
            actionType = "whatsapp",
            targetValue = CENTER_PHONE,
            style = "secondary"
        )
    )

    val customButtonActionTypes: List<Pair<String, String>> = listOf(
        "whatsapp" to "واتساب",
        "phone" to "اتصال هاتفي",
        "url" to "رابط خارجي",
        "book" to "حجز جلسة",
        "chat" to "محادثة الأخصائي",
        "payments" to "الدفع الإلكتروني",
        "map" to "موقع المركز"
    )

    fun customButtonActionLabel(actionType: String): String =
        customButtonActionTypes.firstOrNull { it.first == actionType }?.second ?: "رابط خارجي"

    fun defaultConfig(): CenterConfig = CenterConfig()

    val caseFileCategories: List<Pair<String, String>> = listOf(
        "report" to "تقرير تشخيصي",
        "plan" to "خطة علاجية",
        "assessment" to "اختبار ومقياس",
        "worksheet" to "تمارين منزلية",
        "other" to "مرفق عام"
    )

    fun caseFileCategoryLabel(category: String): String =
        caseFileCategories.firstOrNull { it.first == category }?.second ?: "مرفق عام"
}

@Keep
@IgnoreExtraProperties
data class AccessCode(
    @DocumentId val id: String = "",
    val username: String = "",
    val role: String = "parent",
    val studentId: String = "",
    val displayName: String = "",
    val linkedUid: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "parent",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudentCase(
    @DocumentId val id: String = "",
    val parentUid: String = "",
    val parentName: String = "",
    val parentPhone: String = "",
    val childName: String = "",
    val ageYears: Long = 5L,
    val primaryService: String = "تخاطب",
    val specialistName: String = "أ / أحمد عوف",
    val caseStatus: String = "active",
    val progressPercent: Long = 0L,
    val totalSessions: Long = 0L,
    val attendedSessions: Long = 0L,
    val clinicalNotes: String = "",
    val currentGoals: String = "",
    val clientUsername: String = "",
    val clientPassword: String = "",
    val credentialHash: String = "",
    val pendingSyncRefs: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Appointment(
    @DocumentId val id: String = "",
    val parentUid: String = "",
    val studentId: String = "",
    val childName: String = "",
    val serviceName: String = "",
    val specialistName: String = "",
    val dateLabel: String = "",
    val timeSlot: String = "",
    val scheduledEpochMs: Long = 0L,
    val status: String = "pending",
    val paymentStatus: String = "unpaid",
    val feeAmount: Long = 250L,
    val notes: String = "",
    val reminderMinutesBefore: Long = 120L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MonthlyEvaluation(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val parentUid: String = "",
    val childName: String = "",
    val monthLabel: String = "",
    val specialistName: String = "",
    val speechScore: Long = 0L,
    val attentionScore: Long = 0L,
    val behaviorScore: Long = 0L,
    val cognitiveScore: Long = 0L,
    val motorScore: Long = 0L,
    val overallScore: Long = 0L,
    val summary: String = "",
    val homeRecommendations: String = "",
    val parentAcknowledged: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentRecord(
    @DocumentId val id: String = "",
    val parentUid: String = "",
    val appointmentId: String = "",
    val childName: String = "",
    val description: String = "",
    val amountEgp: Long = 0L,
    val method: String = "card",
    val referenceCode: String = "",
    val status: String = "completed",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val parentUid: String = "",
    val parentName: String = "",
    val specialistId: String = "",
    val specialistName: String = "",
    val lastMessage: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val parentUid: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val senderRole: String = "parent",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SmartNotification(
    @DocumentId val id: String = "",
    val parentUid: String = "",
    val studentId: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "reminder",
    val read: Boolean = false,
    val scheduledForLabel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CaseFile(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val parentUid: String = "",
    val childName: String = "",
    val title: String = "",
    val category: String = "report",
    val notes: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val fileSizeBytes: Long = 0L,
    val contentBase64: String = "",
    val externalUrl: String = "",
    val uploadedByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
