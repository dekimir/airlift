package io.airlift.api.examples.bookstoreNew;

import io.airlift.api.ApiDescription;
import io.airlift.api.ApiReadOnly;
import io.airlift.api.ApiResource;
import io.airlift.api.ApiResourceVersion;
import io.airlift.api.ApiStringId;
import io.airlift.api.ApiUnwrapped;

import static java.util.Objects.requireNonNull;

/**
 * Complete Book resource including system metadata (ID and version).
 */
@ApiResource(name = "book", description = "Book resource with metadata")
public record Book(
        @ApiDescription("Unique book identifier") @ApiReadOnly BookId bookDataId,
        ApiResourceVersion syncToken,
        @ApiUnwrapped BookData data)
{
    public Book
    {
        requireNonNull(bookDataId, "bookId is null");
        requireNonNull(syncToken, "syncToken is null");
        requireNonNull(data, "data is null");
    }

    public static class BookId
            extends ApiStringId<BookData>
    {
        public BookId(String id)
        {
            super(id);
        }
    }
}
