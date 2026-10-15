# Lab 01 — Encapsulation, Contracts, Arrays and ArrayList

CSSD2101 Object-Oriented Programming — package `edu.cssd2101.lab01`

## 1. What this project is

A small ISBN-keyed book catalogue exposed through one interface, `BookstoreAPI`, with two
interchangeable implementations (`ArrayListBookstore`, backed by a dynamic list; and
`FixedArrayBookstore`, backed by a bounded array with a live-prefix invariant), plus a stateless
utility class, `BookArrayUtils`, for operating on caller-owned sparse arrays.

## 2. Container ownership

```
Main
 └─ depends on → BookstoreAPI (interface: defines the contract, shares default query logic)
                    ├─ ArrayListBookstore   — owns: private final List<Book> books (grows freely)
                    └─ FixedArrayBookstore  — owns: private final Book[] books (fixed capacity)
                                                     private int size (live-prefix length)

Book (immutable)
  • Not owned exclusively by any store — the same Book instance can be referenced by
    multiple stores, or exist independently, because it can never be mutated after
    construction. A shallow copy of a container is sufficient: every reachable field of
    Book is an immutable String or a primitive.

BookArrayUtils (stateless, no fields)
  • Never stores a reference to a caller's array. Every method that mutates its input
    (sortByPrice, sortByYear) says so explicitly in its Javadoc; every other method
    returns an independent new array.
```

A `Book` is data; a store is a container. Because `Book` is immutable, sharing one instance
across stores (or across a store and a caller's snapshot) is always safe — there is no
"who owns this book" question, only "who owns this container of references."

## 3. Domain contract decisions (`Book`)

| Concern | Decision | Why |
|---|---|---|
| ISBN syntax | 13 digits, or 9 digits + digit/`X` (uppercase only); hyphens and surrounding whitespace stripped before validation | Syntax check only — this edition does not implement real ISBN check-digit arithmetic, and the Javadoc says so rather than overclaiming |
| Text (title/author) | `null` → `NullPointerException`; blank after `.strip()` → `IllegalArgumentException`; interior whitespace untouched | Distinguishes a programmer error (null) from a data error (blank), and only trims accidental padding, not content |
| Price | Nonnegative `long` cents; `0` is a valid free entry | Keeps money exact (no floating point); explicitly allows the free-book edge case rather than treating `0` as falsy |
| Year | Fixed inclusive range `1450..2027`, no clock read | Makes tests deterministic forever — the range is an *exercise policy*, not a historical claim |
| Identity | `equals`, `hashCode`, `compareTo` all key on normalized ISBN only | Two `Book`s with the same ISBN and different metadata (e.g. a corrected title) represent the same catalogue entry — see T1 test `sameIsbnMeansSameIdentityRegardlessOfMetadata` |
| Display order | `Book.DISPLAY_ORDER` sorts by case-insensitive title, then ISBN — deliberately *not* the same as natural order | Proven distinct in `differentIsbnsWithSameTitleBothSurviveInATreeSet`: two different-ISBN books with the same title are two different identities, so both survive in a `TreeSet<Book>` |

## 4. Catalogue contract decisions (`BookstoreAPI`)

| Concern | Decision |
|---|---|
| Duplicate add | Returns `false`, keeps the **original** object (`assertSame`, not just `equals`) — a duplicate add never replaces metadata |
| Snapshots (`allBooks`) | Immutable (`List.copyOf`), detached from the live store — later insertions never appear in an earlier snapshot |
| Snapshots (`snapshotArray`) | Mutable but detached — clearing a slot in the exported array cannot touch the store |
| Search (`findByTitle`/`findByAuthor`) | Case-insensitive substring match using `Locale.ROOT` (not the platform default locale, which avoids the Turkish-`i` class of bugs) |
| Blank search query | Rejected with `IllegalArgumentException` rather than silently matching everything — `allBooks()` is the explicit way to get everything |
| `findByPriceRange` | Inclusive bounds; rejects negative minimum or `maximum < minimum` |
| `inventoryValueCents` | Uses `Math.addExact` — silent `long` overflow would report a wrong (possibly negative) total, which is worse than throwing |
| `mostExpensive` / `mostRecent` | Tie-break: **first-inserted entry wins**, implemented with strict `>` (not `>=`) — see bug write-up below |

## 5. The fixed-array invariant (`FixedArrayBookstore`)

For capacity `C` and live count `n`: `0 ≤ n ≤ C`. Indices `0..n-1` hold live, distinct-ISBN
books; indices `n..C-1` are `null`. `removeByIsbn` must preserve this after removing from any
position:

| State | 0 | 1 | 2 | 3 | size |
|---|---|---|---|---|---|
| Before removal | A | B | C | null | 3 |
| Shift survivors left | A | C | C | null | 3 (mid-step, not a valid published state) |
| Clear vacated slot | A | C | null | null | 2 |

Implementation (`System.arraycopy` shifts the survivors, then the last live slot is
explicitly nulled out — otherwise the array would retain a stale reference past the live
prefix, which is a memory-retention bug even though it's functionally invisible to
`allBooks()` since that method only ever reads indices `0..size-1`).

Duplicate detection happens **before** the capacity check in `add`, because rejecting a
duplicate never needs a new slot — this lets a full store still correctly report `false`
for a duplicate instead of incorrectly throwing `IllegalStateException`.

## 6. One bug found through a negative test

**Where:** test source layout, not production code — `src/test/java/.../BookTest.java`
colliding with the instructor-supplied `src/acceptance/java/.../BookTest.java`.

**The trap:** `src/test/java` and `src/acceptance/java` are two *separate* source
roots, so they can each contain a file named `BookTest.java` without any error —
right up until something puts both folders on the same compile classpath at once.
That "something" is the `-Pacceptance` Maven profile: it uses the
`build-helper-maven-plugin` to add `src/acceptance/java` as an *additional* test
source directory on top of the normal `src/test/java`. Naming our own T1 test class
`BookTest` (the obvious, idiomatic JUnit name for "tests the `Book` class") silently
worked under a plain `./mvnw test`, because only one `BookTest` was ever on the
classpath at a time — and then failed the moment we actually ran the full contract
suite:

```
[ERROR] COMPILATION ERROR :
[ERROR] .../src/test/java/edu/cssd2101/lab01/BookTest.java:[6,1] duplicate class: edu.cssd2101.lab01.BookTest
```

**Smallest reproducing input:** two files, each declaring `class BookTest` in
package `edu.cssd2101.lab01`, one under `src/test/java`, one under
`src/acceptance/java` — as soon as both roots are active test sources
simultaneously (i.e. `-Pacceptance` is passed), `javac` refuses to compile
because a fully-qualified class name must be unique across the whole compilation
unit, regardless of which directory each file physically lives in.

**The fix:** rename the file *and* its class declaration —
`src/test/java/.../BookTest.java` → `BookIdentityTest.java`, with
`class BookTest` → `class BookIdentityTest` inside it. This preserves every test
method and assertion unchanged; only the identity of the test class itself
changes, so it can no longer collide with the instructor's fixed, unchangeable
acceptance file.

**Why the fix preserves the invariant:** the actual invariant here isn't about
`Book` at all — it's "every class in `edu.cssd2101.lab01` must have a name unique
across every source root that can be active together." The starter's own
`src/acceptance/java` package is fixed and shouldn't be touched (it's the graded
contract suite), so the only side that can safely change is ours. Renaming only
the file we control, with no change to test logic, satisfies the uniqueness
requirement without weakening or deleting a single assertion — which matters,
since the handout explicitly warns against "editing away a failing assertion to
claim completion." This bug was never in `Book`, `BookstoreAPI`,
`FixedArrayBookstore`, or `BookArrayUtils` — the production code was correct the
whole time; only our own test file's *name* was the problem, and it only became
visible once the full acceptance profile (rather than the everyday `mvn test`)
was actually run.

## 7. Complexity analysis

| Operation | Complexity | Justification from the actual code |
|---|---|---|
| `add` | O(n) time, O(1) extra space | Both implementations linearly scan existing entries for a duplicate ISBN before inserting (`ArrayListBookstore` via `List.contains`, which calls `equals` per element; `FixedArrayBookstore` via an explicit loop). The append/write itself is O(1); the scan dominates. |
| `removeByIsbn` | O(n) time, O(1) extra space | O(n) to find the match, then O(n) to shift survivors left (`ArrayList.removeIf`'s internal shift, or `FixedArrayBookstore`'s explicit `System.arraycopy`). No extra array/list is allocated. |
| Snapshot / query (`allBooks`, `findByX`, `inventoryValueCents`, `mostExpensive`, …) | O(n) time, **O(n) auxiliary space** | Every default method on `BookstoreAPI` calls `allBooks()` first, which always allocates a fresh `List.copyOf(...)` snapshot of size n, then does a single O(n) pass over it. This is a deliberate simplicity/performance trade-off called out in the handout: correct and easy to audit, but not the fastest possible design — a caller doing many repeated queries pays the O(n) snapshot cost every single time. |
| `sortByPrice` / `sortByYear` | O(n log n) comparisons, O(n) auxiliary space | `Arrays.sort` on an `Object[]` uses a stable, adaptive merge sort (TimSort), which needs O(n) temporary working space internally. The array is sorted **in place** (mutates the caller's array) — no new array is returned. |
| `removeDuplicates` | Expected O(n) time, O(n) extra space | Backed by a `LinkedHashMap<String, Book>` keyed by ISBN, giving expected O(1) `putIfAbsent` per element (amortized hash table operations) and preserving encounter order for free via the map's iteration order. The map itself, plus the output array, are the O(n) extra space. |

A note the handout specifically asks not to skip: **we never claim an array is "always
faster" than `ArrayList`, or that `ArrayList`'s growth factor is guaranteed by the JDK
spec** — both are implementation details, not contractual guarantees.

## 8. Task → test mapping

| Task | What it required | Where it's proven |
|---|---|---|
| T1 | Audit the immutable `Book` model | `src/test/java/.../BookIdentityTest.java` — 8 tests: normalization, free price, year boundaries (both directions), null/blank text, malformed ISBN, identity vs. metadata, `TreeSet` natural-order proof. (Named `BookIdentityTest`, not `BookTest`, to avoid colliding with the instructor's own hidden `BookTest` in `src/acceptance/java` once the `-Pacceptance` profile adds that folder to the test sources — a real compile-time bug we hit and fixed during development.) |
| T2 | Test the baseline API identically against both implementations | `src/test/java/.../BaselineTest.java` (starter-supplied smoke tests) + `src/test/java/.../BaselineApiTest.java` — 3 parameterized tests × 2 implementations, using one shared `newStore(boolean)` helper |
| T3 | Author search, price-range search, inventory value, most-expensive/most-recent | Implemented in `BookstoreAPI.java`; proven by the instructor-supplied `src/acceptance/java/.../StoreContractTest.java` (enabled via `-Pacceptance`) |
| T4 | `FixedArrayBookstore.removeByIsbn` under the array invariant | Implemented in `FixedArrayBookstore.java`; proven by `StoreContractTest.java`, including the full-capacity edge cases |
| T5 | `BookArrayUtils.filterPriceAtMost`, `averagePrice`, `removeDuplicates` | Implemented in `BookArrayUtils.java`; proven by `src/acceptance/java/.../ArrayUtilsTest.java` |

## 9. Reproducing the evidence

This project targets Java SE 26 (`maven.compiler.release=26` in `pom.xml`). Run these from
the project root once you have a matching JDK selected (see the shared setup chapter):

```bash
./mvnw clean verify              # baseline smoke tests only
./mvnw -Pacceptance clean test   # full contract suite (T3/T4/T5 proof)
./mvnw javadoc:javadoc           # generated API docs
./mvnw -q exec:java              # runs Main, reproduces the documented demo output
```

### Measured results (actual build, JDK 26, `./mvnw -Pacceptance clean verify`)

```
Tests run: 43, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Test classes: `ArrayUtilsTest` (5), `BaselineApiTest` (6), `BaselineTest` (2),
`BookIdentityTest` (8, our own T1 tests), `BookTest` (10, instructor's hidden T1
acceptance test), `StoreContractTest` (12, instructor's T3/T4 acceptance test).

JaCoCo coverage report (`target/site/jacoco/index.html`):

| Metric | Coverage | Missed / Total |
|---|---|---|
| Instructions | 100% | 0 / 870 |
| Branches | 97% | 3 / 100 |
| Lines | 100% | 0 / 143 |
| Methods | 100% | 0 / 53 |
| Classes | 100% | 0 / 6 |

This clears the 70% line-coverage floor the handout describes comfortably. The 3
uncovered branches are worth naming honestly rather than hiding, though we have not
individually traced which exact conditional branches they are — the JaCoCo HTML report
highlights uncovered branches in yellow/red per class/line if you click into
`edu.cssd2101.lab01` from `index.html`, which is the way to identify them precisely
before the group video. 100% branch coverage was not a goal we chased — the handout is
explicit that a coverage percentage alone does not establish correctness, and every
branch that *is* covered is covered by a test that asserts a specific documented
behavior, not just by executing the line.

## 10. Known limitations / things intentionally out of scope

- ISBN validation is syntax-only; no check-digit arithmetic (documented, not a bug).
- Every default query method pays an O(n) snapshot allocation, even for something like
  `findByYear` that could theoretically be answered without materializing a full list
  first. Traded off deliberately for shared, easy-to-audit logic across both store
  implementations.
- `BookstoreAPI` has no thread-safety guarantees; concurrent access is explicitly out of
  scope per the interface Javadoc.

## 11. Group video

This is a one-person group. Not included here — recorded separately per the shared
guidance (≤20 minutes, speaker clearly identifiable, covering: what changed from the
starter, design/contracts/invariants walked through in code, representative normal +
boundary-case behavior, actual full-suite test results, remaining limitations). No
contribution summary is needed across members since there is only one; the "each
member's contribution" section of the required outline reduces to a full account of the
work in this submission.
