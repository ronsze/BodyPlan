package kr.sdbk.bodyplan.feature.my.impl

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kr.sdbk.bodyplan.core.navigation.BodyPlanEntryProviderScope
import kr.sdbk.bodyplan.core.navigation.BodyPlanNavigator
import kr.sdbk.bodyplan.feature.my.api.AiTokenNavKey
import kr.sdbk.bodyplan.feature.my.api.InbodyNavKey
import kr.sdbk.bodyplan.feature.my.api.MyNavKey
import kr.sdbk.bodyplan.feature.my.api.OnboardingNavKey
import kr.sdbk.bodyplan.feature.my.api.ProfileNavKey
import kr.sdbk.bodyplan.feature.my.api.WeightNavKey
import kr.sdbk.bodyplan.feature.my.api.navigateToAiToken
import kr.sdbk.bodyplan.feature.my.api.navigateToInbody
import kr.sdbk.bodyplan.feature.my.api.navigateToProfile
import kr.sdbk.bodyplan.feature.my.api.navigateToWeight
import kr.sdbk.bodyplan.feature.my.impl.aitoken.composable.AiTokenEvents
import kr.sdbk.bodyplan.feature.my.impl.aitoken.composable.AiTokenView
import kr.sdbk.bodyplan.feature.my.impl.home.composable.MyEvents
import kr.sdbk.bodyplan.feature.my.impl.home.composable.MyView
import kr.sdbk.bodyplan.feature.my.impl.inbody.composable.InbodyEvents
import kr.sdbk.bodyplan.feature.my.impl.inbody.composable.InbodyView
import kr.sdbk.bodyplan.feature.my.impl.onboarding.composable.OnboardingView
import kr.sdbk.bodyplan.feature.my.impl.profile.composable.ProfileEvents
import kr.sdbk.bodyplan.feature.my.impl.profile.composable.ProfileView
import kr.sdbk.bodyplan.feature.my.impl.weight.composable.WeightEvents
import kr.sdbk.bodyplan.feature.my.impl.weight.composable.WeightView

fun BodyPlanEntryProviderScope.myNavGraph(navigator: BodyPlanNavigator) {
    entry<OnboardingNavKey> {
        // 완료 표시가 켜지면 앱 껍데기가 다음 화면으로 옮긴다. 이 화면은 어디로 갈지 모른다.
        OnboardingView(viewModel = hiltViewModel())
    }

    entry<MyNavKey> {
        val events = remember {
            MyEvents(
                goToAiToken = navigator::navigateToAiToken,
                goToProfile = navigator::navigateToProfile,
                goToInbody = navigator::navigateToInbody,
                goToWeight = navigator::navigateToWeight,
            )
        }
        MyView(events = events, viewModel = hiltViewModel())
    }

    entry<ProfileNavKey> {
        val events = remember { ProfileEvents(goBack = navigator::goBack) }
        ProfileView(events = events, viewModel = hiltViewModel())
    }

    entry<InbodyNavKey> {
        val events = remember {
            InbodyEvents(
                goBack = navigator::goBack,
                goToAiToken = navigator::navigateToAiToken,
            )
        }
        InbodyView(events = events, viewModel = hiltViewModel())
    }

    entry<WeightNavKey> {
        val events = remember { WeightEvents(goBack = navigator::goBack) }
        WeightView(events = events, viewModel = hiltViewModel())
    }

    entry<AiTokenNavKey> {
        val events = remember { AiTokenEvents(goBack = navigator::goBack) }
        AiTokenView(events = events, viewModel = hiltViewModel())
    }
}
