plugins {
    alias(libs.plugins.bodyplan.android.library)
    alias(libs.plugins.bodyplan.android.hilt)
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
    ksp(libs.room.compiler)
}
