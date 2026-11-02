package com.j4x.flowta.feature.integration.presentation

import app.cash.turbine.test
import com.j4x.flowta.feature.accounts.domain.model.Account
import com.j4x.flowta.feature.accounts.domain.model.AccountType
import com.j4x.flowta.feature.accounts.domain.usecase.GetAccountsUseCase
import com.j4x.flowta.feature.integration.domain.model.IntegrationLink
import com.j4x.flowta.feature.integration.domain.repository.IntegrationLinkRepository
import com.j4x.flowta.feature.integration.domain.usecase.CreateIncomeTypeUseCase
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
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectSourceViewModelTest {

    private val source = "com.micuadre"
    private val getAccounts = mockk<GetAccountsUseCase>()
    private val getTypes = mockk<GetTransactionTypesByCategoryUseCase>()
    private val createType = mockk<CreateIncomeTypeUseCase>()
    private val links = mockk<IntegrationLinkRepository>()
    private val clock = Clock.fixed(Instant.ofEpochMilli(1_700_000_000_000L), ZoneOffset.UTC)

    private fun account(id: String, type: AccountType) = Account(id, id, type, 0.0, null, null, 0L)
    private val cash = account("cash", AccountType.CASH)
    private val bank = account("bank", AccountType.BANK)
    private val wallet = account("wallet", AccountType.DIGITAL_WALLET)
    private val trips = TransactionType("trips", "Viajes", TypeCategory.INCOME, 0L)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { getAccounts() } returns flowOf(listOf(bank, cash, wallet))
        every { getTypes("INCOME") } returns flowOf(listOf(trips))
        coEvery { links.get(source) } returns null
        coEvery { links.save(any()) } returns Unit
        coEvery { createType(any()) } returns Result.success("created")
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = ConnectSourceViewModel(getAccounts, getTypes, createType, links, clock)
        .also { it.onIntent(ConnectSourceIntent.Initialize(source)) }

    @Test
    fun freshConnection_preselectsCashAndWalletAccounts_andNoType() {
        val s = vm().state.value

        assertEquals("cash", s.cashAccountId)
        assertEquals("wallet", s.transferAccountId)
        assertNull(s.selectedTypeId)
        assertFalse(s.canConnect)
    }

    @Test
    fun transferFallsBackToBank_whenNoWallet() {
        every { getAccounts() } returns flowOf(listOf(bank, cash))
        assertEquals("bank", vm().state.value.transferAccountId)
    }

    @Test
    fun withoutCashOrWalletOrBank_nothingPreselected() {
        every { getAccounts() } returns flowOf(listOf(account("s", AccountType.SAVINGS)))
        val s = vm().state.value

        assertNull(s.cashAccountId)
        assertNull(s.transferAccountId)
        assertFalse(s.canConnect)
    }

    @Test
    fun existingLink_winsOverDefaults() {
        coEvery { links.get(source) } returns IntegrationLink(source, "trips", "bank", "cash", 1L)
        val s = vm().state.value

        assertEquals("trips", s.selectedTypeId)
        assertEquals("bank", s.cashAccountId)
        assertEquals("cash", s.transferAccountId)
        assertTrue(s.canConnect)
    }

    @Test
    fun existingLinkWithDeletedAccount_fallsBackToDefaultForThatAccount() {
        coEvery { links.get(source) } returns IntegrationLink(source, "trips", "gone", "bank", 1L)
        val s = vm().state.value

        assertEquals("cash", s.cashAccountId)
        assertEquals("bank", s.transferAccountId)
    }

    @Test
    fun noAccounts_showsEmptyState_andCannotConnect() {
        every { getAccounts() } returns flowOf(emptyList())
        val s = vm().state.value

        assertTrue(s.loaded)
        assertTrue(s.noAccounts)
        assertFalse(s.canConnect)
    }

    @Test
    fun newTypeRequiresNonBlankName() {
        val vm = vm()
        vm.onIntent(ConnectSourceIntent.SelectNewType)
        assertEquals("Carreras", vm.state.value.newTypeName)
        assertTrue(vm.state.value.canConnect)

        vm.onIntent(ConnectSourceIntent.SetNewTypeName("  "))
        assertFalse(vm.state.value.canConnect)
    }

    @Test
    fun saveWithExistingType_savesLinkAndEmitsConnected() = runTest {
        val vm = vm()
        vm.onIntent(ConnectSourceIntent.SelectType("trips"))

        vm.effects.test {
            vm.onIntent(ConnectSourceIntent.Connect)
            assertEquals(ConnectSourceEffect.Connected, awaitItem())
        }
        coVerify { links.save(IntegrationLink(source, "trips", "cash", "wallet", 1_700_000_000_000L)) }
        coVerify(exactly = 0) { createType(any()) }
    }

    @Test
    fun saveWithNewType_createsTypeThenLinksIt() = runTest {
        val vm = vm()
        vm.onIntent(ConnectSourceIntent.SelectNewType)
        vm.onIntent(ConnectSourceIntent.SetNewTypeName("Mis viajes"))

        vm.effects.test {
            vm.onIntent(ConnectSourceIntent.Connect)
            assertEquals(ConnectSourceEffect.Connected, awaitItem())
        }
        coVerify { createType("Mis viajes") }
        coVerify { links.save(IntegrationLink(source, "created", "cash", "wallet", 1_700_000_000_000L)) }
    }

    @Test
    fun saveFailure_emitsError_staysOnScreen() = runTest {
        coEvery { links.save(any()) } throws IllegalStateException("disk")
        val vm = vm()
        vm.onIntent(ConnectSourceIntent.SelectType("trips"))

        vm.effects.test {
            vm.onIntent(ConnectSourceIntent.Connect)
            assertTrue(awaitItem() is ConnectSourceEffect.ShowError)
            expectNoEvents()
        }
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun typeCreationFailure_emitsError_andDoesNotSaveLink() = runTest {
        coEvery { createType(any()) } returns Result.failure(IllegalStateException("x"))
        val vm = vm()
        vm.onIntent(ConnectSourceIntent.SelectNewType)

        vm.effects.test {
            vm.onIntent(ConnectSourceIntent.Connect)
            assertTrue(awaitItem() is ConnectSourceEffect.ShowError)
        }
        coVerify(exactly = 0) { links.save(any()) }
    }

    @Test
    fun connectWhileIncomplete_doesNothing() {
        val vm = vm()
        vm.onIntent(ConnectSourceIntent.Connect)

        coVerify(exactly = 0) { links.save(any()) }
        assertFalse(vm.state.value.isSaving)
    }
}
