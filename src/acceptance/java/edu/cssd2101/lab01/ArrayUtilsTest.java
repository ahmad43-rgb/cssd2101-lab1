package edu.cssd2101.lab01;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class ArrayUtilsTest {
    private final Book a = new Book("0132350882", "A", "Ada", 100, 2009);
    private final Book b = new Book("9780134685991", "B", "Ada", 101, 2010);
    private final Book c = new Book("123456789X", "C", "Ada", 101, 2019);

    @Test
    void sparseFiltersAreCompactAndDoNotMutateInput() {
        Book[] input = {null, a, null, b, c};
        Book[] before = input.clone();
        assertArrayEquals(new Book[] {a}, BookArrayUtils.filterPriceAtMost(input, 100));
        assertArrayEquals(new Book[] {a, b, c}, BookArrayUtils.filterPriceAtMost(input, 101));
        assertArrayEquals(new Book[0], BookArrayUtils.filterPriceAtMost(input, 0));
        assertArrayEquals(new Book[] {b, c}, BookArrayUtils.filterByDecade(input, 2010));
        assertEquals(1, BookArrayUtils.countBeforeYear(input, 2010));
        assertArrayEquals(before, input);
    }

    @Test
    void sortIsStableNullLastAndMutatesOnlyGivenArray() {
        Book[] input = {c, null, b, a, null};
        BookArrayUtils.sortByPrice(input);
        assertArrayEquals(new Book[] {a, c, b, null, null}, input);
        BookArrayUtils.sortByYear(input);
        assertArrayEquals(new Book[] {a, b, c, null, null}, input);
        assertSame(a, BookArrayUtils.findOldest(input).orElseThrow());
    }

    @Test
    void averageRoundsHalfEvenAndDistinguishesNoDataFromFree() {
        assertEquals(
                new BigDecimal("1.00"),
                BookArrayUtils.averagePrice(new Book[] {a, null, b}).orElseThrow());
        assertEquals(
                new BigDecimal("1.01"),
                BookArrayUtils.averagePrice(new Book[] {b, c}).orElseThrow());
        Book free = new Book(a.isbn(), "Free", "Ada", 0, 2026);
        assertEquals(
                new BigDecimal("0.00"),
                BookArrayUtils.averagePrice(new Book[] {free}).orElseThrow());
        assertTrue(BookArrayUtils.averagePrice(new Book[] {null}).isEmpty());
        assertTrue(BookArrayUtils.findOldest(new Book[0]).isEmpty());
        Book huge = new Book(a.isbn(), "Huge", "Ada", Long.MAX_VALUE, 2026);
        assertEquals(
                BigDecimal.valueOf(Long.MAX_VALUE, 2),
                BookArrayUtils.averagePrice(new Book[] {huge, huge}).orElseThrow());
    }

    @Test
    void mergePreservesSlotsWhileDedupKeepsFirstInstance() {
        Book altered = new Book(a.isbn(), "Changed", "Ada", 0, 2026);
        Book[] first = {a, null};
        Book[] second = {b, altered};
        Book[] merged = BookArrayUtils.merge(first, second);
        assertArrayEquals(new Book[] {a, null, b, altered}, merged);
        Book[] unique = BookArrayUtils.removeDuplicates(merged);
        assertArrayEquals(new Book[] {a, b}, unique);
        assertSame(a, unique[0]);
        merged[0] = null;
        assertSame(a, first[0]);
    }

    @Test
    void invalidUtilityArgumentsAreExplicit() {
        assertThrows(NullPointerException.class, () -> BookArrayUtils.filterPriceAtMost(null, 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> BookArrayUtils.filterPriceAtMost(new Book[0], -1));
        for (int d : new int[] {-10, 2011, 10000})
            assertThrows(
                    IllegalArgumentException.class,
                    () -> BookArrayUtils.filterByDecade(new Book[0], d));
        assertThrows(NullPointerException.class, () -> BookArrayUtils.merge(new Book[0], null));
        assertThrows(NullPointerException.class, () -> BookArrayUtils.removeDuplicates(null));
    }
}
