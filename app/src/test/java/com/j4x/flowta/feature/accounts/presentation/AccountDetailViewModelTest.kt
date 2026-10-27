package com.j4x.flowta.feature.accounts.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.j4x.flowta.feature.accounts.domain.model.Account
import com.j4x.flowta.feature.accounts.domain.model.AccountType
import com.j4x.flowta.feature.accounts.domain.repository.AccountRepository
import com.j4x.flowta.feature.accounts.domain.usecase.CountAccountMovementsUseCase
import com.j4x.flowta.feature.accounts.domain.usecase.ResetAccountUseCase
import com.j4x.flowta.feature.transactions.domain.usecase.GetAccountTransactionsUseCase
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountDetailViewModelTest {

    private val accounts = mockk<AccountRepository>()
    private val transactions = mockk<GetAccountTransactionsUseCase>()
    private val count = mockk<CountAccountMovementsUseCase>()
    private val reset = mockk<ResetAccountUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { accounts.getById("a") } returns flowOf(Account("a", "Cash", AccountType.CASH, 10.0, null, null, 1))
        every { transactions.invoke("a") } returns flowOf(emptyList())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = AccountDetailViewModel(
        SavedStateHandle(mapOf("accountId" to "a")), accounts, transactions, count, reset
    )

    @Test
    fun requestReset_loadsMovementCountIntoDialog() = runTest {
        coEvery { count("a") } returns Result.success(7L)
        val vm = viewModel()

        vm.requestReset()

        assertEquals(ResetAccountDialog(movementCount = 7L), vm.state.value.resetDialog)
    }

    @Test
    fun requestReset_failure_emitsErrorAndOpensNoDialog() = runTest {
        coEvery { count("a") } returns Result.failure(IllegalStateException("db down"))
        val vm = viewModel()

        vm.effects.test {
            vm.requestReset()
            assertEquals(AccountDetailEffect.ShowMessage("db down"), awaitItem())
        }
        assertNull(vm.state.value.resetDialog)
    }

    @Test
    fun confirmReset_success_resetsWithAmount_closesDialog_andEmitsSnackbar() = runTest {
        coEvery { count("a") } returns Result.success(1L)
        coEvery { reset("a", 5000.0) } returns Result.success(Unit)
        val vm = viewModel()
        vm.requestReset()

        vm.effects.test {
            vm.confirmReset(5000.0)
            assertEquals(AccountDetailEffect.ShowMessage("Cuenta reiniciada"), awaitItem())
        }
        coVerify(exactly = 1) { reset("a", 5000.0) }
        assertNull(vm.state.value.resetDialog)
    }

    @Test
    fun confirmReset_failure_emitsError_andClosesDialog() = runTest {
        coEvery { count("a") } returns Result.success(1L)
        coEvery { reset("a", 0.0) } returns Result.failure(IllegalStateException("boom"))
        val vm = viewModel()
        vm.requestReset()

        vm.effects.test {
            vm.confirmReset(0.0)
            assertEquals(AccountDetailEffect.ShowMessage("boom"), awaitItem())
        }
        assertNull(vm.state.value.resetDialog)
    }

    @Test
    fun dismissReset_closesDialogWithoutResetting() = runTest {
        coEvery { count("a") } returns Result.success(1L)
        val vm = viewModel()
        vm.requestReset()

        vm.dismissReset()

        assertNull(vm.state.value.resetDialog)
        coVerify(exactly = 0) { reset(any(), any()) }
    }
}
