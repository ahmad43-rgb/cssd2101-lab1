package edu.cssd2101.lab01;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * T2: runs identical add/lookup/duplicate/snapshot scenarios against both store implementations
 * through one shared helper, proving the two implementations are interchangeable behind
 * BookstoreAPI.
 */
class BaselineApiTest {

    /** Shared test helper: builds a fresh store of the requested kind. */
    private BookstoreAPI newStore(boolean array) {
        return array ? new FixedArrayBookstore(4) : new ArrayListBookstore();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void addsLooksUpAndPreservesOriginalOnDuplicate(boolean array) {
        BookstoreAPI store = newStore(array);
        Book original = new Book("9780134685991", "Effective Java", "Joshua Bloch", 4599, 2018);

        assertTrue(store.add(original));
        assertEquals(1, store.size());

        // Same ISBN, everything else different: add must be rejected and the original kept.
        Book duplicateIsbnDifferentData =
                new Book("978-0-134-68599-1", "Changed Title", "Changed Author", 1, 1450);
        assertFalse(store.add(duplicateIsbnDifferentData));
        assertEquals(1, store.size());
        assertSame(original, store.findByIsbn("9780134685991").orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void listSnapshotIsDetachedFromLaterInsertions(boolean array) {
        BookstoreAPI store = newStore(array);
        Book first = new Book("9780134685991", "Effective Java", "Joshua Bloch", 4599, 2018);
        store.add(first);

        List<Book> snapshot = store.allBooks();
        store.add(new Book("123456789X", "Clean Code", "Robert Martin", 3299, 2008));

        // The earlier snapshot must not see the later insertion.
        assertEquals(List.of(first), snapshot);
        assertEquals(2, store.size());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void exportedArrayMutationDoesNotAffectStore(boolean array) {
        BookstoreAPI store = newStore(array);
        Book first = new Book("9780134685991", "Effective Java", "Joshua Bloch", 4599, 2018);
        store.add(first);

        Book[] exported = store.snapshotArray();
        exported[0] = null; // mutate the caller's copy only

        assertEquals(1, store.size());
        assertSame(first, store.findByIsbn("9780134685991").orElseThrow());
    }
}
