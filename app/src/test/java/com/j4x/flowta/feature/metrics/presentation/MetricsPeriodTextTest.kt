package com.j4x.flowta.feature.metrics.presentation

import com.j4x.flowta.core.domain.model.CurrencyCode
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriod
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType
import com.j4x.flowta.feature.metrics.domain.model.PercentChange
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType.DAY
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType.MONTH
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType.WEEK
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType.YEAR
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class MetricsPeriodTextTest {

    private val today = LocalDate.parse("2026-11-11") // Wednesday

    private fun period(type: MetricsPeriodType, date: String = "2026-11-11") =
        MetricsPeriod.containing(type, LocalDate.parse(date))

    @Test
    fun day_todayAndYesterdayHaveFriendlyTitles() {
        assertEquals("Hoy", periodTitle(period(DAY), today))
        assertEquals("miércoles, 11 de noviembre de 2026", periodSubtitle(period(DAY), today))
        assertEquals("Ayer", periodTitle(period(DAY, "2026-11-10"), today))
        assertEquals("martes, 10 de noviembre de 2026", periodSubtitle(period(DAY, "2026-11-10"), today))
    }

    @Test
    fun day_otherDaysUseTheWeekdayAndDateWithTheFullDateBelow() {
        assertEquals("domingo, 1 de noviembre", periodTitle(period(DAY, "2026-11-01"), today))
        assertEquals("1 de noviembre de 2026", periodSubtitle(period(DAY, "2026-11-01"), today))
    }

    @Test
    fun week_currentAndPreviousHaveFriendlyTitles() {
        assertEquals("Esta semana", periodTitle(period(WEEK), today))
        assertEquals("9 – 15 nov · en curso", periodSubtitle(period(WEEK), today))
        assertEquals("Semana pasada", periodTitle(period(WEEK, "2026-11-04"), today))
        assertEquals("2 – 8 nov", periodSubtitle(period(WEEK, "2026-11-04"), today))
    }

    @Test
    fun week_olderWeeksShowTheirRangeAndYear() {
        assertEquals("26 oct – 1 nov", periodTitle(period(WEEK, "2026-10-28"), today))
        assertEquals("2026", periodSubtitle(period(WEEK, "2026-10-28"), today))
    }

    @Test
    fun month_showsNameAndProgress() {
        assertEquals("Noviembre 2026", periodTitle(period(MONTH), today))
        assertEquals("día 11 de 30 · en curso", periodSubtitle(period(MONTH), today))
        assertEquals("Octubre 2026", periodTitle(period(MONTH, "2026-10-05"), today))
        assertEquals("31 días", periodSubtitle(period(MONTH, "2026-10-05"), today))
    }

    @Test
    fun year_showsYearAndProgress() {
        assertEquals("2026", periodTitle(period(YEAR), today))
        assertEquals("día 315 de 365 · en curso", periodSubtitle(period(YEAR), today))
        assertEquals("2024", periodTitle(period(YEAR, "2024-03-01"), today))
        assertEquals("366 días", periodSubtitle(period(YEAR, "2024-03-01"), today))
    }

    @Test
    fun labelsPerPeriodType() {
        assertEquals(listOf("Día", "Semana", "Mes", "Año"), MetricsPeriodType.entries.map { it.tabLabel() })
        assertEquals("Neto del día", MetricsPeriodType.DAY.netLabel())
        assertEquals("Neto de la semana", MetricsPeriodType.WEEK.netLabel())
        assertEquals("Neto del mes", MetricsPeriodType.MONTH.netLabel())
        assertEquals("Neto del año", MetricsPeriodType.YEAR.netLabel())
    }

    @Test
    fun netAmount_isSignedWithTheTrueMinus() {
        val cop = CurrencyCode.COP
        assertEquals("+$1,500", formatNet(1500.0, "$", cop))
        assertEquals("−$1,500", formatNet(-1500.0, "$", cop))
        assertEquals("$0", formatNet(0.0, "$", cop))
    }

    @Test
    fun percentChange_readsWithAnArrowAndTheMagnitude() {
        assertEquals("▲ 18%", PercentChange(18).arrowText())
        assertEquals("▼ 9%", PercentChange(-9).arrowText())
        assertEquals("0%", PercentChange(0).arrowText())
    }

    @Test
    fun dayLabels() {
        val wednesday = LocalDate.parse("2026-11-11")
        assertEquals("Mié 11", shortDayLabel(wednesday))
        assertEquals("Miércoles", weekdayName(wednesday))
        assertEquals("Sáb 7", shortDayLabel(LocalDate.parse("2026-11-07")))
        assertEquals("L", weekdayInitial(LocalDate.parse("2026-11-09")))
        assertEquals("M", weekdayInitial(LocalDate.parse("2026-11-10")))
        assertEquals("D", weekdayInitial(LocalDate.parse("2026-11-15")))
    }
}
