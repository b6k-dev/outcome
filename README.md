# Outcome

Lightweight `Result` type for Java with `Ok`, `Err`, and `Panic`.

Inspired by [better-result](https://github.com/dmmulroy/better-result).

## Contents

- [The Core Types](#the-core-types)
- [Creating Results](#creating-results)
- [Transforming Success Values](#transforming-success-values)
- [Transforming and Recovering Errors](#transforming-and-recovering-errors)
- [Extracting Values](#extracting-values)
- [Observing Without Changing](#observing-without-changing)
- [Folding Results](#folding-results)
- [Modeling Errors with Sealed Types](#modeling-errors-with-sealed-types)
- [Panic](#panic)
- [API Reference](#api-reference)

## The Core Types

Outcome revolves around four ideas:

- `Result<T, E>`: a value that is either success (`T`) or failure (`E`)
- `Result.Ok<T, E>`: the success variant
- `Result.Err<T, E>`: the error variant
- `Panic`: an unrecoverable programming error thrown by the library

```java
// A result holds either a success value or an error value
Result<User, LoginError> login = authenticate(username, password);

// Ok means success
Result<Integer, String> ok = Result.ok(42);
if (ok.isOk()) {
    System.out.println(ok.unwrap());
}

// Err means an expected domain failure
Result<Integer, String> err = Result.err("missing value");
if (err.isErr()) {
    System.out.println(err.unwrapError());
}

// Panic means a bug or invalid API usage
Result<Integer, String> failed = Result.err("boom");
failed.unwrap(); // throws Panic

// Result itself is a sealed sum type, so you can pattern match it directly
String message = switch (login) {
    case Result.Ok(var user) -> "Welcome, " + user.displayName();
    case Result.Err(var error) -> "Login failed: " + error;
};
```

`Err` is for expected failures. `Panic` is for bugs such as unwrapping the wrong variant or throwing from callbacks passed into Result operations.

## Creating Results

```java
// Create success directly
Result<Integer, String> ok = Result.ok(8080);

// Create error directly
Result<Integer, String> err = Result.err("PORT is missing");

// Wrap throwing code
Result<Integer, Throwable> parsed = Result.trying(() -> Integer.parseInt(input));

// Map thrown exceptions to your own error type
sealed interface ParseError permits MissingInput, InvalidIntegerFormat {}
record MissingInput() implements ParseError {}
record InvalidIntegerFormat(String input) implements ParseError {}

Result<Integer, ParseError> typed = Result.fromNullable(input, MissingInput::new)
        .flatMap(value -> Result.trying(
                () -> Integer.parseInt(value),
                error -> new InvalidIntegerFormat(value)
        ));

// Convert nullable values
Result<User, String> userFromNullable = Result.fromNullable(
        repository.findById(id),
        () -> "user not found"
);

// Convert Optional values
Result<User, String> userFromOptional = Result.fromOptional(
        repository.findOptionalById(id),
        () -> "user not found"
);
```

## Transforming Success Values

```java
// map transforms the Ok value
Result<Integer, String> doubled = Result.ok(21)
        .map(value -> value * 2);
// Ok(42)

// Errors pass through unchanged
Result<Integer, String> stillErr = Result.<Integer, String>err("bad input")
        .map(value -> value * 2);
// Err("bad input")

// flatMap chains another Result-returning operation
Result<Integer, String> result = Result.ok("42")
        .flatMap(text -> Result.trying(() -> Integer.parseInt(text), Throwable::getMessage))
        .flatMap(number -> number > 0
                ? Result.ok(number)
                : Result.err("number must be positive"));

// map is for T -> U
// flatMap is for T -> Result<U, E>

// Practical validation pipeline
Result<String, String> readPort() {
    return Result.fromNullable(System.getenv("PORT"), () -> "PORT is missing");
}

Result<Integer, String> parsePort(String text) {
    return Result.trying(() -> Integer.parseInt(text), Throwable::getMessage);
}

Result<Integer, String> validatePort(int port) {
    return port >= 1 && port <= 65535
            ? Result.ok(port)
            : Result.err("port must be between 1 and 65535");
}

Result<Integer, String> port = readPort()
        .flatMap(OutcomeExamples::parsePort)
        .flatMap(OutcomeExamples::validatePort);
```

## Transforming and Recovering Errors

```java
// mapError converts one error type into another
Result<User, Throwable> fromTry = Result.trying(() -> service.loadUser(id));
Result<User, String> readable = fromTry.mapError(Throwable::getMessage);

// Useful at boundaries
Result<User, DatabaseError> fromDb = loadUser(id);
Result<User, ApiError> apiResult = fromDb.mapError(error ->
        new ApiError("Could not load user", error)
);

// orElse recovers by returning another Result
Result<User, String> user = loadUserFromCache(id)
        .orElse(error -> loadUserFromDatabase(id));

// Recover into success
Result<String, String> config = Result.<String, String>err("config missing")
        .orElse(error -> Result.ok("default-config"));

// Or change the error type while staying in Err
Result<String, IllegalStateException> typedError = Result.<String, String>err("config missing")
        .orElse(error -> Result.err(new IllegalStateException(error)));
```

## Extracting Values

```java
// Unwrap success or throw Panic
int value = Result.ok(42).unwrap();

// Unwrap with a custom panic message
User user = maybeUser.unwrap("expected authenticated user here");

// Read the error value
String error = Result.<Integer, String>err("boom").unwrapError();

// Fallback value
int port = loadPort().unwrapOr(8080);

// Lazy fallback
int legacyPort = loadPort().unwrapOrElse(() -> readPortFromLegacyConfig());

// Derive fallback from the error
int statusCode = parseStatus(raw).unwrapOrElse(errorValue -> switch (errorValue) {
    case "missing" -> 400;
    case "invalid" -> 422;
    default -> 500;
});

// Convert an error into an exception
User loaded = loadUser(id).orThrow(errorValue -> new IllegalStateException(errorValue));

// Useful when your internals use Result but your framework boundary expects exceptions
UserControllerResponse handle(String id) {
    User resolved = loadUser(id).orThrow(UserNotFoundException::new);
    return UserControllerResponse.from(resolved);
}
```

## Observing Without Changing

```java
// peek runs only for Ok
Result<Integer, String> parsed = parse(input)
        .peek(value -> logger.info("parsed value: {}", value));

// peekErr runs only for Err
Result<Integer, String> logged = parse(input)
        .peekErr(error -> logger.warn("could not parse input: {}", error));

// tap runs for both Ok and Err
Result<Integer, String> metered = parse(input)
        .tap(() -> metrics.increment("parse.attempt"));

// These methods return the original result unchanged
Result<Integer, String> pipeline = parse(input)
        .peek(value -> logger.info("value={}", value))
        .peekErr(error -> logger.warn("error={}", error))
        .tap(() -> metrics.increment("parse.total"));
```

## Folding Results

```java
// fold collapses both branches into one final value
String message = loadUser(id).fold(
        user -> "Hello, " + user.displayName(),
        error -> "Could not load user: " + error
);

// Great for HTTP-ish boundaries
int status = saveUser(input).fold(
        user -> 201,
        error -> 400
);

// Also useful for rendering or logging
String line = parsePort(raw).fold(
        port -> "Listening on " + port,
        error -> "Startup failed: " + error
);
```

## Modeling Errors with Sealed Types

For non-trivial code, define a proper error sum type instead of using raw strings everywhere.

```java
sealed interface LoginError permits UserNotFound, InvalidPassword, AccountLocked {}

record UserNotFound(String username) implements LoginError {}
record InvalidPassword() implements LoginError {}
record AccountLocked(int remainingSeconds) implements LoginError {}

// Return the sealed error type from your domain code
Result<User, LoginError> result = login(username, password);

// Pattern match with native Java switch
String message = result.fold(
        user -> "Welcome, " + user.displayName(),
        error -> switch (error) {
            case UserNotFound e -> "No user named " + e.username();
            case InvalidPassword e -> "Password is incorrect";
            case AccountLocked e -> "Try again in " + e.remainingSeconds() + " seconds";
        }
);

// You can also pattern match on Result itself with record patterns
String auditLine = switch (result) {
    case Result.Ok(var user) -> "login ok for " + user.displayName();
    case Result.Err(var error) -> switch (error) {
        case UserNotFound e -> "login failed: unknown user " + e.username();
        case InvalidPassword e -> "login failed: invalid password";
        case AccountLocked e -> "login failed: locked for " + e.remainingSeconds() + "s";
    };
};

// Another practical example for API responses
sealed interface CreateUserError permits InvalidEmail, DuplicateEmail, WeakPassword {}
record InvalidEmail(String value) implements CreateUserError {}
record DuplicateEmail(String value) implements CreateUserError {}
record WeakPassword() implements CreateUserError {}

int status = createUser(request).fold(
        user -> 201,
        error -> switch (error) {
            case InvalidEmail e -> 400;
            case DuplicateEmail e -> 409;
            case WeakPassword e -> 422;
        }
);
```

This keeps errors explicit, typed, and exhaustively handled.

## Panic

`Panic` is thrown, not returned. It represents a programming defect rather than a domain error.

```java
// Unwrapping the wrong variant panics
Result.err("bad").unwrap();

// Throwing inside a mapper panics
Result.ok(1).map(value -> {
    throw new IllegalStateException("bug in mapper");
});

// Throwing inside observers panics
Result.ok(1).peek(value -> {
    throw new IllegalStateException("logging callback exploded");
});

// You can catch Panic for reporting
try {
    Result.ok(1).map(value -> {
        throw new IllegalStateException("bug");
    });
} catch (Panic panic) {
    System.err.println(panic.getMessage());
    System.err.println(panic.getCause().getMessage());
}
```

## API Reference

### Static factory methods

| Method | Description |
| --- | --- |
| `Result.ok(value)` | Create an `Ok` |
| `Result.err(error)` | Create an `Err` |
| `Result.trying(supplier)` | Capture a thrown `Throwable` as `Err` |
| `Result.trying(supplier, errorMapper)` | Capture an exception and map it to your error type |
| `Result.fromNullable(value, onNullSupplier)` | Convert a nullable reference into `Result` |
| `Result.fromOptional(optional, onEmptySupplier)` | Convert an `Optional` into `Result` |

### Instance methods

| Method | Description |
| --- | --- |
| `isOk()` | `true` when this result is `Ok` |
| `isErr()` | `true` when this result is `Err` |
| `map(fn)` | Transform the success value |
| `mapError(fn)` | Transform the error value |
| `flatMap(fn)` | Chain a function that returns `Result` |
| `orElse(fn)` | Recover from an error with another `Result` |
| `peek(fn)` | Observe success values |
| `peekErr(fn)` | Observe error values |
| `tap(fn)` | Run a side effect for both branches |
| `unwrap()` | Return the success value or throw `Panic` |
| `unwrap(message)` | Return the success value or throw `Panic` with a custom message |
| `unwrapError()` | Return the error value or throw `Panic` |
| `unwrapOr(value)` | Return the success value or a fallback |
| `unwrapOrElse(supplier)` | Lazily compute a fallback |
| `unwrapOrElse(errorMapper)` | Compute a fallback from the error |
| `orThrow(errorMapper)` | Throw an exception derived from the error |
| `fold(okMapper, errorMapper)` | Collapse both branches into one value |
