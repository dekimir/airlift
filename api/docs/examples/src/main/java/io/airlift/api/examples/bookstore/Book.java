package io.airlift.api.examples.bookstore;

import io.airlift.api.ApiDescription;
import io.airlift.api.ApiReadOnly;
import io.airlift.api.ApiResource;
import io.airlift.api.ApiResourceVersion;
import io.airlift.api.ApiUnwrapped;

import static java.util.Objects.requireNonNull;

/**
 * Complete Book resource including ID and version information.
 * The ApiUnwrapped annotation means all fields from NewBook are
 * included directly in this resource without nesting.
 */
@ApiResource(name = "book", description = "A book in the bookstore")
public record Book(
        @ApiDescription("Unique book identifier") @ApiReadOnly BookId bookId,
        ApiResourceVersion syncToken,
        @ApiUnwrapped NewBook bookData)
{
    public Book
    {
        requireNonNull(bookId, "bookId is null");
        requireNonNull(syncToken, "syncToken is null");
        requireNonNull(bookData, "bookData is null");
    }
}
