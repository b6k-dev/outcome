# Outcome

Lightweight `Result` type for Java with `Ok`, `Err`, and `Panic`.

Inspired by [better-result](https://github.com/dmmulroy/better-result).

## Contents

- [Installation](#installation)
- [Java Compatibility](#java-compatibility)
- [Quick Start](#quick-start)
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

## Installation

Maven:

```xml
<dependency>
    <groupId>io.github.b6k-dev</groupId>
    <artifactId>outcome</artifactId>
    <version>1.0.0</version>
</dependency>
```

Gradle (Groovy DSL):

```groovy
implementation 'io.github.b6k-dev:outcome:1.0.0'
```

Gradle (Kotlin DSL):

```kotlin
implementation("io.github.b6k-dev:outcome:1.0.0")
```

## Java Compatibility

Outcome targets Java 17 and newer. The library API is compiled with `--release 17`, and examples in this README use sealed types, records, and pattern matching features available in modern Java.

## Quick Start

```java
import b6k.dev.outcome.Result;

sealed interface CreateUserError permits InvalidEmail, DuplicateEmail {}
record InvalidEmail(String value) implements CreateUserError {}
record DuplicateEmail(String value) implements CreateUserError {}
record User(String id, String email) {}

Result<User, CreateUserError> createUser(String email) {
    if (email == null || email.isBlank() || !email.contains("@")) {
        return Result.err(new InvalidEmail(String.valueOf(email)));
    }
    if (email.equals("ada@example.com")) {
        return Result.err(new DuplicateEmail(email));
    }
    return Result.ok(new User("u-123", email));
}

String message = createUser("ada@example.com").fold(
        user -> "Created user " + user.email(),
        error -> switch (error) {
            case InvalidEmail e -> "Invalid email: " + e.value();
            case DuplicateEmail e -> "Email already exists: " + e.email();
        }
);
```

Use `Result.ok(...)` and `Result.err(...)` to model expected outcomes, then compose them with methods like `map`, `flatMap`, `orElse`, and `fold` instead of mixing nullable values, sentinel states, and exceptions.

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
Result<User, LoginError> ok = Result.ok(new User("u-123", "Ada"));
if (ok.isOk()) {
    System.out.println(ok.unwrap().displayName());
}

// Err means an expected domain failure
Result<User, LoginError> err = Result.err(new InvalidPassword());
if (err.isErr()) {
    System.out.println(err.unwrapError());
}

// Panic means a bug or invalid API usage
Result<User, LoginError> failed = Result.err(new AccountLocked(300));
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
Result<User, LoginError> ok = Result.ok(new User("u-123", "Ada"));

// Create error directly
Result<User, LoginError> err = Result.err(new InvalidPassword());

// Wrap throwing code
Result<UserId, Throwable> parsed = Result.trying(() -> UserId.parse(rawUserId));

// Map thrown exceptions to your own error type
sealed interface UserIdError permits MissingUserId, InvalidUserId {}
record MissingUserId() implements UserIdError {}
record InvalidUserId(String input) implements UserIdError {}

Result<UserId, UserIdError> typed = Result.fromNullable(rawUserId, MissingUserId::new)
        .flatMap(value -> Result.trying(
                () -> UserId.parse(value),
                error -> new InvalidUserId(value)
        ));

// Convert nullable values
Result<User, LoginError> userFromNullable = Result.fromNullable(
        repository.findById(id),
        () -> new UserNotFound(id)
);

// Convert Optional values
Result<User, LoginError> userFromOptional = Result.fromOptional(
        repository.findOptionalById(id),
        () -> new UserNotFound(id)
);
```

## Transforming Success Values

```java
// map transforms the Ok value
Result<String, LoginError> greeting = Result.ok(new User("u-123", "Ada"))
        .map(user -> "Welcome back, " + user.displayName());
// Ok("Welcome back, Ada")

// Errors pass through unchanged
Result<String, LoginError> stillErr = Result.<User, LoginError>err(new InvalidPassword())
        .map(user -> "Welcome back, " + user.displayName());
// Err(new InvalidPassword())

// flatMap chains another Result-returning operation
Result<Session, LoginError> result = findUser(username)
        .flatMap(user -> verifyPassword(user, password))
        .flatMap(user -> createSession(user));

// map is for T -> U
// flatMap is for T -> Result<U, E>

// Practical authentication pipeline
Result<User, LoginError> findUser(String username) {
    return Result.fromOptional(userRepository.findByUsername(username), () -> new UserNotFound(username));
}

Result<User, LoginError> verifyPassword(User user, String password) {
    return passwordHasher.matches(password, user.passwordHash())
            ? Result.ok(user)
            : Result.err(new InvalidPassword());
}

Result<Session, LoginError> createSession(User user) {
    return user.locked()
            ? Result.err(new AccountLocked(user.lockedForSeconds()))
            : Result.ok(sessionService.createFor(user));
}

Result<Session, LoginError> session = findUser(username)
        .flatMap(user -> verifyPassword(user, password))
        .flatMap(OutcomeExamples::createSession);
```

## Transforming and Recovering Errors

```java
// mapError converts one error type into another
Result<User, Throwable> fromTry = Result.trying(() -> userService.load(id));
Result<User, UserQueryError> readable = fromTry.mapError(error -> new UserQueryFailed(id, error));

// Useful at boundaries
Result<User, UserQueryError> fromDb = loadUser(id);
Result<User, GetUserResponseError> apiResult = fromDb.mapError(error ->
        new GetUserResponseError("Could not load user", error)
);

// orElse recovers by returning another Result
Result<User, UserQueryError> user = loadUserFromCache(id)
        .orElse(error -> loadUserFromDatabase(id));

// Recover into success
Result<UserPreferences, UserQueryError> preferences = loadUserPreferences(userId)
        .orElse(error -> Result.ok(UserPreferences.defaultFor(userId)));

// Or change the error type while staying in Err
Result<User, IllegalStateException> typedError = loadUser(id)
        .orElse(error -> Result.err(new IllegalStateException(error.toString())));
```

## Extracting Values

```java
// Unwrap success or throw Panic
User user = Result.ok(new User("u-123", "Ada")).unwrap();

// Unwrap with a custom panic message
User currentUser = maybeUser.unwrap("expected authenticated user here");

// Read the error value
LoginError error = Result.<User, LoginError>err(new InvalidPassword()).unwrapError();

// Fallback value
UserPreferences preferences = loadUserPreferences(userId)
        .unwrapOr(UserPreferences.defaultFor(userId));

// Lazy fallback
UserPreferences lazyPreferences = loadUserPreferences(userId)
        .unwrapOrElse(() -> UserPreferences.defaultFor(userId));

// Derive fallback from the error
GetUserResponse response = loadUser(id).unwrapOrElse(errorValue -> switch (errorValue) {
    case UserNotFound e -> GetUserResponse.notFound(e.username());
    case InvalidPassword e -> GetUserResponse.unauthorized();
    case AccountLocked e -> GetUserResponse.locked(e.remainingSeconds());
});

// Convert an error into an exception
User loaded = loadUser(id).orThrow(errorValue -> new IllegalStateException(errorValue.toString()));

// Useful when your internals use Result but your framework boundary expects exceptions
UserControllerResponse handle(String id) {
    User resolved = loadUser(id).orThrow(UserNotFoundException::new);
    return UserControllerResponse.from(resolved);
}
```

## Observing Without Changing

```java
// peek runs only for Ok
Result<User, LoginError> authenticated = authenticate(username, password)
        .peek(user -> logger.info("login ok for {}", user.displayName()));

// peekErr runs only for Err
Result<User, LoginError> logged = authenticate(username, password)
        .peekErr(error -> logger.warn("login failed: {}", error));

// tap runs for both Ok and Err
Result<User, LoginError> metered = authenticate(username, password)
        .tap(() -> metrics.increment("login.attempt"));

// These methods return the original result unchanged
Result<User, LoginError> pipeline = authenticate(username, password)
        .peek(user -> logger.info("user={}", user.id()))
        .peekErr(error -> logger.warn("error={}", error))
        .tap(() -> metrics.increment("login.total"));
```

## Folding Results

```java
// fold collapses both branches into one final value
String message = authenticate(username, password).fold(
        user -> "Welcome, " + user.displayName(),
        error -> "Login failed: " + error
);

// Great for HTTP-ish boundaries
int status = authenticate(username, password).fold(
        user -> 200,
        error -> switch (error) {
            case UserNotFound e -> 404;
            case InvalidPassword e -> 401;
            case AccountLocked e -> 423;
        }
);

// Also useful for rendering or logging
String line = loadUser(id).fold(
        user -> "Loaded user " + user.displayName(),
        error -> "Could not load user: " + error
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

| Method                                           | Description                                        |
|--------------------------------------------------|----------------------------------------------------|
| `Result.ok(value)`                               | Create an `Ok`                                     |
| `Result.err(error)`                              | Create an `Err`                                    |
| `Result.trying(supplier)`                        | Capture a thrown `Throwable` as `Err`              |
| `Result.trying(supplier, errorMapper)`           | Capture an exception and map it to your error type |
| `Result.fromNullable(value, onNullSupplier)`     | Convert a nullable reference into `Result`         |
| `Result.fromOptional(optional, onEmptySupplier)` | Convert an `Optional` into `Result`                |

### Instance methods

| Method                        | Description                                                     |
|-------------------------------|-----------------------------------------------------------------|
| `isOk()`                      | `true` when this result is `Ok`                                 |
| `isErr()`                     | `true` when this result is `Err`                                |
| `map(fn)`                     | Transform the success value                                     |
| `mapError(fn)`                | Transform the error value                                       |
| `flatMap(fn)`                 | Chain a function that returns `Result`                          |
| `orElse(fn)`                  | Recover from an error with another `Result`                     |
| `peek(fn)`                    | Observe success values                                          |
| `peekErr(fn)`                 | Observe error values                                            |
| `tap(fn)`                     | Run a side effect for both branches                             |
| `unwrap()`                    | Return the success value or throw `Panic`                       |
| `unwrap(message)`             | Return the success value or throw `Panic` with a custom message |
| `unwrapError()`               | Return the error value or throw `Panic`                         |
| `unwrapOr(value)`             | Return the success value or a fallback                          |
| `unwrapOrElse(supplier)`      | Lazily compute a fallback                                       |
| `unwrapOrElse(errorMapper)`   | Compute a fallback from the error                               |
| `orThrow(errorMapper)`        | Throw an exception derived from the error                       |
| `fold(okMapper, errorMapper)` | Collapse both branches into one value                           |
