package nl.wissels.app.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive afmetingen voor de hele app.
 *
 * Elke afmeting schaalt met de schermresolutie in plaats van vast te zijn in dp.
 * De formules zitten in pure functies (geen Compose-state) zodat ze voor
 * meerdere resolities met unit-tests geverifieerd kunnen worden.
 *
 * Iedere waarde heeft een veilige onder- en bovengrens, zodat de UI op hele
 * kleine phones (320x568dp) en grote tablets (800x1280dp) bruikbaar blijft.
 */
object ResponsiveSizes {

    /** Knop-hoogte op het wedstrijdscherm: 7,5% van de schermhoogte. */
    fun buttonHeight(screenHeightDp: Int): Dp =
        (screenHeightDp.dp * 0.075f).coerceIn(48.dp, 72.dp)

    /** Grote start-knop (voorbereiding/start): 12% van de schermhoogte. */
    fun primaryButtonHeight(screenHeightDp: Int): Dp =
        (screenHeightDp.dp * 0.12f).coerceIn(56.dp, 80.dp)

    /** Logo op het splash-scherm: 45% van de schermbreedte. */
    fun logoSize(screenWidthDp: Int): Dp =
        (screenWidthDp.dp * 0.45f).coerceIn(120.dp, 220.dp)

    /** Logo in het zijmenu: 18% van de schermhoogte. */
    fun drawerLogoHeight(screenHeightDp: Int): Dp =
        (screenHeightDp.dp * 0.18f).coerceIn(80.dp, 160.dp)

    /** Kaart-/sectie-padding: 5% van de schermhoogte. */
    fun sectionPadding(screenHeightDp: Int): Dp =
        (screenHeightDp.dp * 0.05f).coerceIn(8.dp, 32.dp)

    /** Breedte van een score-kolom op het scorebord: 12% van de schermbreedte. */
    fun scoreColumnWidth(screenWidthDp: Int): Dp =
        (screenWidthDp.dp * 0.12f).coerceIn(40.dp, 96.dp)

    /** Onderrand-ruimte voor de credits op het splash-scherm. */
    fun creditsBottomPadding(screenHeightDp: Int): Dp =
        (screenHeightDp.dp * 0.06f).coerceIn(16.dp, 48.dp)

    /**
     * Aantal kolommen in het spelersgrid: 3 op ruime schermen (>=360dp),
     * anders 2 zodat de tegels leesbaar blijven.
     */
    fun gridColumns(screenWidthDp: Int): Int = if (screenWidthDp >= 360) 3 else 2
}
