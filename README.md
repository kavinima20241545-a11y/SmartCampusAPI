# Smart Campus Sensor & Room Management API
**ID NUmber:** 20241545-IIT, w2153619-UoW    
**Module:** 5COSC022W – Client-Server Architectures   
**Base URL:** `http://localhost:8080/api/v1`



## API Overview

This RESTful API manages the university's Smart Campus infrastructure. It provides endpoints for:
- **Rooms** – create, retrieve, and decommission campus rooms
- **Sensors** – register, filter, and monitor IoT sensors within rooms
- **Sensor Readings** – log and retrieve historical sensor data

The API follows REST principles including resource-based URIs, stateless requests, proper HTTP status codes, and HATEOAS-style discovery.


## Build & Run Instructions

### Prerequisites
- MicroSoft-17 SDK
- Maven 



### Run

Server starts at: `http://localhost:8080/api/v1` - intellij Terminal


## API Endpoints Summary

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1` | Discovery – API metadata and resource links |
| GET | `/api/v1/rooms` | List all rooms |
| POST | `/api/v1/rooms` | Create a new room |
| GET | `/api/v1/rooms/{roomId}` | Get a specific room |
| DELETE | `/api/v1/rooms/{roomId}` | Delete a room (blocked if sensors present) |
| GET | `/api/v1/sensors` | List all sensors (supports `?type=` filter) |
| POST | `/api/v1/sensors` | Register a new sensor |
| GET | `/api/v1/sensors/{sensorId}` | Get a specific sensor |
| GET | `/api/v1/sensors/{sensorId}/readings` | Get reading history |
| POST | `/api/v1/sensors/{sensorId}/readings` | Add a new reading |
| GET | `/api/v1/sensors/{sensorId}/readings/{readingId}` | Get specific reading |





## Report – Answers to Coursework Questions

### Part 1.1 - JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of every resource class for each incoming HTTP request** (per-request scope). This is known as the default lifecycle. The runtime does not reuse instances across requests.

**Impact on in-memory data management:** Because each request gets a fresh resource object, any instance variables declared inside resource classes would be lost between requests. To persist state across requests, shared data must live outside the resource class. This project addresses this by using the **Singleton `DataStore` class**, which holds `ConcurrentHashMap` instances. A `ConcurrentHashMap` is thread-safe, meaning multiple concurrent requests can read and write to it simultaneously without corrupting data or creating race conditions. Had we used a plain `HashMap`, concurrent writes could cause lost updates or `ConcurrentModificationException`.



### Part 1.2 - Why HATEOAS is a Hallmark of Advanced REST Design

HATEOAS (Hypermedia as the Engine of Application State) means that API responses embed links to related resources and available actions. For example, a GET /rooms response might include a link to that room's sensors.

**Benefits over static documentation:**
- **Self-discoverability:** Clients do not need to hard-code URLs. They navigate the API by following links embedded in responses, exactly as a web browser navigates HTML pages.
- **Reduced coupling:** If the server changes a URL path, clients that follow links are unaffected, whereas clients relying on documented static paths break.
- **Contextual actions:** Responses can dynamically advertise only the operations currently valid (e.g., no "delete" link if the room has sensors), reducing client-side guard logic.
- **Onboarding speed:** A developer can explore the entire API by starting at the root discovery endpoint and following links, without reading external docs.



### Part 2.1 - Returning IDs vs Full Objects in a List

| Approach | Pros | Cons |
|----------|------|------|
| **IDs only** | Minimal payload, fast transfer | Client must make N follow-up GET requests (N+1 problem) |
| **Full objects** | All data in one round-trip | Larger payload, especially with many rooms |

For the Smart Campus use-case where clients (facilities managers, dashboards) need full room details immediately, returning **full objects** is superior. The bandwidth cost is justified by the elimination of multiple follow-up requests. In a high-latency mobile scenario, IDs-only with pagination might be preferred.



### Part 2.2 - Is DELETE Idempotent in This Implementation?

Technically, **DELETE is not strictly idempotent** in this implementation. Idempotency means repeated identical requests produce the same result. Here:
- **First DELETE on a room:** returns `204 No Content` (room removed).
- **Second DELETE on the same room:** returns `404 Not Found` (already gone).

The *server state* after both calls is identical (the room is absent), which satisfies idempotency in the REST sense. However, the **HTTP response code changes**, so from a strict client perspective it is not fully idempotent. This is an acceptable and common REST trade-off — the resource is gone either way, no additional side effects occur, and no data is corrupted.



### Part 3.1 - Effect of @Consumes Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells JAX-RS that the endpoint only accepts requests with `Content-Type: application/json`. If a client sends data as `text/plain` or `application/xml`, the JAX-RS runtime checks the `Content-Type` header before even invoking the method. Because no matching method is found for that media type, **Jersey automatically returns `415 Unsupported Media Type`** without the resource method ever executing. This protects the API from receiving data it cannot deserialise, and eliminates the need for manual content-type checking inside every method.



### Part 3.2 - @QueryParam vs Path Segment for Filtering

| Design | Example | Use-case |
|--------|---------|----------|
| Query parameter | `GET /sensors?type=CO2` | Filtering/searching a collection |
| Path segment | `GET /sensors/type/CO2` | Identifying a discrete sub-resource |

**Query parameters are superior for filtering** because:
1. **Semantics:** A path segment implies a unique, addressable resource. `/sensors/type/CO2` suggests "CO2" is a resource ID, not a filter value.
2. **Composability:** Multiple filters are natural with query params (`?type=CO2&status=ACTIVE`), but chaining path segments becomes unwieldy and non-standard.
3. **REST convention:** Path identifies the resource; query string refines or filters the collection. This is the universal convention followed by major public APIs (GitHub, Stripe, etc.).
4. **Optional by design:** `@QueryParam` is null when omitted, allowing the same method to serve both filtered and unfiltered requests cleanly.



### Part 4.1 - Benefits of the Sub-Resource Locator Pattern

The sub-resource locator pattern (returning an instance of a delegate class from a `@Path` method) separates concerns by giving each nested resource its own class. Benefits include:

- **Separation of concerns:** `SensorResource` manages sensors; `SensorReadingResource` manages readings. Each class has a single responsibility.
- **Manageable class size:** A single "mega-controller" with all paths (sensors, readings, filtering) would be hundreds of lines and hard to maintain or test.
- **Independent testability:** Each resource class can be unit-tested in isolation, injected with mock data.
- **Context injection:** The parent can pass context (e.g., `sensorId`) to the sub-resource via its constructor, so the sub-resource always operates in the right scope.
- **Scalability:** Adding new sub-resources (e.g., `/sensors/{id}/alerts`) means creating a new class, not modifying an existing one (Open/Closed principle).



### Part 5.2 - Why 422 is More Accurate Than 404 for a Missing Reference

- **404 Not Found** means the requested URL/resource itself does not exist on the server.
- **422 Unprocessable Entity** means the request URL is valid and the server understood the request, but the **payload content is semantically invalid** — in this case, the `roomId` field references a room that does not exist.

Using 404 would be misleading: the `/api/v1/sensors` endpoint clearly exists. The problem is not a missing URL but a broken foreign-key reference inside a valid JSON body. HTTP 422 communicates precisely this: "I understood your JSON, but I cannot process it because a referenced entity is absent." This helps client developers distinguish between "wrong URL" and "invalid data reference."



### Part 5.4 - Security Risks of Exposing Java Stack Traces

Exposing raw stack traces to API consumers is a significant security risk:

1. **Technology fingerprinting:** Stack traces reveal the framework (Jersey, Grizzly), Java version, and internal package names, allowing attackers to target known CVEs for those specific versions.
2. **Internal architecture exposure:** Class names, method signatures, and file line numbers reveal the application's internal structure, making it easier to craft targeted attacks.
3. **Data exposure:** Stack traces can accidentally include variable values that contain sensitive data (e.g., database connection strings, user input, configuration values).
4. **Attack surface mapping:** Error messages like `NullPointerException at DataStore.java:47` tell an attacker exactly which code path to probe further.

The `GlobalExceptionMapper` in this project logs the full trace **server-side** for debugging while returning only a generic message to the client, following the principle of minimum information disclosure.



### Part 5.5 - Why Filters are Better than Manual Logger Calls

Using JAX-RS filters for cross-cutting concerns like logging has significant advantages over placing `Logger.info()` inside every resource method:

1. **DRY (Don't Repeat Yourself):** One filter class handles all endpoints. Adding a new resource automatically gets logging without any extra code.
2. **Consistency:** Manual logging in 20 methods risks some being forgotten or formatted differently. A filter guarantees uniform log format for every request.
3. **Separation of concerns:** Business logic (sensor management) is not polluted with infrastructure concerns (logging). Resource methods stay focused and readable.
4. **Easy to modify:** Changing the log format (e.g., adding a request ID) requires editing one class, not hunting through every resource file.
5. **Interceptability:** Filters can access both request and response contexts, enabling correlation between what came in and what went out — impossible with method-level logging.
