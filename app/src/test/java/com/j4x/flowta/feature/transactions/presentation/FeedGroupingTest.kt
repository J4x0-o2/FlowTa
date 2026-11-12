package com.j4x.flowta.feature.transactions.presentation

import com.j4x.flowta.feature.transactions.domain.model.Transaction
import com.j4x.flowta.feature.transfers.domain.model.InternalTransfer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedGroupingTest {

    private fun tx(id: String, flow: String, amount: Double, type: String, at: Long) = FeedItem.Tx(
        Transaction(
            id = id, amount = amount, flowType = flow, note = null, createdAt = at,
            accountId = "a", accountName = "Cash", transactionTypeId = "t_$type", typeName = type,
            typeCategory = flow, sourceTypeId = null, sourceTypeName = null
        )
    )

    private fun xfer(id: String, amount: Double, from: String, to: String, at: Long) = FeedItem.Xfer(
        InternalTransfer(
            id = id, amount = amount, note = null, createdAt = at,
            fromAccountId = "f", fromAccountName = from, toAccountId = "t", toAccountName = to,
            transferTypeId = null
        )
    )

    private fun expense(id: String, amount: Double, type: String, at: Long) = tx(id, "EXPENSE", amount, type, at)
    private fun income(id: String, amount: Double, type: String, at: Long) = tx(id, "INCOME", amount, type, at)

    @Test
    fun emptyFeedYieldsNoEntries() {
        assertTrue(groupFeed(emptyList()).isEmpty())
    }

    @Test
    fun singleItemStaysSingle() {
        val item = expense("e1", 10.0, "Mercado", 100)
        val entries = groupFeed(listOf(item))
        assertEquals(listOf<FeedEntry>(FeedEntry.Single(item)), entries)
        assertEquals("e1", entries[0].key)
    }

    @Test
    fun twoConsecutiveOfSameTypeCollapseIntoGroup() {
        val a = expense("e2", 5.0, "Café", 200)
        val b = expense("e1", 10.0, "Mercado", 100)
        val entries = groupFeed(listOf(a, b))
        assertEquals(1, entries.size)
        val group = entries[0] as FeedEntry.Group
        assertEquals(listOf<FeedItem>(a, b), group.items)
        assertEquals("EXPENSE", group.flowType)
        assertEquals(2, group.count)
    }

    @Test
    fun differentTypeBreaksTheStreak() {
        val feed = listOf(
            expense("e3", 1.0, "A", 300),
            expense("e2", 1.0, "A", 200),
            income("i1", 50.0, "Salario", 150),
            expense("e1", 1.0, "A", 100)
        )
        val entries = groupFeed(feed)
        assertEquals(3, entries.size)
        assertTrue(entries[0] is FeedEntry.Group)
        assertEquals(FeedEntry.Single(feed[2]), entries[1])
        assertEquals(FeedEntry.Single(feed[3]), entries[2])
    }

    @Test
    fun threeStreaksProduceThreeGroups() {
        val feed = listOf(
            expense("e2", 1.0, "A", 600), expense("e1", 1.0, "A", 500),
            income("i2", 1.0, "S", 400), income("i1", 1.0, "S", 300),
            xfer("x2", 1.0, "Cash", "Bank", 200), xfer("x1", 1.0, "Cash", "Bank", 100)
        )
        val entries = groupFeed(feed)
        assertEquals(3, entries.size)
        assertEquals(
            listOf("EXPENSE", "INCOME", "TRANSFER"),
            entries.map { (it as FeedEntry.Group).flowType }
        )
    }

    @Test
    fun groupTotalSumsAmounts() {
        val entries = groupFeed(
            listOf(expense("e3", 10.5, "A", 3), expense("e2", 20.0, "B", 2), expense("e1", 4.5, "C", 1))
        )
        assertEquals(35.0, (entries[0] as FeedEntry.Group).total, 0.0001)
    }

    @Test
    fun categoriesAreDistinctInFeedOrder() {
        val entries = groupFeed(
            listOf(
                expense("e4", 1.0, "Mercado", 4), expense("e3", 1.0, "Transporte", 3),
                expense("e2", 1.0, "Mercado", 2), expense("e1", 1.0, "Café", 1)
            )
        )
        assertEquals(listOf("Mercado", "Transporte", "Café"), (entries[0] as FeedEntry.Group).categories)
    }

    @Test
    fun transferGroupCategoriesAreTheRoutes() {
        val entries = groupFeed(
            listOf(xfer("x2", 1.0, "Cash", "Bank", 2), xfer("x1", 1.0, "Bank", "Ahorros", 1))
        )
        assertEquals(listOf("Cash → Bank", "Bank → Ahorros"), (entries[0] as FeedEntry.Group).categories)
    }

    @Test
    fun timeRangeSpansOldestToNewest() {
        // Feed is newest-first.
        val entries = groupFeed(
            listOf(expense("e3", 1.0, "A", 900), expense("e2", 1.0, "A", 500), expense("e1", 1.0, "A", 100))
        )
        val group = entries[0] as FeedEntry.Group
        assertEquals(100L, group.fromMillis)
        assertEquals(900L, group.toMillis)
    }

    @Test
    fun keyIsTheOldestItemIdSoNewArrivalsKeepItStable() {
        val older = expense("e1", 1.0, "A", 100)
        val mid = expense("e2", 1.0, "A", 200)
        val before = groupFeed(listOf(mid, older))[0]
        val after = groupFeed(listOf(expense("e3", 1.0, "A", 300), mid, older))[0]
        assertEquals("group_e1", before.key)
        assertEquals(before.key, after.key)
    }

    @Test
    fun singleKeyDiffersFromGroupKeyOfSameItem() {
        val only = groupFeed(listOf(expense("e1", 1.0, "A", 100)))[0]
        val grouped = groupFeed(listOf(expense("e2", 1.0, "A", 200), expense("e1", 1.0, "A", 100)))[0]
        assertTrue(only.key != grouped.key)
    }

    // --- reconcileExpansion ---

    private fun groupOf(vararg ids: String) =
        groupFeed(ids.mapIndexed { i, id -> expense(id, 1.0, "A", 1000L - i) }).single()

    @Test
    fun reconcile_keepsKeysThatStillExist() {
        val g = groupOf("e3", "e2", "e1")
        assertEquals(listOf(g.key), reconcileExpansion(listOf(g.key), listOf(g), listOf(g)))
    }

    @Test
    fun reconcile_dropsKeysWhoseGroupVanished() {
        val g = groupOf("e3", "e2")
        assertEquals(emptyList<String>(), reconcileExpansion(listOf(g.key), listOf(g), emptyList()))
    }

    @Test
    fun reconcile_followsGroupWhenOldestMovementIsDeleted() {
        val before = groupOf("e3", "e2", "e1")
        val after = groupOf("e3", "e2")
        assertTrue(before.key != after.key)
        assertEquals(listOf(after.key), reconcileExpansion(listOf(before.key), listOf(before), listOf(after)))
    }

    @Test
    fun reconcile_doesNotExpandUnrelatedGroups() {
        val before = groupOf("e3", "e2")
        val other = groupOf("x2", "x1")
        assertEquals(emptyList<String>(), reconcileExpansion(listOf(before.key), listOf(before), listOf(other)))
    }
}
