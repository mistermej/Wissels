package nl.wissels.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-tests voor de responsive afmetingen over meerdere schermresoluties.
 */
class ResponsiveSizesTest {

    private data class Screen(val w: Int, val h: Int)

    private val resolutions = listOf(
        Screen(320, 568),   // kleine phone
        Screen(360, 640),   // standaard
        Screen(393, 851),   // Pixel 7
        Screen(411, 914),   // Pixel 7 Pro
        Screen(600, 960),   // kleine tablet
        Screen(800, 1280),  // tablet
    )

    @Test
    fun buttonHeight_withinMaterialTouchTargetRange() {
        for (s in resolutions) {
            val h = ResponsiveSizes.buttonHeight(s.h)
            assertTrue("buttonHeight too small for ${s.w}x${s.h}: $h", h.value >= 48f)
            assertTrue("buttonHeight too big for ${s.w}x${s.h}: $h", h.value <= 72f)
        }
    }

    @Test
    fun buttonHeight_scalesWithScreenHeight() {
        // Op een groter scherm mag de knop niet kleiner zijn dan op een
        // kleiner scherm (monotoon groeiend, tot het plafond).
        val small = ResponsiveSizes.buttonHeight(568).value
        val large = ResponsiveSizes.buttonHeight(914).value
        assertTrue("buttonHeight should not shrink on larger screens: $small vs $large", large >= small)
    }

    @Test
    fun actionButtonHeight_withinRange() {
        for (s in resolutions) {
            val h = ResponsiveSizes.actionButtonHeight(s.h)
            assertTrue("actionButtonHeight too small for ${s.w}x${s.h}: $h", h.value >= 40f)
            assertTrue("actionButtonHeight too big for ${s.w}x${s.h}: $h", h.value <= 56f)
        }
    }

    @Test
    fun actionButtonHeight_smallerThanButtonHeight() {
        for (s in resolutions) {
            val action = ResponsiveSizes.actionButtonHeight(s.h).value
            val regular = ResponsiveSizes.buttonHeight(s.h).value
            assertTrue("actionButtonHeight should be <= buttonHeight: $action vs $regular", action <= regular)
        }
    }

    @Test
    fun primaryButtonHeight_withinRange() {
        for (s in resolutions) {
            val h = ResponsiveSizes.primaryButtonHeight(s.h)
            assertTrue("primaryButtonHeight too small for ${s.w}x${s.h}: $h", h.value >= 56f)
            assertTrue("primaryButtonHeight too big for ${s.w}x${s.h}: $h", h.value <= 80f)
        }
    }

    @Test
    fun logoSize_withinRange() {
        for (s in resolutions) {
            val w = ResponsiveSizes.logoSize(s.w)
            assertTrue("logoSize too small for ${s.w}x${s.h}: $w", w.value >= 120f)
            assertTrue("logoSize too big for ${s.w}x${s.h}: $w", w.value <= 220f)
        }
    }

    @Test
    fun drawerLogoHeight_withinRange() {
        for (s in resolutions) {
            val h = ResponsiveSizes.drawerLogoHeight(s.h)
            assertTrue("drawerLogoHeight too small for ${s.w}x${s.h}: $h", h.value >= 120f)
            assertTrue("drawerLogoHeight too big for ${s.w}x${s.h}: $h", h.value <= 220f)
        }
    }

    @Test
    fun sectionPadding_withinRange() {
        for (s in resolutions) {
            val p = ResponsiveSizes.sectionPadding(s.h)
            assertTrue("sectionPadding too small for ${s.w}x${s.h}: $p", p.value >= 8f)
            assertTrue("sectionPadding too big for ${s.w}x${s.h}: $p", p.value <= 32f)
        }
    }

    @Test
    fun scoreColumnWidth_withinRange() {
        for (s in resolutions) {
            val w = ResponsiveSizes.scoreColumnWidth(s.w)
            assertTrue("scoreColumnWidth too small for ${s.w}x${s.h}: $w", w.value >= 40f)
            assertTrue("scoreColumnWidth too big for ${s.w}x${s.h}: $w", w.value <= 96f)
        }
    }

    @Test
    fun creditsBottomPadding_withinRange() {
        for (s in resolutions) {
            val p = ResponsiveSizes.creditsBottomPadding(s.h)
            assertTrue("creditsBottomPadding too small for ${s.w}x${s.h}: $p", p.value >= 16f)
            assertTrue("creditsBottomPadding too big for ${s.w}x${s.h}: $p", p.value <= 48f)
        }
    }

    @Test
    fun gridColumns_threeOnWideScreens_twoOnSmall() {
        assertEquals(2, ResponsiveSizes.gridColumns(320))
        assertEquals(3, ResponsiveSizes.gridColumns(360))
        assertEquals(3, ResponsiveSizes.gridColumns(393))
        assertEquals(3, ResponsiveSizes.gridColumns(411))
        assertEquals(3, ResponsiveSizes.gridColumns(800))
    }

    @Test
    fun noValueBelowMaterialMinimumOnSmallestPhone() {
        val s = Screen(320, 568)
        assertTrue(ResponsiveSizes.buttonHeight(s.h).value >= 48f)
        assertTrue(ResponsiveSizes.actionButtonHeight(s.h).value >= 40f)
        assertTrue(ResponsiveSizes.primaryButtonHeight(s.h).value >= 56f)
        assertTrue(ResponsiveSizes.logoSize(s.w).value >= 120f)
        assertTrue(ResponsiveSizes.drawerLogoHeight(s.h).value >= 120f)
        assertEquals(2, ResponsiveSizes.gridColumns(s.w))
    }

    @Test
    fun noValueAboveCeilingOnTablet() {
        val s = Screen(800, 1280)
        assertTrue(ResponsiveSizes.buttonHeight(s.h).value <= 72f)
        assertTrue(ResponsiveSizes.actionButtonHeight(s.h).value <= 56f)
        assertTrue(ResponsiveSizes.primaryButtonHeight(s.h).value <= 80f)
        assertTrue(ResponsiveSizes.logoSize(s.w).value <= 220f)
        assertTrue(ResponsiveSizes.drawerLogoHeight(s.h).value <= 220f)
        assertEquals(3, ResponsiveSizes.gridColumns(s.w))
    }
}
