plugins {
    alias(libs.plugins.bodyplan.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "kr.sdbk.bodyplan.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
}
