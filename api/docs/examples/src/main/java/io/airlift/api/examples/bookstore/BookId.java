package io.airlift.api.examples.bookstore;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.airlift.api.ApiId;

/**
 * Unique identifier for a Book resource.
 * Extends ApiId to provide type-safe ID handling.
 *
 * Note: We use BookInternalId as a simple wrapper to avoid
 * constructor ambiguity when using String as the internal ID type.
 */
public class BookId extends ApiId<Book, BookId.BookInternalId>
{
    /**
     * Simple internal ID wrapper.
     */
    public record BookInternalId(String value)
    {
        @Override
        public String toString()
        {
            return value;
        }
    }

    /**
     * Default constructor required by the framework.
     */
    public BookId()
    {
        this("default-book-id");
    }

    /**
     * Create a BookId from an internal ID.
     */
    public BookId(BookInternalId internalId)
    {
        super(internalId);
    }

    /**
     * JsonCreator allows BookId to be deserialized from JSON.
     */
    @JsonCreator
    public BookId(String id)
    {
        super(id);
    }
}
