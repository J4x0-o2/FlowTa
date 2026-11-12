package com.j4x.flowta.feature.transactions.presentation

/** Flow type label shared with the bubble colors: INCOME, EXPENSE or TRANSFER. */
val FeedItem.flowType: String
    get() = when (this) {
        is FeedItem.Tx   -> transaction.flowType
        is FeedItem.Xfer -> "TRANSFER"
    }

val FeedItem.amount: Double
    get() = when (this) {
        is FeedItem.Tx   -> transaction.amount
        is FeedItem.Xfer -> transfer.amount
    }

/** Category name for movements, "from -> to" route for transfers (same label the bubble shows). */
val FeedItem.categoryLabel: String
    get() = when (this) {
        is FeedItem.Tx   -> transaction.typeName
        is FeedItem.Xfer -> "${transfer.fromAccountName} → ${transfer.toAccountName}"
    }

/** One row of the main feed: a lone movement or a collapsed streak of the same flow type. */
sealed interface FeedEntry {
    /** Stable LazyColumn key and expansion key. */
    val key: String

    data class Single(val item: FeedItem) : FeedEntry {
        override val key: String get() = item.id
    }

    /** [items] keep feed order (newest first). */
    data class Group(val items: List<FeedItem>) : FeedEntry {
        init {
            require(items.size >= 2) { "A group needs at least two movements" }
        }

        val flowType: String = items.first().flowType
        val count: Int = items.size
        val total: Double = items.sumOf { it.amount }
        val categories: List<String> = items.map { it.categoryLabel }.distinct()
        val fromMillis: Long = items.minOf { it.createdAt }
        val toMillis: Long = items.maxOf { it.createdAt }

        // Anchored on the oldest movement: newer arrivals join the streak without changing the key.
        override val key: String = "group_${items.last().id}"
    }
}

/** Collapses runs of 2+ consecutive same-flow movements, preserving feed order. */
fun groupFeed(items: List<FeedItem>): List<FeedEntry> {
    val entries = ArrayList<FeedEntry>()
    var run = ArrayList<FeedItem>()

    fun flush() {
        when (run.size) {
            0    -> Unit
            1    -> entries += FeedEntry.Single(run[0])
            else -> entries += FeedEntry.Group(run)
        }
        run = ArrayList()
    }

    for (item in items) {
        if (run.isNotEmpty() && run[0].flowType != item.flowType) flush()
        run += item
    }
    flush()
    return entries
}
