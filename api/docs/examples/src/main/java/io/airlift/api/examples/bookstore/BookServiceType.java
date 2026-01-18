package io.airlift.api.examples.bookstore;

import io.airlift.api.ApiServiceType;

/**
 * Service type identifier for the book service.
 * This is used to group related API services together.
 */
public class BookServiceType implements ApiServiceType
{
    @Override
    public String id()
    {
        return "bookstore";
    }

    @Override
    public int version()
    {
        return 1;
    }

    @Override
    public String title()
    {
        return "Bookstore API";
    }

    @Override
    public String description()
    {
        return "API for managing books in a bookstore";
    }
}
