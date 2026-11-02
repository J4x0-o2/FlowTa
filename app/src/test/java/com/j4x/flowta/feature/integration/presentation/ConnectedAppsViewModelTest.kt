package com.j4x.flowta.feature.integration.presentation

import app.cash.turbine.test
import com.j4x.flowta.feature.accounts.domain.model.Account
import com.j4x.flowta.feature.accounts.domain.model.AccountType
import com.j4x.flowta.feature.accounts.domain.usecase.GetAccountsUseCase
import com.j4x.flowta.feature.integration.domain.model.IntegrationLink
import com.j4x.flowta.feature.integration.domain.repository.IntegrationLinkRepository
import com.j4x.flowta.feature.types.domain.model.TransactionType
import com.j4x.flowta.feature.types.domain.model.TypeCategory
import com.j4x.flowta.feature.types.domain.usecase.GetTransactionTypesByCategoryUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectedAppsViewModelTest {

    private class FakeLinks(initial: List<IntegrationLink>) : IntegrationLinkRepository {
        val all = MutableStateFlow(initial)
        var deleteFailure: Exception? = null
        val deleted = mutableListOf<String>()

        override suspend fun get(sourceApp: String) = all.value.firstOrNull { it.sourceApp == sourceApp }
        override fun observe(sourceApp: String): Flow<IntegrationLink?> = error("unused")
        override fun observeAll(): Flow<List<IntegrationLink>> = all
        override suspend fun save(link: IntegrationLink) = error("unused")
        override suspend fun delete(sourceApp: String) {
            deleteFailure?.let { throw it }
            deleted += sourceApp
            all.value = all.value.filterNot { it.sourceApp == sourceApp }
        }
    }

    private val labels = object : AppLabelResolver {
        override fun label(packageName: String) = if (packageName == "com.micuadre.app") "MiCuadre" else packageName
    }

    private val source = "com.micuadre.app"
    private val link = IntegrationLink(source, "trips", "cash", "bank", 1L)
    private val accounts = MutableStateFlow(listOf(account("cash", "Efectivo"), account("bank", "Nequi")))
    private val types = MutableStateFlow(listOf(TransactionType("trips", "Carreras", TypeCategory.INCOME, 0L)))
    private val getAccounts = mockk<GetAccountsUseCase>()
    private val getTypes = mockk<GetTransactionTypesByCategoryUseCase>()

    private fun account(id: String, name: String) = Account(id, name, AccountType.CASH, 0.0, null, null, 0L)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { getAccounts() } returns accounts
        every { getTypes("INCOME") } returns types
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(links: FakeLinks) = ConnectedAppsViewModel(links, getAccounts, getTypes, labels)

    @Test
    fun noLinks_cardIsHidden() {
        assertTrue(vm(FakeLinks(emptyList())).state.value.apps.isEmpty())
    }

    @Test
    fun link_isShownWithResolvedLabelTypeAndAccountNames() {
        val app = vm(FakeLinks(listOf(link))).state.value.apps.single()

        assertEquals(source, app.sourceApp)
        assertEquals("MiCuadre", app.label)
        assertEquals("Carreras", app.typeName)
        assertEquals("Efectivo", app.cashAccountName)
        assertEquals("Nequi", app.transferAccountName)
        assertFalse(app.needsReconnect)
    }

    @Test
    fun unknownPackage_fallsBackToPackageNameAsLabel() {
        val other = link.copy(sourceApp = "com.other")
        assertEquals("com.other", vm(FakeLinks(listOf(other))).state.value.apps.single().label)
    }

    @Test
    fun deletedReferences_areNullAndFlagReconnect() {
        accounts.value = listOf(account("cash", "Efectivo"))
        types.value = emptyList()
        val app = vm(FakeLinks(listOf(link))).state.value.apps.single()

        assertNull(app.typeName)
        assertEquals("Efectivo", app.cashAccountName)
        assertNull(app.transferAccountName)
        assertTrue(app.needsReconnect)
    }

    @Test
    fun cardUpdates_whenAnAccountIsRenamedOrDeleted() {
        val vm = vm(FakeLinks(listOf(link)))
        accounts.value = listOf(account("cash", "Caja"), account("bank", "Nequi"))
        assertEquals("Caja", vm.state.value.apps.single().cashAccountName)

        accounts.value = listOf(account("bank", "Nequi"))
        assertNull(vm.state.value.apps.single().cashAccountName)
    }

    @Test
    fun requestThenConfirm_deletesLinkAndRowDisappears() {
        val links = FakeLinks(listOf(link))
        val vm = vm(links)

        vm.onIntent(ConnectedAppsIntent.RequestDisconnect(source))
        assertEquals(source, vm.state.value.pendingDisconnect?.sourceApp)
        assertTrue(links.deleted.isEmpty())

        vm.onIntent(ConnectedAppsIntent.ConfirmDisconnect)
        assertEquals(listOf(source), links.deleted)
        assertTrue(vm.state.value.apps.isEmpty())
        assertNull(vm.state.value.pendingDisconnect)
    }

    @Test
    fun dismiss_clearsPending_withoutDeleting() {
        val links = FakeLinks(listOf(link))
        val vm = vm(links)

        vm.onIntent(ConnectedAppsIntent.RequestDisconnect(source))
        vm.onIntent(ConnectedAppsIntent.DismissDisconnect)

        assertNull(vm.state.value.pendingDisconnect)
        assertTrue(links.deleted.isEmpty())
        assertEquals(1, vm.state.value.apps.size)
    }

    @Test
    fun confirmWithoutRequest_doesNothing() {
        val links = FakeLinks(listOf(link))
        vm(links).onIntent(ConnectedAppsIntent.ConfirmDisconnect)
        assertTrue(links.deleted.isEmpty())
    }

    @Test
    fun deleteFailure_emitsError_andKeepsTheRow() = runTest {
        val links = FakeLinks(listOf(link)).apply { deleteFailure = IllegalStateException("disk") }
        val vm = vm(links)
        vm.onIntent(ConnectedAppsIntent.RequestDisconnect(source))

        vm.effects.test {
            vm.onIntent(ConnectedAppsIntent.ConfirmDisconnect)
            assertTrue(awaitItem() is ConnectedAppsEffect.ShowError)
        }
        assertEquals(1, vm.state.value.apps.size)
        assertNull(vm.state.value.pendingDisconnect)
    }

    @Test
    fun linksFlowFailure_emitsError_insteadOfCrashing() = runTest {
        val broken = object : IntegrationLinkRepository by FakeLinks(emptyList()) {
            override fun observeAll(): Flow<List<IntegrationLink>> = kotlinx.coroutines.flow.flow {
                throw IllegalStateException("db")
            }
        }
        val vm = ConnectedAppsViewModel(broken, getAccounts, getTypes, labels)

        vm.effects.test { assertTrue(awaitItem() is ConnectedAppsEffect.ShowError) }
        assertTrue(vm.state.value.apps.isEmpty())
    }
}
