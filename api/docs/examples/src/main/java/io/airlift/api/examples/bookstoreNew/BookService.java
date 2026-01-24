package io.airlift.api.examples.bookstoreNew;

import io.airlift.api.ApiCreate;
import io.airlift.api.ApiGet;
import io.airlift.api.ApiParameter;
import io.airlift.api.ApiService;
import io.airlift.api.ApiStringId;
import io.airlift.api.ApiTrait;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static io.airlift.api.responses.ApiException.badRequest;

@ApiService(name = "bookService", type = BookServiceType.class, description = "Manage books in the bookstore")
public class BookService
{
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final Map<String, BookData> books = new ConcurrentHashMap<>();

    @ApiCreate(description = "Create a new book", traits = {ApiTrait.BETA}) // TODO: Remove BETA trait and explain quotas.
    public void createBook(BookData bookData)
    {
        if (bookData == null) {
            throw badRequest("Must provide BookData payload");
        }
        String id = String.valueOf(nextId.getAndIncrement()); // BookData includes ISBN, but ISBN is neither universal nor actually unique.
        books.put(id, bookData);
    }

    @ApiGet(description = "Get a book by its ID")
    public BookData getBook(@ApiParameter BookId id)
    {
        return new BookData("Sample Title", "Sample Author", id.toString(), 2024, 29.99);
    }

    public static class BookId extends ApiStringId<BookData>
    {
        public BookId(String id)
        {
            super(id);
        }
    }

//    @ApiList(description = "List all books in the bookstore")
//    public List<BookData> listBooks()
//    {
//        return ImmutableList.copyOf(books.values());
//    }
}
