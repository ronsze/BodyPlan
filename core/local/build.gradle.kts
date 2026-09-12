plugins {
    alias(libs.plugins.bodyplan.android.library)
    alias(libs.plugins.bodyplan.android.hilt)
    // 백업 스냅샷이 Entity를 그대로 JSON으로 담는다.
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "kr.sdbk.bodyplan.core.local"
}

// 스키마를 파일로 남겨 이후 마이그레이션의 근거로 삼는다.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // seed와 컬럼 값이 도메인 enum 이름을 그대로 쓴다. 문자열로 베껴 두면 이름이 바뀔 때 조용히 깨진다.
    implementation(project(":core:domain"))
    implementation(libs.room.runtime)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.room.compiler)
}
