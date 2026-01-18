# Building Your First API Service: A Step-by-Step Tutorial

This tutorial walks you through building an API service from scratch using the Airlift API framework. We'll build a simple bookstore API, explaining each piece of code and why it's needed.

> **Complete Example**: You can find the complete working code in [examples/src/main/java/io/airlift/api/examples/bookstore/](examples/src/main/java/io/airlift/api/examples/bookstore/)

## Prerequisites

- Java 21 or later
- Maven
- Basic understanding of REST APIs
- Familiarity with Java records

## Understanding the Resource-Oriented Architecture

Before writing any code, understand that this framework follows a **resource-oriented** design:

- **Resources** are the "things" your API manages (e.g., books, users, orders)
- **Methods** are standard operations on those resources (List, Get, Create, Update, Delete)
- **Services** expose these methods through HTTP endpoints

## Step 1: Define the Resource ID

Every resource needs a unique identifier. Start by creating the ID class.

### Why IDs Come First

Resources need IDs, so we define the ID class before the resource itself. The ID provides:
- Type safety (you can't accidentally pass a `BookId` where a `UserId` is expected)
- Automatic serialization/deserialization
- Conversion between external (string) and internal (your database) ID formats

### Create BookId.java

```java
package io.airlift.api.examples.bookstore;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.airlift.api.ApiId;

public class BookId extends ApiId<Book, BookId.BookInternalId>
{
    // Wrapper to avoid constructor ambiguity
    public record BookInternalId(String value)
    {
        @Override
        public String toString()
        {
            return value;
        }
    }

    // Required: Default constructor
    public BookId()
    {
        this("default-book-id");
    }

    // For internal use (your backend code)
    public BookId(BookInternalId internalId)
    {
        super(internalId);
    }

    // For JSON deserialization
    @JsonCreator
    public BookId(String id)
    {
        super(id);
    }
}
```

**Key Points:**
- Extends `ApiId<RESOURCE, INTERNAL_ID_TYPE>`
- `RESOURCE` is the resource class this ID identifies (we'll create `Book` next)
- `INTERNAL_ID_TYPE` is your internal ID format (UUID, Long, String wrapper, etc.)
- **Must have a default constructor** - the framework requires this
- **@JsonCreator** on the String constructor allows JSON like `{"bookId": "123"}` to work

**See:** [examples/src/.../BookId.java](examples/src/main/java/io/airlift/api/examples/bookstore/BookId.java)

## Step 2: Define the "New" Resource

When clients create a resource, they don't provide an ID or version (the server generates those). So we create a "New" resource class.

### Why Two Resource Classes?

- **NewBook**: Used for creation - contains only the fields clients provide
- **Book**: The complete resource - includes ID, version, and all data

This separation is cleaner than having nullable IDs or version fields.

### Create NewBook.java

```java
package io.airlift.api.examples.bookstore;

import io.airlift.api.ApiDescription;
import io.airlift.api.ApiResource;

import static java.util.Objects.requireNonNull;

@ApiResource(name = "book", description = "A book in the bookstore")
public record NewBook(
        @ApiDescription("Title of the book") String title,
        @ApiDescription("Author of the book") String author,
        @ApiDescription("ISBN number") String isbn,
        @ApiDescription("Year published") int year,
        @ApiDescription("Price in USD") double price)
{
    // Compact constructor for validation
    public NewBook
    {
        requireNonNull(title, "title is null");
        requireNonNull(author, "author is null");
        requireNonNull(isbn, "isbn is null");

        if (year < 0) {
            throw new IllegalArgumentException("year must be positive");
        }
        if (price < 0) {
            throw new IllegalArgumentException("price must be positive");
        }
    }
}
```

**Key Points:**
- **@ApiResource** marks this as an API resource
  - `name` is used in URIs (`/v1/bookstore/books`)
  - `description` appears in OpenAPI documentation
- **@ApiDescription** documents each field
- Use Java record's **compact constructor** for validation
- Contains only the mutable, client-provided fields

**See:** [examples/src/.../NewBook.java](examples/src/main/java/io/airlift/api/examples/bookstore/NewBook.java)

## Step 3: Define the Complete Resource

Now create the full resource that includes the ID and version information.

### Why Version Tokens?

`ApiResourceVersion` (the "syncToken") enables:
- **Optimistic locking**: Prevent lost updates when multiple clients modify the same resource
- **Conditional operations**: Update only if the version matches
- **Change tracking**: Know when a resource was last modified

### Create Book.java

```java
package io.airlift.api.examples.bookstore;

import io.airlift.api.ApiDescription;
import io.airlift.api.ApiReadOnly;
import io.airlift.api.ApiResource;
import io.airlift.api.ApiResourceVersion;
import io.airlift.api.ApiUnwrapped;

import static java.util.Objects.requireNonNull;

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
```

**Key Points:**
- **Three required fields** for complete resources:
  1. **Resource ID** (with `@ApiReadOnly` - clients can't modify it)
  2. **ApiResourceVersion syncToken** (for versioning)
  3. **@ApiUnwrapped NewBook** (includes all the data fields without nesting)
- **@ApiUnwrapped** means all fields from `NewBook` appear directly in the JSON:
  ```json
  {
    "bookId": "123",
    "syncToken": {"version": 1},
    "title": "...",      // These come from NewBook
    "author": "...",     // but appear at the top level
    "isbn": "...",
    "year": 2020,
    "price": 29.99
  }
  ```
  Without `@ApiUnwrapped`, you'd have nested JSON: `{"bookId": "123", "bookData": {...}}`

**See:** [examples/src/.../Book.java](examples/src/main/java/io/airlift/api/examples/bookstore/Book.java)

## Step 4: Create the Service Type

Service types group related APIs and define their version.

### Why Service Types?

- **Versioning**: Different versions of your API can coexist (`/v1/bookstore/...`, `/v2/bookstore/...`)
- **Grouping**: Related services share a service type
- **Documentation**: Appears in OpenAPI specs

### Create BookServiceType.java

```java
package io.airlift.api.examples.bookstore;

import io.airlift.api.ApiServiceType;

public class BookServiceType implements ApiServiceType
{
    @Override
    public String id()
    {
        return "bookstore";  // Used in URI: /v1/bookstore/...
    }

    @Override
    public int version()
    {
        return 1;  // API version: /v1/...
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
```

**Key Points:**
- **id()**: Appears in URIs (`/v{version}/{id}/...`)
- **version()**: API version number
- **title()** and **description()**: OpenAPI documentation

**See:** [examples/src/.../BookServiceType.java](examples/src/main/java/io/airlift/api/examples/bookstore/BookServiceType.java)

## Step 5: Implement the Service

Now we can finally implement the service with its methods.

### Understanding Standard Methods

The framework provides five standard method annotations:
- **@ApiList**: List all resources (with pagination)
- **@ApiGet**: Get a single resource by ID
- **@ApiCreate**: Create a new resource
- **@ApiUpdate**: Update a resource (PATCH semantics)
- **@ApiDelete**: Delete a resource

### Create BookService.java (Minimal Version)

Let's start with just Create and Get:

```java
package io.airlift.api.examples.bookstore;

import io.airlift.api.ApiCreate;
import io.airlift.api.ApiGet;
import io.airlift.api.ApiParameter;
import io.airlift.api.ApiResourceVersion;
import io.airlift.api.ApiService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static io.airlift.api.responses.ApiException.notFound;

@ApiService(
    name = "book",
    type = BookServiceType.class,
    description = "Manage books in the bookstore"
)
public class BookService
{
    // In-memory storage (use a real database in production!)
    private final Map<String, BookData> books = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    private record BookData(BookId id, int version, NewBook data) {}

    @ApiCreate(description = "Create a new book")
    public Book createBook(NewBook newBook)
    {
        // Generate ID
        String id = String.valueOf(nextId.getAndIncrement());
        BookId bookId = new BookId(id);

        // Store it
        BookData data = new BookData(bookId, 1, newBook);
        books.put(id, data);

        // Return the complete resource
        return new Book(bookId, new ApiResourceVersion(1), newBook);
    }

    @ApiGet(description = "Get a book by its ID")
    public Book getBook(@ApiParameter BookId bookId)
    {
        BookData data = books.get(bookId.toString());
        if (data == null) {
            throw notFound("Book not found: " + bookId);
        }
        return new Book(data.id, new ApiResourceVersion(data.version), data.data);
    }
}
```

**Key Points:**
- **@ApiService** configures the service:
  - `name`: Resource name (used in URIs: `/books`)
  - `type`: Links to the service type we created
  - `description`: OpenAPI documentation
- **@ApiCreate** method:
  - Takes `NewBook` as input (the data without ID/version)
  - Returns complete `Book` (with ID and version)
  - Framework automatically creates endpoint: `POST /v1/bookstore/books`
- **@ApiGet** method:
  - Takes `@ApiParameter BookId` (from the URL path)
  - Returns `Book` or throws exception if not found
  - Framework automatically creates endpoint: `GET /v1/bookstore/books/{bookId}`
- **Error handling**: Use `ApiException.notFound()`, `ApiException.badRequest()`, etc.

### Adding More Methods

Now add List, Update, and Delete. See the complete implementation in [examples/src/.../BookService.java](examples/src/main/java/io/airlift/api/examples/bookstore/BookService.java).

#### @ApiList - List Resources

```java
@ApiList(description = "List all books in the bookstore")
public ApiPaginatedResult<Book> listBooks(
        @ApiParameter ApiPagination pagination,
        @ApiParameter(allowedValues = {"title", "author", "year"}) ApiOrderBy ordering)
{
    // 1. Get all books
    List<Book> allBooks = books.values().stream()
            .map(bd -> new Book(bd.id, new ApiResourceVersion(bd.version), bd.data))
            .toList();

    // 2. Apply ordering (if pagination contains ordering)
    // ... sorting logic ...

    // 3. Apply pagination
    int pageOffset = pagination.pageToken().map(Integer::parseInt).orElse(0);
    List<Book> paginatedBooks = orderedBooks.stream()
            .skip((long) pageOffset * pagination.pageSize())
            .limit(pagination.pageSize())
            .toList();

    // 4. Return paginated result with next page token
    String nextPageToken = paginatedBooks.isEmpty()
            ? ApiPaginatedResult.EMPTY_PAGE_TOKEN
            : Integer.toString(pageOffset + 1);

    return new ApiPaginatedResult<>(nextPageToken, paginatedBooks);
}
```

**Key Points:**
- **ApiPagination**: Framework injects this from query parameters (`?pageSize=10&pageToken=abc`)
- **ApiOrderBy**: Framework parses `?orderBy=title desc`
- **allowedValues**: Validates which fields can be used for ordering
- **ApiPaginatedResult**: Wrapper that includes results and next page token
- Creates endpoint: `GET /v1/bookstore/books?pageSize=10&orderBy=title`

#### @ApiUpdate - Update Resources

```java
@ApiUpdate(description = "Update a book")
public Book updateBook(
        @ApiParameter BookId bookId,
        ApiPatch<Book> patch)
{
    // 1. Get current book
    BookData currentData = books.get(bookId.toString());
    if (currentData == null) {
        throw notFound("Book not found: " + bookId);
    }

    // 2. Apply patch to current book
    Book currentBook = new Book(
            currentData.id,
            new ApiResourceVersion(currentData.version),
            currentData.data);
    Book updatedBook = patch.apply(currentBook);

    // 3. Increment version and store
    int newVersion = currentData.version + 1;
    BookData newData = new BookData(
            updatedBook.bookId(),
            newVersion,
            updatedBook.bookData());
    books.put(bookId.toString(), newData);

    // 4. Return updated book
    return new Book(
            updatedBook.bookId(),
            new ApiResourceVersion(newVersion),
            updatedBook.bookData());
}
```

**Key Points:**
- **ApiPatch<Book>**: Handles partial updates automatically
  - Client sends only changed fields: `{"price": 39.99}`
  - Framework merges with existing resource
- **Version increment**: Always increment version on updates
- Creates endpoint: `PATCH /v1/bookstore/books/{bookId}`

#### @ApiDelete - Delete Resources

```java
@ApiDelete(description = "Delete a book")
public Book deleteBook(@ApiParameter BookId bookId)
{
    BookData data = books.remove(bookId.toString());
    if (data == null) {
        throw notFound("Book not found: " + bookId);
    }
    // Return the deleted book
    return new Book(data.id, new ApiResourceVersion(data.version), data.data);
}
```

**Key Points:**
- **Returns the deleted resource** (common pattern - lets client know what was deleted)
- Creates endpoint: `DELETE /v1/bookstore/books/{bookId}`

## Step 6: Bootstrap the Server

Finally, create a main class to run your API server.

### Create BookstoreServer.java

```java
package io.airlift.api.examples.bookstore;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.airlift.api.binding.ApiModule;
import io.airlift.bootstrap.Bootstrap;
import io.airlift.http.server.HttpServerInfo;
import io.airlift.http.server.testing.TestingHttpServerModule;
import io.airlift.jaxrs.JaxrsModule;
import io.airlift.json.JsonModule;
import io.airlift.node.NodeModule;

import java.net.URI;

public class BookstoreServer
{
    private final Injector injector;
    private final URI baseUri;

    public BookstoreServer(int port)
    {
        // 1. Set up required modules
        ImmutableList.Builder<Module> modules = ImmutableList.<Module>builder()
                .add(new NodeModule())                              // Node info
                .add(new TestingHttpServerModule(getClass().getName(), port))  // HTTP server
                .add(new JsonModule())                              // JSON serialization
                .add(new JaxrsModule());                            // JAX-RS support

        // 2. Configure the API module with our service
        ApiModule.Builder apiBuilder = ApiModule.builder()
                .addApi(builder -> builder.add(BookService.class));
        modules.add(apiBuilder.build());

        // 3. Set server properties
        ImmutableMap.Builder<String, String> serverProperties =
            ImmutableMap.<String, String>builder()
                .put("node.environment", "development");

        // 4. Bootstrap the application
        Bootstrap app = new Bootstrap(modules.build());
        injector = app.setRequiredConfigurationProperties(serverProperties.build())
                .initialize();

        // 5. Get the server URI
        HttpServerInfo httpServerInfo = injector.getInstance(HttpServerInfo.class);
        baseUri = httpServerInfo.getHttpUri();
    }

    public static void main(String[] args)
    {
        new BookstoreServer(8080);
        System.out.println("Server started on http://localhost:8080");
        System.out.println("Try: GET http://localhost:8080/v1/bookstore/books");
    }
}
```

**Key Points:**
- **Required modules**:
  - `NodeModule`: Node configuration
  - `TestingHttpServerModule`: Embedded HTTP server (use real server module in production)
  - `JsonModule`: JSON serialization
  - `JaxrsModule`: JAX-RS/Jersey support
- **ApiModule**: Registers your service with `.add(BookService.class)`
- **Bootstrap**: Ties everything together using Guice dependency injection

**See:** [examples/src/.../BookstoreServer.java](examples/src/main/java/io/airlift/api/examples/bookstore/BookstoreServer.java)

## Step 7: Create Your Project Structure

Create a Maven project:

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-api</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <dep.airlift.version>389-SNAPSHOT</dep.airlift.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.airlift</groupId>
            <artifactId>api</artifactId>
            <version>${dep.airlift.version}</version>
        </dependency>
        <dependency>
            <groupId>io.airlift</groupId>
            <artifactId>bootstrap</artifactId>
            <version>${dep.airlift.version}</version>
        </dependency>
        <dependency>
            <groupId>io.airlift</groupId>
            <artifactId>http-server</artifactId>
            <version>${dep.airlift.version}</version>
        </dependency>
        <dependency>
            <groupId>io.airlift</groupId>
            <artifactId>node</artifactId>
            <version>${dep.airlift.version}</version>
        </dependency>
        <dependency>
            <groupId>io.airlift</groupId>
            <artifactId>jaxrs</artifactId>
            <version>${dep.airlift.version}</version>
        </dependency>
        <dependency>
            <groupId>io.airlift</groupId>
            <artifactId>json</artifactId>
            <version>${dep.airlift.version}</version>
        </dependency>
        <dependency>
            <groupId>io.airlift</groupId>
            <artifactId>log</artifactId>
            <version>${dep.airlift.version}</version>
        </dependency>
    </dependencies>
</project>
```

## Step 8: Run Your API

```bash
# Compile
mvn compile

# Run the server
mvn exec:java -Dexec.mainClass="com.example.BookstoreServer"
```

## Step 9: Test Your API

```bash
# Create a book
curl -X POST http://localhost:8080/v1/bookstore/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Building APIs",
    "author": "Jane Developer",
    "isbn": "978-1234567890",
    "year": 2024,
    "price": 49.99
  }'

# Response:
# {
#   "bookId": "1",
#   "syncToken": {"version": 1},
#   "title": "Building APIs",
#   "author": "Jane Developer",
#   "isbn": "978-1234567890",
#   "year": 2024,
#   "price": 49.99
# }

# Get the book
curl http://localhost:8080/v1/bookstore/books/1

# List all books
curl http://localhost:8080/v1/bookstore/books

# Update the book
curl -X PATCH http://localhost:8080/v1/bookstore/books/1 \
  -H "Content-Type: application/json" \
  -d '{"price": 39.99}'

# Delete the book
curl -X DELETE http://localhost:8080/v1/bookstore/books/1
```

## What You Get Automatically

The framework provides all of this without extra code:

1. **Standard URI structure**: `/v{version}/{serviceType}/{resources}/{resourceId}`
2. **HTTP method mapping**: GET, POST, PATCH, DELETE
3. **JSON serialization/deserialization**
4. **Request validation**
5. **OpenAPI documentation**: `GET http://localhost:8080/openapi`
6. **Error responses** with proper status codes
7. **Pagination support** with page tokens
8. **Ordering support** with `orderBy` query parameter

## Summary: The Build Order

When creating a new service, follow this order:

1. **Resource ID** (`BookId`) - Every resource needs an ID
2. **"New" Resource** (`NewBook`) - For creation without ID/version
3. **Complete Resource** (`Book`) - With ID, version, and data
4. **Service Type** (`BookServiceType`) - Defines versioning and grouping
5. **Service** (`BookService`) - Implements the methods
6. **Server Bootstrap** (`BookstoreServer`) - Wires everything together

## Next Steps

- Read [resources.md](resources.md) for advanced resource features
- Read [methods.md](methods.md) for custom methods
- Read [pagination.md](pagination.md) for pagination details
- Read [enforcement.md](enforcement.md) to understand validation rules
- Explore [examples/](examples/) for more complex patterns

## Common Patterns

### Enum Fields
```java
public enum BookGenre { FICTION, NON_FICTION, TECHNICAL }

@ApiResource(name = "book", description = "...")
public record NewBook(
    String title,
    @ApiDescription("Genre of the book") BookGenre genre
) {}
```

### Optional Fields
```java
@ApiResource(name = "book", description = "...")
public record NewBook(
    String title,
    @ApiDescription("Subtitle (optional)") Optional<String> subtitle
) {}
```

### Nested Objects
```java
public record Author(String name, String email) {}

@ApiResource(name = "book", description = "...")
public record NewBook(
    String title,
    @ApiDescription("Book author") Author author
) {}
```

### List Fields
```java
@ApiResource(name = "book", description = "...")
public record NewBook(
    String title,
    @ApiDescription("Tags for categorization") List<String> tags
) {}
```

### Validation Annotations
```java
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@ApiResource(name = "book", description = "...")
public record NewBook(
    @NotBlank String title,
    @Min(1) int year
) {}
```

## Troubleshooting

### "Cannot find symbol: ServiceType"
You need `ApiServiceType`, not `ServiceType`. The service type should implement the `ApiServiceType` interface.

### "Method paginate() not found on ApiPagination"
Don't call `pagination.paginate()` - instead, manually implement pagination and return an `ApiPaginatedResult`.

### "Constructor ambiguity in ApiId"
Use a wrapper type for your internal ID instead of a plain `String` - see the `BookInternalId` pattern in our examples.

### "Resource validation failed"
Make sure your resource has:
- `@ApiResource` annotation
- An ID field extending `ApiId`
- An `ApiResourceVersion syncToken` field
- All required fields are non-null

---

**You're ready to build your first API service!** Check out the [complete working example](examples/) for reference.
