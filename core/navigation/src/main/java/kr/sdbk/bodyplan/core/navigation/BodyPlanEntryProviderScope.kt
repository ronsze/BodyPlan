package kr.sdbk.bodyplan.core.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

/** feature의 `impl` 모듈이 자기 화면을 등록하는 navGraph 확장 함수의 리시버. */
typealias BodyPlanEntryProviderScope = EntryProviderScope<NavKey>
