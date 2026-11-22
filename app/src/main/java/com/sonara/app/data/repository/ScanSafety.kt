package com.sonara.app.data.repository

/** Whether a finished scan may be applied to the stored library. */
sealed interface ScanVerdict {
    data object Accept : ScanVerdict
    data class Reject(val reason: String) : ScanVerdict
}

/**
 * The rule that keeps a bad scan from emptying the library. Android answers a query without audio permission,
 * or during a MediaStore hiccup, with "no rows" rather than an error, and that must never read as "the person
 * deleted every song". This is checked on what MediaStore returned, before folder exclusions are applied, so
 * excluding every folder on purpose still works.
 */
object ScanSafety {

    fun verify(permissionGranted: Boolean, storedCount: Int, rawScannedCount: Int): ScanVerdict = when {
        !permissionGranted ->
            ScanVerdict.Reject("Audio access is not granted, so the library was left as it is.")
        rawScannedCount == 0 && storedCount > 0 ->
            ScanVerdict.Reject("The system returned no music, so the library was left as it is. Try again in a moment.")
        else -> ScanVerdict.Accept
    }
}
