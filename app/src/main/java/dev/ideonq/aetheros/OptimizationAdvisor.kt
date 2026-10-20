package dev.ideonq.aetheros

data class Advice(val title: String, val detail: String)

object OptimizationAdvisor {
    fun suggestions(profile: ExperienceProfile, isPowerSaverOn: Boolean): List<Advice> {
        val common = listOf(
            Advice("안 쓰는 앱부터 절전", "자주 사용하지 않는 앱의 배터리 설정을 개별적으로 검토하세요."),
            Advice("RAM 강제 정리 금지", "Android의 캐시 메모리를 반복 종료하면 앱 재시작 비용이 증가할 수 있어요."),
            Advice("핵심 서비스 보호", "통화·메시지·인증·은행·Google Play 서비스를 비활성화하지 않아요.")
        )
        val preferred = when (profile) {
            ExperienceProfile.BALANCED -> Advice(
                "균형 잡힌 설정",
                "필요할 때 AOD를 표시하고 적정 수준의 전환 효과를 유지하세요."
            )
            ExperienceProfile.BATTERY -> Advice(
                "화면 소비 전력 줄이기",
                "AOD 표시 시간, 화면 밝기, 배경 효과와 미사용 앱 절전을 점검하세요."
            )
            ExperienceProfile.FLUID -> Advice(
                "부드러운 상호작용",
                "주사율과 제스처 설정을 점검하고 애니메이션을 과도하게 단축하지 마세요."
            )
        }
        val power = if (isPowerSaverOn) Advice(
            "절전 모드 사용 중",
            "절전 모드가 성능과 화면 동작에 영향을 줄 수 있어요."
        ) else Advice(
            "절전 모드 꺼짐",
            "배터리 우선이라면 Android 절전 설정에서 직접 활성화할 수 있어요."
        )
        return listOf(preferred, power) + common
    }
}
