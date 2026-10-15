package edu.cssd2101.lab01;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class BookIdentityTest {

    @Test
    void normalizesIsbnAndText() {
        Book b = new Book("  978-0-134-68599-1  ", "  Effective Java  ", "  Joshua Bloch  ", 4599, 2018);

        assertEquals("9780134685991", b.isbn());
        assertEquals("Effective Java", b.title());
        assertEquals("Joshua Bloch", b.author());
    }

    @Test
    void allowsFreeBooks() {
        Book b = new Book("9780134685991", "Free Sample", "Some Author", 0, 2018);
        assertEquals(0, b.priceCents());
    }

    @Test
    void acceptsYearBoundaries() {
        Book early = new Book("9780134685991", "Old", "Author", 100, 1450);
        Book late = new Book("123456789X", "New", "Author", 100, 2027);

        assertEquals(1450, early.year());
        assertEquals(2027, late.year());
    }

    @Test
    void rejectsYearJustOutsideBoundaries() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Book("9780134685991", "Too Old", "Author", 100, 1449));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Book("123456789X", "Too New", "Author", 100, 2028));
    }

    @Test
    void rejectsNullAndBlankText() {
        assertThrows(
                NullPointerException.class,
                () -> new Book("9780134685991", null, "Author", 100, 2018));
        assertThrows(
                NullPointerException.class,
                () -> new Book("9780134685991", "Title", null, 100, 2018));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Book("9780134685991", "   ", "Author", 100, 2018));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Book("9780134685991", "Title", "   ", 100, 2018));
    }

    @Test
    void rejectsMalformedIsbn() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Book("12345", "Title", "Author", 100, 2018)); // too short
        assertThrows(
                IllegalArgumentException.class,
                () -> new Book("12345678901234", "Title", "Author", 100, 2018)); // too long
        assertThrows(
                IllegalArgumentException.class,
                () -> new Book("123456789x", "Title", "Author", 100, 2018)); // lowercase x
        assertThrows(
                IllegalArgumentException.class,
                () -> new Book("12345678!X", "Title", "Author", 100, 2018)); // bad punctuation
    }

    @Test
    void sameIsbnMeansSameIdentityRegardlessOfMetadata() {
        Book original = new Book("9780134685991", "Effective Java", "Joshua Bloch", 4599, 2018);
        Book sameIsbnDifferentEverythingElse =
                new Book("978-0-134-68599-1", "Different Title", "Different Author", 1, 1450);

        // equals is symmetric
        assertEquals(original, sameIsbnDifferentEverythingElse);
        assertEquals(sameIsbnDifferentEverythingElse, original);

        // hash codes agree, as the equals/hashCode contract requires
        assertEquals(original.hashCode(), sameIsbnDifferentEverythingElse.hashCode());

        // natural comparison (by ISBN) returns exactly zero for the same identity
        assertEquals(0, original.compareTo(sameIsbnDifferentEverythingElse));
    }

    @Test
    void differentIsbnsWithSameTitleBothSurviveInATreeSet() {
        Book first = new Book("9780134685991", "Same Title", "Author A", 100, 2018);
        Book second = new Book("123456789X", "Same Title", "Author B", 200, 2019);

        java.util.TreeSet<Book> set = new java.util.TreeSet<>();
        set.add(first);
        set.add(second);

        // natural order is ISBN, not title, so two distinct ISBNs are two distinct entries
        assertEquals(2, set.size());
        assertTrue(set.contains(first));
        assertTrue(set.contains(second));
    }
}
