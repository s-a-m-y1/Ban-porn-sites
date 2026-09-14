package com.contentfilter.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.After
import org.junit.Test
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Pure-JVM tests for BlocklistIndex.isBlocked — the O(1) domain match on the
 * DNS hot path (exact match + parent-domain walk over the in-memory sets).
 *
 * The index's backing sets are private to the object, so each test seeds them
 * directly via reflection. No Android framework code runs: isBlocked only
 * touches String/Set, and load(context) — the one Android-dependent path —
 * is never called here.
 */
class BlocklistIndexTest {

    @Before
    fun setUp() {
        seed(emptySet(), emptySet())
    }

    @After
    fun tearDown() {
        seed(emptySet(), emptySet())
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    /** Replace the category and custom-domain sets of the singleton index. */
    private fun seed(categories: Set<String>, custom: Set<String>) {
        setField("blockedByCategory", categories)
        setField("customBlocked", custom)
    }

    private fun setField(name: String, value: Any?) {
        val field = BlocklistIndex::class.java.getDeclaredField(name)
        field.isAccessible = true
        setOn(field, singleton(), value)
    }

    /** Kotlin object properties may compile to static or instance fields. */
    private fun setOn(field: Field, target: Any?, value: Any?) {
        if (Modifier.isStatic(field.modifiers)) field.set(null, value)
        else field.set(target, value)
    }

    private fun singleton(): Any? =
        BlocklistIndex::class.java.getDeclaredField("INSTANCE")
            .apply { isAccessible = true }
            .get(null)

    // ---------------------------------------------------------------------
    // empty index
    // ---------------------------------------------------------------------

    @Test
    fun nothingIsBlockedWhenIndexIsEmpty() {
        assertFalse(BlocklistIndex.isBlocked("example.com"))
        assertFalse(BlocklistIndex.isBlocked("sub.example.com"))
        assertFalse(BlocklistIndex.isBlocked("localhost"))
    }

    @Test
    fun indexIsNotReadyBeforeLoad() {
        // load(context) is the only thing that flips this flag; these tests
        // never call it, so the VPN-service-start precondition is observable.
        assertFalse(BlocklistIndex.isReady())
    }

    // ---------------------------------------------------------------------
    // exact matching
    // ---------------------------------------------------------------------

    @Test
    fun exactMatchInCategorySetIsBlocked() {
        seed(categories = setOf("pornsite.com"), custom = emptySet())
        assertTrue(BlocklistIndex.isBlocked("pornsite.com"))
    }

    @Test
    fun exactMatchInCustomSetIsBlocked() {
        seed(categories = emptySet(), custom = setOf("myblock.net"))
        assertTrue(BlocklistIndex.isBlocked("myblock.net"))
    }

    @Test
    fun siblingDomainIsNotBlocked() {
        seed(categories = setOf("example.com"), custom = emptySet())
        assertFalse(BlocklistIndex.isBlocked("example.org"))
        assertFalse(BlocklistIndex.isBlocked("notexample.com"))
        assertFalse(BlocklistIndex.isBlocked("example.com.evil.net"))
    }

    // ---------------------------------------------------------------------
    // parent-domain walk
    // ---------------------------------------------------------------------

    @Test
    fun subdomainOfBlockedDomainIsBlocked() {
        seed(categories = setOf("example.com"), custom = emptySet())
        assertTrue(BlocklistIndex.isBlocked("www.example.com"))
        assertTrue(BlocklistIndex.isBlocked("a.b.cdn.example.com"))
    }

    @Test
    fun parentWalkStopsAtFirstBlockedAncestor() {
        // only the middle label is listed — deeper subdomains must still hit it
        seed(categories = setOf("b.example.com"), custom = emptySet())
        assertTrue(BlocklistIndex.isBlocked("a.b.example.com"))
        // but a sibling of the listed label must not
        assertFalse(BlocklistIndex.isBlocked("c.example.com"))
    }

    @Test
    fun parentWalkReachesTopLevelLabels() {
        // an aggressive but documented consequence of the walk: listing a
        // public suffix blocks everything underneath it
        seed(categories = setOf("co.uk"), custom = emptySet())
        assertTrue(BlocklistIndex.isBlocked("site.co.uk"))
        assertTrue(BlocklistIndex.isBlocked("deep.site.co.uk"))
    }

    // ---------------------------------------------------------------------
    // normalization
    // ---------------------------------------------------------------------

    @Test
    fun matchingIsCaseInsensitive() {
        seed(categories = setOf("example.com"), custom = emptySet())
        assertTrue(BlocklistIndex.isBlocked("EXAMPLE.COM"))
        assertTrue(BlocklistIndex.isBlocked("WwW.ExAmPlE.CoM"))
    }

    @Test
    fun trailingRootDotIsIgnored() {
        seed(categories = setOf("example.com"), custom = emptySet())
        assertTrue(BlocklistIndex.isBlocked("example.com."))
        assertTrue(BlocklistIndex.isBlocked("www.example.com."))
    }

    @Test
    fun storedEntriesAreExpectedToBeLowercase() {
        // load() lowercases before storing; a caller passing mixed-case input
        // against a mixed-case-stored set would be a caller-side bug
        seed(categories = setOf("example.com"), custom = setOf("custom.net"))
        assertTrue(BlocklistIndex.isBlocked("www.example.com"))
        assertTrue(BlocklistIndex.isBlocked("www.custom.net"))
    }

    @Test
    fun singleLabelDomainWalkTerminates() {
        seed(categories = setOf("localhost"), custom = setOf("intranet"))
        assertTrue(BlocklistIndex.isBlocked("localhost"))
        assertTrue(BlocklistIndex.isBlocked("intranet"))
        assertFalse(BlocklistIndex.isBlocked("somehost"))
    }

    @Test
    fun categoryAndCustomSetsAreIndependent() {
        seed(categories = setOf("cat.com"), custom = setOf("cust.com"))
        assertTrue(BlocklistIndex.isBlocked("cat.com"))
        assertTrue(BlocklistIndex.isBlocked("cust.com"))
        assertFalse(BlocklistIndex.isBlocked("other.com"))
    }

    @Test
    fun deepSubdomainOfCustomEntryIsBlocked() {
        seed(categories = emptySet(), custom = setOf("distraction.io"))
        assertTrue(BlocklistIndex.isBlocked("forum.distraction.io"))
        assertFalse(BlocklistIndex.isBlocked("distraction.io.evil.com"))
    }
}
