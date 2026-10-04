package io.github.riaanjutte.cbonline.roster

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SquadTagsTest {

    @Test
    fun `suggests enclosed tags at the start`() {
        assertEquals("[CB]", suggestSquadTag("[CB]Ace"))
        assertEquals("=JG52=", suggestSquadTag("=JG52=Hans"))
        assertEquals("|TAG|", suggestSquadTag("|TAG| Bob"))
        assertEquals("(56RAF)", suggestSquadTag("(56RAF)Smith"))
        assertEquals("-VVS-", suggestSquadTag("-VVS-Ivan"))
    }

    @Test
    fun `suggests enclosed tags at the end`() {
        assertEquals("[1/KG]", suggestSquadTag("Fritz [1/KG]"))
        assertEquals("=GEMINI=", suggestSquadTag("Castor=GEMINI="))
    }

    @Test
    fun `suggests an underscore prefix including the underscore`() {
        assertEquals("JG52_", suggestSquadTag("JG52_Hans"))
        assertEquals("No.4_", suggestSquadTag("No.4_Smith"))
    }

    @Test
    fun `no suggestion for plain names`() {
        assertNull(suggestSquadTag("Pilot01"))
        assertNull(suggestSquadTag("J.Smith"))
        assertNull(suggestSquadTag(""))
    }

    @Test
    fun `a name that is only a tag gets no suggestion`() {
        assertNull(suggestSquadTag("[CB]"))
        assertNull(suggestSquadTag("JG52_"))
    }

    @Test
    fun `members match by contained tag ignoring case and spaces around the tag`() {
        assertTrue(isSquadMember("=JG52=Hans", "=jg52="))
        assertTrue(isSquadMember("Fritz [1/KG]", " [1/KG] "))
        assertFalse(isSquadMember("Hans", "=JG52="))
    }

    @Test
    fun `tags shorter than two characters match nobody`() {
        assertFalse(isSquadMember("Anyone", "A"))
        assertFalse(isSquadMember("Anyone", "  "))
        assertFalse(isValidSquadTag(" x "))
        assertTrue(isValidSquadTag("CB"))
    }
}
