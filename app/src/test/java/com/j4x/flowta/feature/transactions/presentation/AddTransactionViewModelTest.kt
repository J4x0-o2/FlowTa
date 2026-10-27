package com.j4x.flowta.feature.transactions.presentation

import com.j4x.flowta.core.domain.model.CurrencyCode
import com.j4x.flowta.feature.accounts.domain.model.Account
import com.j4x.flowta.feature.accounts.domain.model.AccountType
import com.j4x.flowta.feature.accounts.domain.usecase.GetAccountsUseCase
import com.j4x.flowta.feature.transactions.domain.usecase.AddTransactionUseCase
import com.j4x.flowta.feature.transfers.domain.usecase.ExecuteInternalTransferUseCase
import com.j4x.flowta.feature.types.domain.model.TransactionType
import com.j4x.flowta.feature.types.domain.model.TypeCategory
import com.j4x.flowta.feature.types.domain.usecase.GetTransactionTypesByCategoryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModelTest {

    private val getAccounts = mockk<GetAccountsUseCase>()
    private val getTypes = mockk<GetTransactionTypesByCategoryUseCase>()
    private val addTransaction = mockk<AddTransactionUseCase>()
    private val executeTransfer = mockk<ExecuteInternalTransferUseCase>()

    private fun account(id: String) = Account(id, id, AccountType.CASH, 0.0, null, null, 0L)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { getAccounts() } returns flowOf(listOf(account("a"), account("b")))
        every { getTypes(any()) } returns flowOf(listOf(TransactionType("t", "Tipo", TypeCategory.EXPENSE, 0L)))
        coEvery { addTransaction(any()) } returns Result.success(Unit)
        coEvery { executeTransfer(any()) } returns Result.success(Unit)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(code: CurrencyCode = CurrencyCode.COP) =
        AddTransactionViewModel(getAccounts, getTypes, addTransaction, executeTransfer).also {
            it.onIntent(AddTransactionIntent.SetCurrency(code))
            it.onIntent(AddTransactionIntent.SelectType("t"))
            it.onIntent(AddTransactionIntent.SelectAccount("a"))
        }

    @Test
    fun copAmountWithThousandsDot_isSubmittedAsWholeAmount() {
        val vm = viewModel()
        vm.onIntent(AddTransactionIntent.SetAmount("5.000"))

        assertEquals(5000.0, vm.state.value.parsedAmount!!, 0.0)
        assertTrue(vm.state.value.canSubmit)
        vm.onIntent(AddTransactionIntent.Submit)

        coVerify { addTransaction(match { it.amount == 5000.0 }) }
    }

    @Test
    fun copAmountWithCommaThousands_isSubmittedAsWholeAmount() {
        val vm = viewModel()
        vm.onIntent(AddTransactionIntent.SetAmount("1,000,000"))
        vm.onIntent(AddTransactionIntent.Submit)

        coVerify { addTransaction(match { it.amount == 1_000_000.0 }) }
    }

    @Test
    fun copAmountWithDecimalComma_isRejectedAndNeverSubmitted() {
        val vm = viewModel()
        vm.onIntent(AddTransactionIntent.SetAmount("5,5"))

        assertNull(vm.state.value.parsedAmount)
        assertTrue(vm.state.value.amountInvalid)
        assertFalse(vm.state.value.canSubmit)
        vm.onIntent(AddTransactionIntent.Submit)

        coVerify(exactly = 0) { addTransaction(any()) }
    }

    @Test
    fun usdAmbiguousGrouping_isRejected_andDecimalIsAccepted() {
        val vm = viewModel(CurrencyCode.USD)
        vm.onIntent(AddTransactionIntent.SetAmount("5.000"))
        assertFalse(vm.state.value.canSubmit)
        assertTrue(vm.state.value.amountInvalid)

        vm.onIntent(AddTransactionIntent.SetAmount("12.5"))
        assertTrue(vm.state.value.canSubmit)
        vm.onIntent(AddTransactionIntent.Submit)
        coVerify { addTransaction(match { it.amount == 12.5 }) }
    }

    @Test
    fun blankOrZeroAmount_isNotSubmittable_butBlankIsNotAnError() {
        val vm = viewModel()
        assertFalse(vm.state.value.canSubmit)
        assertFalse(vm.state.value.amountInvalid)

        vm.onIntent(AddTransactionIntent.SetAmount("0"))
        assertFalse(vm.state.value.canSubmit)
    }

    @Test
    fun transferTab_parsesByCurrencyToo() {
        val vm = viewModel()
        vm.onIntent(AddTransactionIntent.SelectTab(TransactionTab.TRANSFER))
        vm.onIntent(AddTransactionIntent.SelectFromAccount("a"))
        vm.onIntent(AddTransactionIntent.SelectToAccount("b"))

        vm.onIntent(AddTransactionIntent.SetAmount("2.500"))
        assertTrue(vm.state.value.canSubmit)
        vm.onIntent(AddTransactionIntent.Submit)
        coVerify { executeTransfer(match { it.amount == 2500.0 }) }

        vm.onIntent(AddTransactionIntent.SetAmount("2,5"))
        assertFalse(vm.state.value.canSubmit)
    }

    @Test
    fun currencyIsKeptAcrossTabChangeAndReset() {
        val vm = viewModel(CurrencyCode.USD)
        vm.onIntent(AddTransactionIntent.SelectTab(TransactionTab.INCOME))
        assertEquals(CurrencyCode.USD, vm.state.value.currencyCode)
        vm.onIntent(AddTransactionIntent.Reset)
        assertEquals(CurrencyCode.USD, vm.state.value.currencyCode)
    }
}
