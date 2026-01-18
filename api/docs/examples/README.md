# Airlift API Examples

This directory contains working examples that demonstrate how to use the `io.airlift.api` framework to build RESTful APIs following Google Cloud API design guidelines.

## Prerequisites

- Java 21 or later
- Maven 3.6 or later
- The parent `io.airlift:api` library must be built and installed locally

## Building the Parent Library

Before running the examples, you need to build and install the parent API library:

```bash
# From the api directory (parent of docs/)
cd ..
mvn clean install
```

## Building the Examples

```bash
# From the examples directory
cd examples
mvn clean compile
```

## Running the Examples

### Bookstore Example

The bookstore example demonstrates a complete CRUD API for managing books. It showcases:

- **Resource definitions** (`Book`, `BookData`, `BookId`)
- **Standard methods** (List, Get, Create, Update, Delete)
- **Service implementation** (`BookService`)
- **Pagination and ordering** for list operations
- **Patch-based updates** for partial resource modifications

#### Starting the Server

```bash
mvn compile exec:java -Dexec.mainClass="io.airlift.api.examples.bookstore.BookstoreServer"
```

The server will start on `http://localhost:8080` by default.

#### API Endpoints

Once the server is running, you can interact with these endpoints:

##### List all books
```bash
curl http://localhost:8080/v1/bookstore/books
```

With pagination:
```bash
curl "http://localhost:8080/v1/bookstore/books?pageSize=10&pageToken=abc"
```

With ordering:
```bash
curl "http://localhost:8080/v1/bookstore/books?orderBy=year%20desc"
```

##### Create a book
```bash
curl -X POST http://localhost:8080/v1/bookstore/books \
  -H "Content-Type: application/json" \
  -d '{
    "title": "The Pragmatic Programmer",
    "author": "Hunt & Thomas",
    "isbn": "978-0135957059",
    "year": 2019,
    "price": 49.99
  }'
```

##### Get a book
```bash
curl http://localhost:8080/v1/bookstore/books/1
```

##### Update a book (partial update using PATCH)
```bash
curl -X PATCH http://localhost:8080/v1/bookstore/books/1 \
  -H "Content-Type: application/json" \
  -d '{
    "price": 39.99
  }'
```

##### Delete a book
```bash
curl -X DELETE http://localhost:8080/v1/bookstore/books/1
```

##### View OpenAPI Specification
```bash
curl http://localhost:8080/openapi
```

Or open in a browser: http://localhost:8080/openapi

## Understanding the Code

### Resource Structure

The example uses three main classes for the Book resource:

1. **`BookId`** - Type-safe identifier for books
   - Extends `ApiId<Book, BookInternalId>`
   - Provides serialization/deserialization support

2. **`BookData`** - Book information/data without system metadata
   - Annotated with `@ApiResource`
   - Contains only the domain fields (title, author, etc.)
   - No ID or version - those are system-generated
   - Includes validation in the compact constructor

3. **`Book`** - The complete resource with system metadata
   - Includes `BookId` and `ApiResourceVersion` (syncToken)
   - Uses `@ApiUnwrapped` to include all fields from `BookData`
   - Represents: resource = data + metadata
   - This is what the API returns to clients

### Service Implementation

**`BookService`** demonstrates the standard API methods:

- **`@ApiList`** - List resources with pagination and ordering
- **`@ApiGet`** - Get a single resource by ID
- **`@ApiCreate`** - Create a new resource
- **`@ApiUpdate`** - Update a resource (PATCH semantics)
- **`@ApiDelete`** - Delete a resource

Each method is annotated to indicate its purpose, and the framework automatically:
- Generates appropriate HTTP endpoints
- Handles serialization/deserialization
- Validates requests
- Generates OpenAPI documentation

### Server Bootstrap

**`BookstoreServer`** shows how to bootstrap an API server:

1. Create required Guice modules (Node, HTTP server, JSON, JAX-RS)
2. Build the API module with your service
3. Bootstrap the application with configuration
4. Start listening for requests

## Key Concepts

### Google Cloud API Design Guide

This framework follows the [Google Cloud API Design Guide](https://cloud.google.com/apis/design), which emphasizes:

- **Resource-oriented design** - APIs are built around resources (nouns) rather than actions (verbs)
- **Standard methods** - Use consistent method names (List, Get, Create, Update, Delete)
- **Standard fields** - Resources include standard fields like IDs and version tokens
- **Consistent naming** - URIs and field names follow predictable patterns

### URI Structure

The framework automatically generates URIs following this pattern:
```
/v{version}/{serviceType}/{resources}/{resourceId}
```

For example:
- `/v1/bookstore/books` - List all books
- `/v1/bookstore/books/123` - Get book with ID 123

### Pagination

The `@ApiList` methods automatically support pagination through the `ApiPagination` parameter:
- `pageSize` - Number of items per page
- `pageToken` - Token for fetching the next page

### Ordering

List methods can support ordering through the `ApiOrderBy` parameter:
- Specify allowed fields in the `@ApiParameter` annotation
- Clients use `orderBy=field` or `orderBy=field desc`

### Partial Updates (PATCH)

The `@ApiUpdate` methods use `ApiPatch<T>` to support partial updates:
- Clients send only the fields they want to change
- The framework applies changes to the existing resource
- Version tokens help prevent lost updates

## Next Steps

1. Explore the code in `src/main/java/io/airlift/api/examples/bookstore/`
2. Try modifying the example to add new fields or methods
3. Create your own service following this pattern
4. Read the [full documentation](../) for advanced features

## Advanced Features

The framework supports many additional features not shown in this basic example:

- **Filtering** - Allow clients to filter list results
- **Custom methods** - Define operations beyond standard CRUD
- **Quotas** - Rate limiting and quota management
- **Validation** - Request validation with detailed error messages
- **Streaming** - Stream large responses
- **Multipart uploads** - Handle file uploads
- **Field masks** - Partial responses and updates

See the main documentation for details on these features.
