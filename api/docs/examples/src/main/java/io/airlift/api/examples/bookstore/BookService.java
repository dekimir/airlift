package io.airlift.api.examples.bookstore;

import io.airlift.api.ApiCreate;
import io.airlift.api.ApiDelete;
import io.airlift.api.ApiGet;
import io.airlift.api.ApiList;
import io.airlift.api.ApiOrderBy;
import io.airlift.api.ApiOrderByDirection;
import io.airlift.api.ApiPaginatedResult;
import io.airlift.api.ApiPagination;
import io.airlift.api.ApiParameter;
import io.airlift.api.ApiPatch;
import io.airlift.api.ApiResourceVersion;
import io.airlift.api.ApiService;
import io.airlift.api.ApiUpdate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static io.airlift.api.responses.ApiException.notFound;

/**
 * Book service implementing standard CRUD operations.
 * This demonstrates the core functionality of the API framework:
 * - List (with pagination)
 * - Get (by ID)
 * - Create
 * - Update (using patch)
 * - Delete
 */
@ApiService(name = "book", type = BookServiceType.class, description = "Manage books in the bookstore")
public class BookService
{
    private final Map<String, StoredBook> books = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    /**
     * Internal storage record combining ID, version, and book data.
     */
    private record StoredBook(BookId id, int version, BookData data) {}

    /**
     * List all books with pagination support.
     */
    @ApiList(description = "List all books in the bookstore")
    public ApiPaginatedResult<Book> listBooks(
            @ApiParameter ApiPagination pagination,
            @ApiParameter(allowedValues = {"title", "author", "year"}) ApiOrderBy ordering)
    {
        List<Book> allBooks = books.values().stream()
                .map(bd -> new Book(bd.id, new ApiResourceVersion(bd.version), bd.data))
                .toList();

        // Apply ordering if specified
        List<Book> orderedBooks = new ArrayList<>(allBooks);
        if (pagination.ordering().isPresent()) {
            var orderingSpec = pagination.ordering().get();
            Comparator<Book> comparator = switch (orderingSpec.field()) {
                case "title" -> Comparator.comparing(b -> b.data().title());
                case "author" -> Comparator.comparing(b -> b.data().author());
                case "year" -> Comparator.comparingInt(b -> b.data().year());
                default -> (a, b) -> 0;
            };
            if (orderingSpec.direction() == ApiOrderByDirection.DESCENDING) {
                comparator = comparator.reversed();
            }
            orderedBooks.sort(comparator);
        }

        // Apply pagination
        int pageOffset = pagination.pageToken().map(Integer::parseInt).orElse(0);
        List<Book> paginatedBooks = orderedBooks.stream()
                .skip((long) pageOffset * pagination.pageSize())
                .limit(pagination.pageSize())
                .toList();

        String nextPageToken = paginatedBooks.isEmpty()
                ? ApiPaginatedResult.EMPTY_PAGE_TOKEN
                : Integer.toString(pageOffset + 1);

        return new ApiPaginatedResult<>(nextPageToken, paginatedBooks);
    }

    /**
     * Get a specific book by ID.
     */
    @ApiGet(description = "Get a book by its ID")
    public Book getBook(@ApiParameter BookId bookId)
    {
        StoredBook stored = books.get(bookId.toString());
        if (stored == null) {
            throw notFound("Book not found: " + bookId);
        }
        return new Book(stored.id, new ApiResourceVersion(stored.version), stored.data);
    }

    /**
     * Create a new book.
     */
    @ApiCreate(description = "Create a new book")
    public Book createBook(BookData bookData)
    {
        String id = String.valueOf(nextId.getAndIncrement());
        BookId bookId = new BookId(id);
        StoredBook stored = new StoredBook(bookId, 1, bookData);
        books.put(id, stored);

        return new Book(bookId, new ApiResourceVersion(1), bookData);
    }

    /**
     * Update a book using patch semantics.
     * The ApiPatch automatically handles partial updates.
     */
    @ApiUpdate(description = "Update a book")
    public Book updateBook(
            @ApiParameter BookId bookId,
            ApiPatch<Book> patch)
    {
        StoredBook currentStored = books.get(bookId.toString());
        if (currentStored == null) {
            throw notFound("Book not found: " + bookId);
        }

        // Apply patch to current book
        Book currentBook = new Book(
                currentStored.id,
                new ApiResourceVersion(currentStored.version),
                currentStored.data);
        Book updatedBook = patch.apply(currentBook);

        // Store updated version
        int newVersion = currentStored.version + 1;
        StoredBook newStored = new StoredBook(
                updatedBook.bookId(),
                newVersion,
                updatedBook.data());
        books.put(bookId.toString(), newStored);

        return new Book(
                updatedBook.bookId(),
                new ApiResourceVersion(newVersion),
                updatedBook.data());
    }

    /**
     * Delete a book by ID.
     * Returns the deleted book.
     */
    @ApiDelete(description = "Delete a book")
    public Book deleteBook(@ApiParameter BookId bookId)
    {
        StoredBook stored = books.remove(bookId.toString());
        if (stored == null) {
            throw notFound("Book not found: " + bookId);
        }
        return new Book(stored.id, new ApiResourceVersion(stored.version), stored.data);
    }

    /**
     * Get the number of books (for testing purposes).
     */
    public int getBookCount()
    {
        return books.size();
    }
}
