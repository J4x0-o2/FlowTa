package com.j4x.flowta.core.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Colors for categories. They are derived from the category id (no schema change), so a category keeps
 * the same color on every screen and across sessions.
 */
internal val categoryPalette = listOf(
    Color(0xFFE74C3C), Color(0xFF3498DB), Color(0xFF3A9564), Color(0xFFF39C12),
    Color(0xFF9B59B6), Color(0xFF1ABC9C), Color(0xFFE67E22), Color(0xFF34495E),
    Color(0xFFE91E63), Color(0xFF8BC34A), Color(0xFF5C6BC0), Color(0xFF95A5A6)
)

internal val CATEGORY_PALETTE_SIZE: Int get() = categoryPalette.size

/** Palette slot of [id]: deterministic (String.hashCode is specified) and always inside the palette. */
internal fun categoryColorIndex(id: String): Int = (id.hashCode() and Int.MAX_VALUE) % categoryPalette.size

fun categoryColor(id: String): Color = categoryPalette[categoryColorIndex(id)]

/** Greens for income sources, a separate palette so they read as "money in" next to the category colors. */
internal val incomeSourcePalette = listOf(
    Color(0xFF2E7D32), Color(0xFF43A047), Color(0xFF66BB6A), Color(0xFF00897B),
    Color(0xFF26A69A), Color(0xFF7CB342), Color(0xFF9CCC65), Color(0xFF00695C)
)

/** Income that has no source. */
internal val NoSourceColor = Color(0xFF95A5A6)

/** Stable color of an income source; null (no source) is neutral gray. */
fun incomeSourceColor(id: String?): Color =
    if (id == null) NoSourceColor else incomeSourcePalette[(id.hashCode() and Int.MAX_VALUE) % incomeSourcePalette.size]
