package com.sonara.app.core.navigation

/**
 * Routes of the screens that take arguments. Names and paths travel as query parameters because an
 * artist or folder name may contain "/" or other characters that break path segments.
 */
object SonaraRoutes {
    const val ARG_ALBUM_ID = "albumId"
    const val ARG_PLAYLIST_ID = "playlistId"
    const val ARG_NAME = "name"
    const val ARG_PATH = "path"
    const val ARG_RECURSIVE = "recursive"

    const val ALBUM = "album/{$ARG_ALBUM_ID}"
    const val PLAYLIST = "playlist/{$ARG_PLAYLIST_ID}"
    const val ARTIST = "artist?$ARG_NAME={$ARG_NAME}"
    const val GENRE = "genre?$ARG_NAME={$ARG_NAME}"
    const val FOLDER = "folder?$ARG_PATH={$ARG_PATH}&$ARG_RECURSIVE={$ARG_RECURSIVE}"

    fun album(albumId: Long) = "album/$albumId"
    fun playlist(playlistId: Long) = "playlist/$playlistId"
    fun artist(name: String) = "artist?$ARG_NAME=${encode(name)}"
    fun genre(name: String) = "genre?$ARG_NAME=${encode(name)}"
    fun folder(path: String, recursive: Boolean) = "folder?$ARG_PATH=${encode(path)}&$ARG_RECURSIVE=$recursive"

    /** Percent-encodes every UTF-8 byte except the unreserved characters, so any text survives a round trip. */
    fun encode(value: String): String {
        val out = StringBuilder()
        for (byte in value.toByteArray(Charsets.UTF_8)) {
            val c = byte.toInt() and 0xFF
            val unreserved = (c in 'a'.code..'z'.code) || (c in 'A'.code..'Z'.code) ||
                (c in '0'.code..'9'.code) || c == '-'.code || c == '_'.code || c == '.'.code || c == '~'.code
            if (unreserved) {
                out.append(c.toChar())
            } else {
                out.append('%')
                out.append("0123456789ABCDEF"[c shr 4])
                out.append("0123456789ABCDEF"[c and 0xF])
            }
        }
        return out.toString()
    }
}
