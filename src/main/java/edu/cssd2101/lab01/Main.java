package edu.cssd2101.lab01;

/** Deterministic demonstration using only baseline operations. */
public final class Main {
    private Main() {}

    /**
     * Runs the same client against both representations.
     *
     * @param args ignored command-line arguments
     */
    public static void main(String[] args) {
        demonstrate("ArrayList", new ArrayListBookstore());
        demonstrate("FixedArray", new FixedArrayBookstore(3));
    }

    private static void demonstrate(String label, BookstoreAPI store) {
        store.add(new Book("9780134685991", "Effective Java", "Joshua Bloch", 4599, 2018));
        store.add(new Book("0132350882", "Clean Code", "Robert Martin", 3299, 2008));
        System.out.println(label + " size=" + store.size());
        System.out.println("found=" + store.findByIsbn("978-0134685991").orElseThrow().title());
        System.out.println("snapshot=" + store.allBooks());
    }
}
