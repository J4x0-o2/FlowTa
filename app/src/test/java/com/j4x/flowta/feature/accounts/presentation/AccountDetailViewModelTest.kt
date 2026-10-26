package com.j4x.flowta.feature.accounts.presentation

import com.j4x.flowta.core.domain.model.CurrencyCode
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

    private fun cop(text: String) = parseStartingBalance(text, CurrencyCode.COP)
    private fun usd(text: String) = parseStartingBalance(text, CurrencyCode.USD)

    @Test
    fun parseStartingBalance_blankIsZero_forEveryCurrency() {
        assertEquals(0.0, cop("")!!, 0.0)
        assertEquals(0.0, cop("  ")!!, 0.0)
        assertEquals(0.0, usd("")!!, 0.0)
    }

    @Test
    fun parseStartingBalance_cop_separatorsAreThousandsGroupingNeverDecimals() {
        assertEquals(5000.0, cop("5000")!!, 0.0)
        assertEquals(5000.0, cop("5.000")!!, 0.0)
        assertEquals(5000.0, cop("5,000")!!, 0.0)
        assertEquals(1_000_000.0, cop("1.000.000")!!, 0.0)
        assertEquals(1_000_000.0, cop("1,000,000")!!, 0.0)
        assertEquals(1500.0, cop("1.500")!!, 0.0)
    }

    @Test
    fun parseStartingBalance_cop_rejectsAmbiguousOrMalformedInput() {
        assertNull(cop("5,5"))        // decimals do not exist in COP
        assertNull(cop("12.50"))
        assertNull(cop("1.000,50"))
        assertNull(cop("1.00.000"))   // bad grouping
        assertNull(cop("1000.000"))   // first group too long
        assertNull(cop("5.0000"))
        assertNull(cop("5."))
        assertNull(cop(".5"))
        assertNull(cop("abc"))
        assertNull(cop("-3"))
    }

    @Test
    fun parseStartingBalance_usd_acceptsOneDecimalSeparatorWithOneOrTwoDigits() {
        assertEquals(12.5, usd("12.5")!!, 0.0)
        assertEquals(12.5, usd("12,5")!!, 0.0)
        assertEquals(12.34, usd("12.34")!!, 0.0)
        assertEquals(5000.0, usd("5000")!!, 0.0)
    }

    @Test
    fun parseStartingBalance_usd_groupingIsAcceptedWhenUnambiguous() {
        assertEquals(1_000_000.0, usd("1.000.000")!!, 0.0)
        assertEquals(1_000_000.0, usd("1,000,000")!!, 0.0)
        assertEquals(1234.56, usd("1,234.56")!!, 0.0)
        assertEquals(1234.56, usd("1.234,56")!!, 0.0)
    }

    @Test
    fun parseStartingBalance_usd_rejectsAmbiguousOrMalformedInput() {
        assertNull(usd("5.000"))      // 5 or 5000? do not guess
        assertNull(usd("5,000"))
        assertNull(usd("12.345"))
        assertNull(usd("1.234,567"))
        assertNull(usd("1,234.5.6"))
        assertNull(usd("1.000.00"))
        assertNull(usd("5."))
        assertNull(usd("abc"))
        assertNull(usd("-3"))
    }
}
