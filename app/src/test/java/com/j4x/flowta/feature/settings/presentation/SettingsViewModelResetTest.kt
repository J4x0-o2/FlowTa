package com.j4x.flowta.feature.settings.presentation

import app.cash.turbine.test
import com.j4x.flowta.feature.settings.domain.model.UserPreferences
import com.j4x.flowta.feature.settings.domain.repository.SettingsRepository
import com.j4x.flowta.feature.settings.domain.usecase.ResetAllDataUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelResetTest {

    private val repository = mockk<SettingsRepository>(relaxed = true)
    private val resetAll = mockk<ResetAllDataUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repository.getPreferences() } returns flowOf(UserPreferences())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SettingsViewModel(repository, resetAll)

    @Test
    fun confirmIsDisabled_untilExactlyBORRAR_isTyped() {
        val vm = viewModel()
        vm.onIntent(SettingsIntent.OpenResetAll)
        assertFalse(vm.state.value.canConfirmResetAll)

        for (text in listOf("", "borrar", "BORRA", "BORRAR ", " BORRAR", "BORRARR")) {
            vm.onIntent(SettingsIntent.ResetConfirmationChanged(text))
            assertFalse("'$text' must not unlock", vm.state.value.canConfirmResetAll)
        }

        vm.onIntent(SettingsIntent.ResetConfirmationChanged("BORRAR"))
        assertTrue(vm.state.value.canConfirmResetAll)
    }

    @Test
    fun confirm_withoutTheWord_doesNothing() = runTest {
        val vm = viewModel()
        vm.onIntent(SettingsIntent.OpenResetAll)
        vm.onIntent(SettingsIntent.ResetConfirmationChanged("borrar"))

        vm.onIntent(SettingsIntent.ConfirmResetAll)

        coVerify(exactly = 0) { resetAll.invoke() }
    }

    @Test
    fun confirm_success_runsUseCase_closesDialog_andEmitsDataReset() = runTest {
        coEvery { resetAll.invoke() } returns Result.success(Unit)
        val vm = viewModel()
        vm.onIntent(SettingsIntent.OpenResetAll)
        vm.onIntent(SettingsIntent.ResetConfirmationChanged("BORRAR"))

        vm.effects.test {
            vm.onIntent(SettingsIntent.ConfirmResetAll)
            assertEquals(SettingsEffect.DataReset, awaitItem())
        }
        coVerify(exactly = 1) { resetAll.invoke() }
        assertFalse(vm.state.value.showResetAll)
        assertEquals("", vm.state.value.resetConfirmation)
    }

    @Test
    fun confirm_failure_keepsDialogOpen_withError_andNoNavigation() = runTest {
        coEvery { resetAll.invoke() } returns Result.failure(IllegalStateException("boom"))
        val vm = viewModel()
        vm.onIntent(SettingsIntent.OpenResetAll)
        vm.onIntent(SettingsIntent.ResetConfirmationChanged("BORRAR"))

        vm.effects.test {
            vm.onIntent(SettingsIntent.ConfirmResetAll)
            expectNoEvents()
        }
        assertTrue(vm.state.value.showResetAll)
        assertNotNull(vm.state.value.resetError)
        assertFalse(vm.state.value.isResetting)
    }

    @Test
    fun dismiss_closesDialog_andClearsTypedText() {
        val vm = viewModel()
        vm.onIntent(SettingsIntent.OpenResetAll)
        vm.onIntent(SettingsIntent.ResetConfirmationChanged("BORR"))

        vm.onIntent(SettingsIntent.DismissResetAll)

        assertFalse(vm.state.value.showResetAll)
        assertEquals("", vm.state.value.resetConfirmation)
        assertNull(vm.state.value.resetError)
    }
}
