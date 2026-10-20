package dev.ideonq.aetheros

import android.content.Context

enum class ExperienceProfile(val title: String, val note: String) {
    BALANCED("Balanced", "디자인과 사용 시간을 균형 있게"),
    BATTERY("Battery", "화면과 앱 절전 설정을 점검"),
    FLUID("Fluid", "모션과 반응성 중심의 경험")
}

class AetherPreferences(context: Context) {
    private val store = context.getSharedPreferences("aether", Context.MODE_PRIVATE)

    var profile: ExperienceProfile
        get() = runCatching {
            ExperienceProfile.valueOf(store.getString("profile", "BALANCED") ?: "BALANCED")
        }.getOrDefault(ExperienceProfile.BALANCED)
        set(value) { store.edit().putString("profile", value.name).apply() }

    var glassEffects: Boolean
        get() = store.getBoolean("glassEffects", true)
        set(value) { store.edit().putBoolean("glassEffects", value).apply() }

    fun reset() = store.edit().clear().apply()
}
