package com.example.ui.components

import androidx.annotation.DrawableRes
import com.example.R
import com.example.data.model.CenterServiceItem

object ServiceArtwork {
    val imageChoices: List<Pair<String, String>> = listOf(
        "speech" to "تخاطب ونطق",
        "psych" to "قياس نفسي",
        "behavior" to "تعديل سلوك",
        "math" to "حساب ذهني",
        "guidance" to "إرشاد معرفي",
        "learning" to "صعوبات تعلم",
        "early" to "تدخل مبكر",
        "skills" to "تنمية مهارات"
    )

    @DrawableRes
    fun drawableForKey(key: String): Int = when (key) {
        "speech" -> R.drawable.img_srv_speech
        "psych" -> R.drawable.img_srv_psych
        "behavior" -> R.drawable.img_srv_behavior
        "math" -> R.drawable.img_srv_math
        "guidance" -> R.drawable.img_srv_guidance
        "learning" -> R.drawable.img_srv_learning
        "early" -> R.drawable.img_srv_early
        "skills" -> R.drawable.img_srv_skills
        else -> R.drawable.img_srv_speech
    }

    @DrawableRes
    fun drawableForService(service: CenterServiceItem): Int {
        if (service.imageKey.isNotBlank()) {
            return drawableForKey(service.imageKey)
        }
        val id = service.id.lowercase()
        val title = service.title
        return when {
            id == "srv_speech" || "تخاطب" in title || "نطق" in title -> R.drawable.img_srv_speech
            id == "srv_psych_test" || "قياس" in title || "نفس" in title -> R.drawable.img_srv_psych
            id == "srv_behavior" || "سلوك" in title -> R.drawable.img_srv_behavior
            id == "srv_mental_math" || "حساب" in title || "ذهني" in title -> R.drawable.img_srv_math
            id == "srv_guidance" || "إرشاد" in title || "ديني" in title -> R.drawable.img_srv_guidance
            id == "srv_learning" || "تعلم" in title || "قراءة" in title -> R.drawable.img_srv_learning
            id == "srv_early" || "مبكر" in title -> R.drawable.img_srv_early
            id == "srv_skills" || "مهار" in title -> R.drawable.img_srv_skills
            else -> R.drawable.img_srv_skills
        }
    }

    fun inferKeyForService(service: CenterServiceItem): String {
        if (service.imageKey.isNotBlank()) return service.imageKey
        val id = service.id.lowercase()
        val title = service.title
        return when {
            id == "srv_speech" || "تخاطب" in title -> "speech"
            id == "srv_psych_test" || "قياس" in title -> "psych"
            id == "srv_behavior" || "سلوك" in title -> "behavior"
            id == "srv_mental_math" || "حساب" in title -> "math"
            id == "srv_guidance" || "إرشاد" in title -> "guidance"
            id == "srv_learning" || "تعلم" in title -> "learning"
            id == "srv_early" || "مبكر" in title -> "early"
            else -> "skills"
        }
    }
}
