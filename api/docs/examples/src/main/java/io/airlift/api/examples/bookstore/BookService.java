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
    private final Map<String, BookData> books = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    /**
     * Internal storage for book data.
     */
    private record BookData(BookId id, int version, NewBook data) {}

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
                case "title" -> Comparator.comparing(b -> b.bookData().title());
                case "author" -> Comparator.comparing(b -> b.bookData().author());
                case "year" -> Comparator.comparingInt(b -> b.bookData().year());
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
        BookData data = books.get(bookId.toString());
        if (data == null) {
            throw notFound("Book not found: " + bookId);
        }
        return new Book(data.id, new ApiResourceVersion(data.version), data.data);
    }

    /**
     * Create a new book.
     */
    @ApiCreate(description = "Create a new book")
    public Book createBook(NewBook newBook)
    {
        String id = String.valueOf(nextId.getAndIncrement());
        BookId bookId = new BookId(id);
        BookData data = new BookData(bookId, 1, newBook);
        books.put(id, data);

        return new Book(bookId, new ApiResourceVersion(1), newBook);
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
        BookData currentData = books.get(bookId.toString());
        if (currentData == null) {
            throw notFound("Book not found: " + bookId);
        }

        // Apply patch to current book
        Book currentBook = new Book(
                currentData.id,
                new ApiResourceVersion(currentData.version),
                currentData.data);
        Book updatedBook = patch.apply(currentBook);

        // Store updated version
        int newVersion = currentData.version + 1;
        BookData newData = new BookData(
                updatedBook.bookId(),
                newVersion,
                updatedBook.bookData());
        books.put(bookId.toString(), newData);

        return new Book(
                updatedBook.bookId(),
                new ApiResourceVersion(newVersion),
                updatedBook.bookData());
    }

    /**
     * Delete a book by ID.
     * Returns the deleted book.
     */
    @ApiDelete(description = "Delete a book")
    public Book deleteBook(@ApiParameter BookId bookId)
    {
        BookData data = books.remove(bookId.toString());
        if (data == null) {
            throw notFound("Book not found: " + bookId);
        }
        return new Book(data.id, new ApiResourceVersion(data.version), data.data);
    }

    /**
     * Get the number of books (for testing purposes).
     */
    public int getBookCount()
    {
        return books.size();
    }
}
