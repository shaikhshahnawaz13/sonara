package com.sonara.app.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryCategoryTest {

    @Test
    fun categoriesAreInTheOrderTheyAreShown() {
        assertEquals(
            listOf(LibraryCategory.Songs, LibraryCategory.Albums, LibraryCategory.Artists, LibraryCategory.Genres),
            LibraryCategory.entries,
        )
    }

    @Test
    fun movingForwardSlidesInFromTheRight() {
        assertEquals(1, categorySlideDirection(LibraryCategory.Songs, LibraryCategory.Albums))
        assertEquals(1, categorySlideDirection(LibraryCategory.Songs, LibraryCategory.Genres))
        assertEquals(1, categorySlideDirection(LibraryCategory.Artists, LibraryCategory.Genres))
    }

    @Test
    fun movingBackwardSlidesInFromTheLeft() {
        assertEquals(-1, categorySlideDirection(LibraryCategory.Albums, LibraryCategory.Songs))
        assertEquals(-1, categorySlideDirection(LibraryCategory.Genres, LibraryCategory.Songs))
        assertEquals(-1, categorySlideDirection(LibraryCategory.Genres, LibraryCategory.Artists))
    }
}
