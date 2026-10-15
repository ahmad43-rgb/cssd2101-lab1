package edu.cssd2101.lab01;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.TreeSet;

class BookTest {
    static Book book(String isbn, String title, long price, int year) {
        return new Book(isbn, title, "Ada", price, year);
    }

    @Test
    void normalizesAndExposesImmutableData() {
        Book b = new Book(" 978-0134685991 ", " Java ", " Ada ", 0, 1450);
        assertAll(
                () -> assertEquals("9780134685991", b.isbn()),
                () -> assertEquals("Java", b.title()),
                () -> assertEquals("Ada", b.author()),
                () -> assertEquals(0, b.priceCents()),
                () -> assertEquals(1450, b.year()),
                () -> assertEquals("Java [9780134685991] 0 cents", b.toString()));
        assertEquals(2027, book("0132350882", "X", Long.MAX_VALUE, 2027).year());
        assertEquals("123456789X", book("123456789X", "X", 0, 2026).isbn());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                " ",
                "123",
                "123456789x",
                "12345678901234",
                "abcdefghij",
                "978/0134685991"
            })
    void rejectsInvalidIsbn(String isbn) {
        assertThrows(IllegalArgumentException.class, () -> book(isbn, "X", 0, 2026));
    }

    @Test
    void rejectsNullsBlanksAndNumericBoundaries() {
        assertThrows(NullPointerException.class, () -> book(null, "X", 0, 2026));
        assertThrows(NullPointerException.class, () -> book("0132350882", null, 0, 2026));
        assertThrows(NullPointerException.class, () -> new Book("0132350882", "X", null, 0, 2026));
        assertThrows(IllegalArgumentException.class, () -> book("0132350882", " ", 0, 2026));
        assertThrows(
                IllegalArgumentException.class, () -> new Book("0132350882", "X", " ", 0, 2026));
        assertThrows(IllegalArgumentException.class, () -> book("0132350882", "X", -1, 2026));
        for (int y : new int[] {1449, 2028, Integer.MIN_VALUE, Integer.MAX_VALUE})
            assertThrows(IllegalArgumentException.class, () -> book("0132350882", "X", 0, y));
    }

    @Test
    void identityOrderAndDisplayOrderHaveDifferentPurposes() {
        Book a = book("0132350882", "Zulu", 1, 2026),
                changed = book("0132350882", "Alpha", 9, 2027);
        Book distinct = book("9780134685991", "Zulu", 1, 2026);
        assertEquals(a, changed);
        assertEquals(changed, a);
        assertEquals(a.hashCode(), changed.hashCode());
        assertEquals(0, a.compareTo(changed));
        assertNotEquals(a, distinct);
        assertNotEquals(a, null);
        assertNotEquals(a, "other");
        assertEquals(2, new TreeSet<>(List.of(a, changed, distinct)).size());
        assertTrue(Book.DISPLAY_ORDER.compare(changed, a) < 0);
        assertTrue(Book.DISPLAY_ORDER.compare(a, distinct) < 0);
        assertThrows(NullPointerException.class, () -> a.compareTo(null));
    }
}
