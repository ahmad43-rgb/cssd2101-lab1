package edu.cssd2101.lab01;

import java.util.Comparator;
import java.util.Objects;

/**
 * Immutable catalogue entry. Identity and natural order use normalized ISBN only.
 *
 * <p>ISBN syntax is checked, not its check digit. Prices are nonnegative cents; years use this
 * edition's deterministic inclusive range 1450 to 2027.
 */
public final class Book implements Comparable<Book> {
    /** Display ordering by case-insensitive title, then ISBN; not an identity ordering. */
    public static final Comparator<Book> DISPLAY_ORDER =
            Comparator.comparing(Book::title, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Book::isbn);

    private final String isbn;
    private final String title;
    private final String author;
    private final long priceCents;
    private final int year;

    /**
     * Creates one valid entry, stripping surrounding text whitespace and ISBN hyphens.
     *
     * @param isbn ten characters (nine digits and digit/X) or thirteen digits
     * @param title nonblank title
     * @param author nonblank author
     * @param priceCents nonnegative price in cents
     * @param year publication year, 1450 through 2027
     * @throws NullPointerException if a string is null
     * @throws IllegalArgumentException if syntax, text, price or year is invalid
     */
    public Book(String isbn, String title, String author, long priceCents, int year) {
        this.isbn = normalizeIsbn(isbn);
        this.title = text(title);
        this.author = text(author);
        if (priceCents < 0) throw new IllegalArgumentException("priceCents must be nonnegative");
        if (year < 1450 || year > 2027)
            throw new IllegalArgumentException("year must be 1450..2027");
        this.priceCents = priceCents;
        this.year = year;
    }

    static String normalizeIsbn(String value) {
        String normalized = Objects.requireNonNull(value, "isbn").strip().replace("-", "");
        if (!normalized.matches("[0-9]{13}|[0-9]{9}[0-9X]"))
            throw new IllegalArgumentException(
                    "ISBN syntax must be 13 digits or 9 digits plus digit/X");
        return normalized;
    }

    static String text(String value) {
        String normalized = Objects.requireNonNull(value, "text").strip();
        if (normalized.isBlank()) throw new IllegalArgumentException("text must not be blank");
        return normalized;
    }

    /**
     * Returns canonical ISBN.
     *
     * @return ISBN without hyphens
     */
    public String isbn() {
        return isbn;
    }

    /**
     * Returns the title.
     *
     * @return stripped title
     */
    public String title() {
        return title;
    }

    /**
     * Returns the author.
     *
     * @return stripped author
     */
    public String author() {
        return author;
    }

    /**
     * Returns exact price.
     *
     * @return nonnegative cents
     */
    public long priceCents() {
        return priceCents;
    }

    /**
     * Returns the publication year.
     *
     * @return year in 1450..2027
     */
    public int year() {
        return year;
    }

    /**
     * Compares identity consistently with equality.
     *
     * @param other entry to compare
     * @return ISBN lexicographic comparison
     * @throws NullPointerException if other is null
     */
    @Override
    public int compareTo(Book other) {
        return isbn.compareTo(Objects.requireNonNull(other).isbn);
    }

    /**
     * Tests ISBN identity.
     *
     * @param other candidate
     * @return true for the same canonical ISBN
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof Book b && isbn.equals(b.isbn);
    }

    /**
     * Hashes identity.
     *
     * @return stable ISBN hash
     */
    @Override
    public int hashCode() {
        return isbn.hashCode();
    }

    /**
     * Describes this entry without locale-sensitive formatting.
     *
     * @return title, ISBN and cents
     */
    @Override
    public String toString() {
        return title + " [" + isbn + "] " + priceCents + " cents";
    }
}
