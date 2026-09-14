package com.contentfilter.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM tests for PinManager.validate — the PIN-strength rules checked when
 * the user creates or changes their app password.
 *
 * Only [PinManager.validate] is tested: the other PinManager functions take an
 * Android Context (SharedPreferences) and there is no Robolectric or mocking
 * library in the project's test dependencies, so hash/save/verify are out of
 * reach of a pure-JVM unit test by design.
 *
 * R.string fields are plain int constants compiled into the app's R class, so
 * referencing them from a JVM unit test is safe (no android.jar method calls).
 */
class PinManagerTest {

    private val errLength = R.string.pin_err_length
    private val errDigits = R.string.pin_err_digits
    private val errSame = R.string.pin_err_same
    private val errSeq = R.string.pin_err_seq
    private val errCommon = R.string.pin_err_common

    private val allErrorIds = setOf(errLength, errDigits, errSame, errSeq, errCommon)

    private fun assertInvalid(pin: String): Int {
        val result = PinManager.validate(pin)
        assert(result in allErrorIds) { "expected an error id for '$pin' but got $result" }
        return result
    }

    // ---------------------------------------------------------------------
    // valid PINs
    // ---------------------------------------------------------------------

    @Test
    fun fourDigitValidPinPasses() {
        assertEquals(0, PinManager.validate("9374"))
    }

    @Test
    fun fiveDigitValidPinPasses() {
        assertEquals(0, PinManager.validate("80621"))
    }

    @Test
    fun sixDigitValidPinPasses() {
        assertEquals(0, PinManager.validate("102938"))
    }

    @Test
    fun pinWithRepeatedDigitsButNotAllSameIsAccepted() {
        assertEquals(0, PinManager.validate("5577"))
        assertEquals(0, PinManager.validate("9090"))
    }

    // ---------------------------------------------------------------------
    // length rule
    // ---------------------------------------------------------------------

    @Test
    fun tooShortPinIsRejected() {
        assertEquals(errLength, assertInvalid("123"))
    }

    @Test
    fun emptyPinIsRejectedAsLengthError() {
        assertEquals(errLength, assertInvalid(""))
    }

    @Test
    fun tooLongPinIsRejected() {
        assertEquals(errLength, assertInvalid("1234567"))
    }

    // ---------------------------------------------------------------------
    // digits-only rule
    // ---------------------------------------------------------------------

    @Test
    fun pinWithLettersIsRejected() {
        assertEquals(errDigits, assertInvalid("12a4"))
    }

    @Test
    fun pinWithMixedCaseLettersIsRejected() {
        assertEquals(errDigits, assertInvalid("A1b2"))
    }

    @Test
    fun pinWithSpacesIsRejected() {
        assertEquals(errDigits, assertInvalid("12 4"))
    }

    @Test
    fun pinWithSpecialCharactersIsRejected() {
        assertEquals(errDigits, assertInvalid("12#4"))
    }

    @Test
    fun pinWithNewlineIsRejected() {
        assertEquals(errDigits, assertInvalid("12\n4"))
    }

    // ---------------------------------------------------------------------
    // all-same-digit rule
    // ---------------------------------------------------------------------

    @Test
    fun allSameDigitFourIsRejected() {
        assertEquals(errSame, assertInvalid("1111"))
    }

    @Test
    fun allSameDigitSixIsRejected() {
        assertEquals(errSame, assertInvalid("000000"))
    }

    // ---------------------------------------------------------------------
    // sequence rule
    // ---------------------------------------------------------------------

    @Test
    fun ascendingSequenceIsRejected() {
        assertEquals(errSeq, assertInvalid("1234"))
    }

    @Test
    fun ascendingSequenceOfSixIsRejected() {
        assertEquals(errSeq, assertInvalid("456789"))
    }

    @Test
    fun descendingSequenceIsRejected() {
        assertEquals(errSeq, assertInvalid("4321"))
    }

    @Test
    fun descendingSequenceOfFiveIsRejected() {
        assertEquals(errSeq, assertInvalid("98765"))
    }

    @Test
    fun pinWithSomeConsecutiveDigitsButNotFullSequenceIsAccepted() {
        // "8523" contains pairs but no full run of b == a+1 (or a-1)
        assertEquals(0, PinManager.validate("8523"))
    }

    // ---------------------------------------------------------------------
    // common-PIN list
    // ---------------------------------------------------------------------

    @Test
    fun commonFourDigitPinsAreRejected() {
        // from the implementation's explicit common list
        assertEquals(errCommon, assertInvalid("1212"))
        assertEquals(errCommon, assertInvalid("6969"))
        assertEquals(errCommon, assertInvalid("1004"))
        assertEquals(errCommon, assertInvalid("2000"))
        assertEquals(errCommon, assertInvalid("2020"))
    }

    @Test
    fun notAllCommonListPinsAreSequencesOrSame() {
        // 6969: not a sequence, not all-same — only the common list catches it
        assertEquals(errCommon, assertInvalid("6969"))
    }

    @Test
    fun commonPins0000And1111AreCaughtByEarlierRules() {
        // "0000"/"1111" are all-same (checked before the common list) and
        // also on the common list; the implementation reports errSame.
        assertEquals(errSame, assertInvalid("0000"))
        assertEquals(errSame, assertInvalid("1111"))
    }

    @Test
    fun commonPin1122IsRejectedAsCommon() {
        // not all-same, not a sequence — only the common list matches
        assertEquals(errCommon, assertInvalid("1122"))
    }

    // ---------------------------------------------------------------------
    // error-id sanity (guards the test harness itself)
    // ---------------------------------------------------------------------

    @Test
    fun fiveErrorIdsAreDistinct() {
        assertEquals(5, allErrorIds.size)
    }
}
