package edu.cssd2101.lab01;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Fixed-capacity array store: live entries occupy exactly indices zero through size-1. */
public final class FixedArrayBookstore implements BookstoreAPI {
    private final Book[] books;
    private int size;

    /**
     * Creates an empty bounded catalogue.
     *
     * @param capacity maximum entries, including zero
     * @throws IllegalArgumentException if capacity is negative
     */
    public FixedArrayBookstore(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("capacity must be nonnegative");
        books = new Book[capacity];
    }

    /** {@inheritDoc} */
    @Override
    public boolean add(Book book) {
        Objects.requireNonNull(book, "book");
        for (int i = 0; i < size; i++) if (books[i].equals(book)) return false;
        if (size == books.length) throw new IllegalStateException("catalogue is full");
        books[size++] = book;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeByIsbn(String isbn) {
        String key = Book.normalizeIsbn(isbn);
        for (int i = 0; i < size; i++) {
            if (books[i].isbn().equals(key)) {
                int survivors = size - i - 1;
                System.arraycopy(books, i + 1, books, i, survivors);
                books[size - 1] = null;
                size--;
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public List<Book> allBooks() {
        return List.copyOf(Arrays.asList(Arrays.copyOf(books, size)));
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return size;
    }
}
