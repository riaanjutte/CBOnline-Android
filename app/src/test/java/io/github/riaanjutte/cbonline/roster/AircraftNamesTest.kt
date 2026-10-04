package io.github.riaanjutte.cbonline.roster

import org.junit.Assert.assertEquals
import org.junit.Test

class AircraftNamesTest {

    private fun check(raw: String, expected: String) = assertEquals(expected, tidyAircraftName(raw))

    @Test
    fun `German types and variants`() {
        check("bf 109 g-14", "Bf 109 G-14")
        check("fw 190 a-8", "Fw 190 A-8")
        check("he 111 h-6", "He 111 H-6")
        check("me 262 a", "Me 262 A")
    }

    @Test
    fun `American designations`() {
        check("p-51d-15", "P-51D-15")
        check("a-20b", "A-20B")
    }

    @Test
    fun `British marks keep roman numerals upper and variant letters lower`() {
        check("spitfire mk.ixe", "Spitfire Mk.IXe")
        check("spitfire mk.vb", "Spitfire Mk.Vb")
        check("tempest mk.v ser.2", "Tempest Mk.V Ser.2")
        check("typhoon mk.ib", "Typhoon Mk.Ib")
    }

    @Test
    fun `Soviet design bureaus keep their usual capitals`() {
        check("yak-9", "Yak-9")
        check("mig-3", "MiG-3")
        check("lagg-3", "LaGG-3")
        check("la-5fn", "La-5FN")
        check("il-2", "Il-2")
    }

    @Test
    fun `tokens starting with a digit are left alone`() = check("ju 52 3mg4e", "Ju 52 3mg4e")

    @Test
    fun `extra spaces collapse and blank stays blank`() {
        check("  bf   109  ", "Bf 109")
        check("", "")
    }
}
