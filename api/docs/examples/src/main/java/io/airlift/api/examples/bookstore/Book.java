package io.airlift.api.examples.bookstore;

import io.airlift.api.ApiDescription;
import io.airlift.api.ApiReadOnly;
import io.airlift.api.ApiResource;
import io.airlift.api.ApiResourceVersion;
import io.airlift.api.ApiUnwrapped;

import static java.util.Objects.requireNonNull;

/**
 * Complete Book resource including system metadata (ID and version).
 * Represents a book resource in the API - book data plus system-generated metadata.
 * The ApiUnwrapped annotation means all fields from BookData are
 * included directly in this resource without nesting.
 */
@ApiResource(name = "book", description = "Book resource with metadata")
public record Book(
        @ApiDescription("Unique book identifier") @ApiReadOnly BookId bookId,
        ApiResourceVersion syncToken,
        @ApiUnwrapped BookData data)
{
    public Book
    {
        requireNonNull(bookId, "bookId is null");
        requireNonNull(syncToken, "syncToken is null");
        requireNonNull(data, "data is null");
    }
}
