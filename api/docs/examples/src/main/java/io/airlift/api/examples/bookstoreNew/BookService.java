package io.airlift.api.examples.bookstoreNew;

import com.google.common.collect.ImmutableList;
import io.airlift.api.ApiCreate;
import io.airlift.api.ApiGet;
import io.airlift.api.ApiList;
import io.airlift.api.ApiParameter;
import io.airlift.api.ApiResourceVersion;
import io.airlift.api.ApiService;
import io.airlift.api.ApiTrait;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static io.airlift.api.responses.ApiException.badRequest;
import static io.airlift.api.responses.ApiException.notFound;

@ApiService(name = "bookService", type = BookServiceType.class, description = "Manage books in the bookstore")
public class BookService
{
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final Map<String, Book> books = new ConcurrentHashMap<>();

    @ApiCreate(description = "Create a new book", traits = {ApiTrait.BETA}) // TODO: Remove BETA trait and explain quotas.
    public Book createBook(BookData bookData)
    {
        if (bookData == null) {
            throw badRequest("Must provide BookData payload");
        }
        String id = String.valueOf(nextId.getAndIncrement()); // BookData includes ISBN, but ISBN is neither universal nor actually unique.
        Book book = new Book(new Book.Id(id), new ApiResourceVersion(), bookData);
        books.put(id, book);
        return book;
    }

    @ApiGet(description = "Get a book by its ID")
    public Book getBook(@ApiParameter Book.Id id)
    {
        Book book = books.get(id.toString());
        if (book == null) {
            throw notFound("Book with ID %s not found".formatted(id));
        }
        return book;
    }

    @ApiList(description = "List all books in the bookstore")
    public List<Book> listBooks()
    {
        return ImmutableList.copyOf(books.values());
    }
}
