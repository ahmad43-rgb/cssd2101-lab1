package edu.cssd2101.lab01;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Locale;

class StoreContractTest {
    private BookstoreAPI store(boolean array) {
        return array ? new FixedArrayBookstore(4) : new ArrayListBookstore();
    }

    private final Book a = new Book("0132350882", "INDIGO Java", "Ada Lovelace", 101, 2026);
    private final Book b = new Book("9780134685991", "Advanced java", "Grace Hopper", 200, 2025);
    private final Book c = new Book("123456789X", "Other", "Ada Lovelace", 200, 2026);

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void emptyContract(boolean array) {
        BookstoreAPI s = store(array);
        assertEquals(0, s.size());
        assertEquals(0, s.inventoryValueCents());
        assertTrue(s.findByIsbn(a.isbn()).isEmpty());
        assertFalse(s.removeByIsbn(a.isbn()));
        assertTrue(s.mostRecent().isEmpty());
        assertTrue(s.mostExpensive().isEmpty());
        assertTrue(s.findByYear(100).isEmpty());
        assertTrue(s.findByTitle("java").isEmpty());
        assertTrue(s.findByAuthor("Ada").isEmpty());
        assertTrue(s.findByPriceRange(0, 0).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void duplicatePreservesOriginalAndSnapshotsAreDetached(boolean array) {
        BookstoreAPI s = store(array);
        s.add(a);
        List<Book> snapshot = s.allBooks();
        Book[] exported = s.snapshotArray();
        assertFalse(s.add(new Book(a.isbn(), "Changed", "Other", 0, 2026)));
        assertSame(a, s.findByIsbn(a.isbn()).orElseThrow());
        exported[0] = null;
        s.add(b);
        assertEquals(List.of(a), snapshot);
        assertEquals(List.of(a, b), s.allBooks());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void searchAnalyticsAndRemovalPreserveOrder(boolean array) {
        BookstoreAPI s = store(array);
        s.add(a);
        s.add(b);
        s.add(c);
        assertEquals(List.of(a, b), s.findByTitle(" JAVA "));
        assertEquals(List.of(a, c), s.findByAuthor("ada"));
        assertEquals(List.of(b, c), s.findByPriceRange(200, 200));
        assertEquals(List.of(a), s.findByPriceRange(0, 101));
        assertEquals(List.of(a, c), s.findByYear(2026));
        assertEquals(501, s.inventoryValueCents());
        assertSame(b, s.mostExpensive().orElseThrow());
        assertSame(a, s.mostRecent().orElseThrow());
        assertTrue(s.removeByIsbn(b.isbn()));
        assertEquals(List.of(a, c), s.allBooks());
        assertTrue(s.removeByIsbn(a.isbn()));
        assertTrue(s.removeByIsbn(c.isbn()));
        assertTrue(s.add(b));
        assertEquals(List.of(b), s.allBooks());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void invalidOperationsPreserveState(boolean array) {
        BookstoreAPI s = store(array);
        s.add(a);
        assertThrows(NullPointerException.class, () -> s.add(null));
        assertThrows(NullPointerException.class, () -> s.findByIsbn(null));
        assertThrows(IllegalArgumentException.class, () -> s.removeByIsbn("bad"));
        assertThrows(IllegalArgumentException.class, () -> s.findByTitle(" "));
        assertThrows(NullPointerException.class, () -> s.findByAuthor(null));
        assertThrows(IllegalArgumentException.class, () -> s.findByPriceRange(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> s.findByPriceRange(10, 9));
        assertEquals(List.of(a), s.allBooks());
    }

    @Test
    void fullArrayRejectsNewEntryButRecognizesDuplicateFirst() {
        BookstoreAPI s = new FixedArrayBookstore(1);
        s.add(a);
        assertFalse(s.add(a));
        assertThrows(IllegalStateException.class, () -> s.add(b));
        assertEquals(List.of(a), s.allBooks());
        assertTrue(s.removeByIsbn(a.isbn()));
        assertTrue(s.add(b));
        assertThrows(IllegalArgumentException.class, () -> new FixedArrayBookstore(-1));
        assertThrows(IllegalStateException.class, () -> new FixedArrayBookstore(0).add(a));
    }

    @Test
    void overflowIsExplicitAndDoesNotMutateInventory() {
        BookstoreAPI s = new ArrayListBookstore();
        s.add(new Book(a.isbn(), "Huge", "Ada", Long.MAX_VALUE, 2026));
        s.add(b);
        assertThrows(ArithmeticException.class, s::inventoryValueCents);
        assertEquals(2, s.size());
    }

    @Test
    void caseSearchIsIndependentOfDefaultLocale() {
        Locale old = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            BookstoreAPI s = new ArrayListBookstore();
            s.add(a);
            assertEquals(List.of(a), s.findByTitle("indigo"));
        } finally {
            Locale.setDefault(old);
        }
    }

    @Test
    void deterministicDemoRuns() {
        Main.main(new String[0]);
    }
}
