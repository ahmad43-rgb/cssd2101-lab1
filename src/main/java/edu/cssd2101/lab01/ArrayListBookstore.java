package edu.cssd2101.lab01;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** ArrayList implementation with linear uniqueness checks and immutable snapshots. */
public final class ArrayListBookstore implements BookstoreAPI {
    private final List<Book> books = new ArrayList<>();

    /** Creates an empty catalogue. */
    public ArrayListBookstore() {}

    /** {@inheritDoc} */
    @Override
    public boolean add(Book book) {
        Objects.requireNonNull(book, "book");
        if (books.contains(book)) return false;
        books.add(book);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeByIsbn(String isbn) {
        String key = Book.normalizeIsbn(isbn);
        return books.removeIf(b -> b.isbn().equals(key));
    }

    /** {@inheritDoc} */
    @Override
    public List<Book> allBooks() {
        return List.copyOf(books);
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return books.size();
    }
}
