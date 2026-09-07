plugins {
    alias(libs.plugins.bodyplan.jvm.library)
}

dependencies {
    implementation(libs.javax.inject)
    // Repository가 Flow를 공개 시그니처로 내므로 소비 모듈에도 보여야 한다.
    api(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
