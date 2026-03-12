package b6k.dev.outcome;

import b6k.dev.outcome.error.Panic;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    private static final int OK_VALUE = 45;
    private static final String ERR_VALUE = "oops";

    private Result<Integer, String> okResult() {
        return Result.ok(OK_VALUE);
    }

    private Result<Integer, String> errResult() {
        return Result.err(ERR_VALUE);
    }

    @Nested
    class FactoryMethods {
        @Test
        void createOkWithValue() {
            var result = Result.ok(12);

            assertTrue(result.isOk());
            assertFalse(result.isErr());
        }

        @Test
        void createErrWithValue() {
            var result = Result.err("oops");

            assertTrue(result.isErr());
            assertFalse(result.isOk());
        }

        @Test
        void createOkFromNonNullValue() {
            var called = new AtomicBoolean(false);

            var result = Result.fromNullable(OK_VALUE, () -> {
                called.set(true);
                return ERR_VALUE;
            });

            assertEquals(Result.ok(OK_VALUE), result);
            assertFalse(called.get());
        }

        @Test
        void createErrFromNullValue() {
            var result = Result.fromNullable(null, () -> ERR_VALUE);

            assertEquals(Result.err(ERR_VALUE), result);
        }

        @Test
        void createOkFromPresentOptional() {
            var called = new AtomicBoolean(false);

            var result = Result.fromOptional(Optional.of(OK_VALUE), () -> {
                called.set(true);
                return ERR_VALUE;
            });

            assertEquals(Result.ok(OK_VALUE), result);
            assertFalse(called.get());
        }

        @Test
        void createErrFromEmptyOptional() {
            var result = Result.fromOptional(Optional.empty(), () -> ERR_VALUE);

            assertEquals(Result.err(ERR_VALUE), result);
        }

        @Test
        void acceptsSuppliersReturningSubtypeForFactoryMethods() {
            Supplier<StringBuilder> onMissing = () -> new StringBuilder(ERR_VALUE.toUpperCase());

            Result<Integer, CharSequence> fromNullable = Result.fromNullable(null, onMissing);
            Result<Integer, CharSequence> fromOptional = Result.fromOptional(Optional.empty(), onMissing);

            assertEquals(ERR_VALUE.toUpperCase(), fromNullable.unwrapError().toString());
            assertEquals(ERR_VALUE.toUpperCase(), fromOptional.unwrapError().toString());
        }
    }

    @Nested
    class Trying {
        @Test
        void returnsOkWhenSupplierSucceeds() {
            var result = Result.trying(() -> OK_VALUE);

            assertEquals(Result.ok(OK_VALUE), result);
        }

        @Test
        void returnsErrWhenSupplierThrows() {
            var error = new IllegalStateException("boom");

            var result = Result.trying(() -> {
                throw error;
            });

            assertEquals(Result.err(error), result);
        }

        @Test
        void mapsThrownErrorWhenErrorMapperProvided() {
            var result = Result.trying(
                    () -> {
                        throw new IllegalStateException("boom");
                    },
                    Throwable::getMessage
            );

            assertEquals(Result.err("boom"), result);
        }

        @Test
        void preservesValueWhenErrorMapperProvidedAndSupplierSucceeds() {
            var result = Result.trying(() -> OK_VALUE, Throwable::getMessage);

            assertEquals(Result.ok(OK_VALUE), result);
        }

        @Test
        void acceptsMapperWithBroaderInputAndNarrowerOutput() {
            Function<Object, StringBuilder> errorMapper = error ->
                    new StringBuilder(((Throwable) error).getMessage().toUpperCase());

            Result<Integer, CharSequence> result = Result.trying(
                    () -> {
                        throw new IllegalArgumentException(ERR_VALUE);
                    },
                    errorMapper
            );

            assertEquals(ERR_VALUE.toUpperCase(), result.unwrapError().toString());
        }
    }

    @Nested
    class Unwrap {
        @Test
        void returnsValueForOk() {
            assertEquals(OK_VALUE, okResult().unwrap());
        }

        @Test
        void ignoresMessageForOk() {
            assertEquals(OK_VALUE, okResult().unwrap("blah, blah"));
        }

        @Test
        void throwsWithDefaultMessageForErr() {
            var panic = assertThrows(Panic.class, () -> errResult().unwrap());

            assertEquals("Tried to unwrap an error", panic.getMessage());
        }

        @Test
        void throwsWithCustomMessageForErr() {
            var message = "blah, blah";

            var panic = assertThrows(Panic.class, () -> errResult().unwrap(message));

            assertEquals(message, panic.getMessage());
        }
    }

    @Nested
    class UnwrapError {
        @Test
        void throwsForOk() {
            assertThatThrownBy(() -> okResult().unwrapError()).isInstanceOf(Panic.class)
                    .hasMessage("Tried to unwrap error from an ok value");
        }

        @Test
        void returnsErrorForErr() {
            assertThat(errResult().unwrapError()).isEqualTo(ERR_VALUE);
        }
    }

    @Nested
    class UnwrapOr {
        @Test
        void returnsValueForOk() {
            assertEquals(OK_VALUE, okResult().unwrapOr(23));
        }

        @Test
        void returnsDefaultForErr() {
            var fallback = 23;

            assertEquals(fallback, errResult().unwrapOr(fallback));
        }
    }

    @Nested
    class UnwrapOrElse {
        @Test
        void returnsValueForOk() {
            assertEquals(OK_VALUE, okResult().unwrapOrElse(() -> 44));
        }

        @Test
        void returnsDefaultForErr() {
            var fallback = 41;

            assertEquals(fallback, errResult().unwrapOrElse(() -> fallback));
        }

        @Test
        void returnsValueForOkWhenUsingErrorMapper() {
            var called = new AtomicBoolean(false);

            var result = okResult().unwrapOrElse(error -> {
                called.set(true);
                return error.length();
            });

            assertEquals(OK_VALUE, result);
            assertFalse(called.get());
        }

        @Test
        void mapsErrorForErrWhenUsingErrorMapper() {
            var result = errResult().unwrapOrElse(String::length);

            assertEquals(ERR_VALUE.length(), result);
        }

        @Test
        void acceptsErrorMapperWithBroaderInputAndNarrowerOutput() {
            Result<Number, String> result = Result.err(ERR_VALUE);
            Function<Object, Long> mapper = value -> (long) value.toString().length();

            Number unwrapped = result.unwrapOrElse(mapper);

            assertEquals(ERR_VALUE.length(), unwrapped.longValue());
        }
    }

    @Nested
    class OrElse {
        @Test
        void preservesValueForOk() {
            var called = new AtomicBoolean(false);

            var result = okResult().orElse(_ -> {
                called.set(true);
                return Result.ok(23);
            });

            assertEquals(OK_VALUE, result.unwrap());
            assertFalse(called.get());
        }

        @Test
        void allowsChangingErrorTypeForOk() {
            Result<Integer, IllegalStateException> result =
                    okResult().orElse(error -> Result.err(new IllegalStateException(error)));

            assertEquals(OK_VALUE, result.unwrap());
        }

        @Test
        void recoversErrToOk() {
            var result = errResult().orElse(error -> Result.ok(error.length()));

            assertEquals(ERR_VALUE.length(), result.unwrap());
        }

        @Test
        void transformsErrToDifferentErrorType() {
            Result<Integer, IllegalStateException> result = errResult().orElse(_ -> Result.err(new IllegalStateException("new error")));

            assertThat(result.unwrapError())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("new error");
        }

        @Test
        void acceptsRecoveryWithBroaderInputAndNarrowerOutput() {
            Result<Integer, String> result = errResult();
            Function<Object, Result<Integer, CharSequence>> recovery =
                    value -> Result.err(new StringBuilder(value.toString().toUpperCase()));

            var recovered = result.orElse(recovery);

            assertEquals(ERR_VALUE.toUpperCase(), recovered.unwrapError().toString());
        }
    }

    @Nested
    class OrThrow {
        @Test
        void returnsValueForOk() throws Exception {
            var called = new AtomicBoolean(false);

            var result = okResult().orThrow(error -> {
                called.set(true);
                return new Exception(error);
            });

            assertEquals(OK_VALUE, result);
            assertFalse(called.get());
        }

        @Test
        void throwsMappedExceptionForErr() {
            assertThatThrownBy(() -> errResult().orThrow(IllegalStateException::new))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(ERR_VALUE);
        }

        @Test
        void acceptsMapperWithBroaderInputAndNarrowerOutput() {
            Result<Integer, String> result = errResult();
            Function<Object, IllegalArgumentException> mapper = value ->
                    new IllegalArgumentException(value.toString().toUpperCase());

            assertThatThrownBy(() -> result.orThrow(mapper))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ERR_VALUE.toUpperCase());
        }
    }

    @Nested
    class Map {
        @Test
        void transformsValueForOk() {
            var result = Result.ok(3).map(it -> it * 3);

            assertEquals(9, result.unwrap());
        }

        @Test
        void acceptsMapperWithBroaderInputAndNarrowerOutput() {
            Result<Integer, String> result = okResult();
            Function<Number, StringBuilder> mapper = value -> new StringBuilder(value.toString());

            Result<CharSequence, String> mapped = result.map(mapper);

            assertEquals("45", mapped.unwrap().toString());
        }

        @Test
        void preservesErrorForErr() {
            var result = errResult().map(it -> it * 3);

            assertTrue(result.isErr());
        }
    }

    @Nested
    class MapError {
        @Test
        void preservesValueForOk() {
            var result = okResult().mapError(String::toUpperCase);

            assertEquals(OK_VALUE, result.unwrap());
        }

        @Test
        void transformsErrorForErr() {
            var result = errResult().mapError(String::toUpperCase);

            assertEquals(ERR_VALUE.toUpperCase(), result.unwrapError());
        }

        @Test
        void acceptsMapperWithBroaderInputAndNarrowerOutput() {
            Result<Integer, String> result = errResult();
            Function<Object, StringBuilder> mapper = value -> new StringBuilder(value.toString().toUpperCase());

            Result<Integer, CharSequence> mapped = result.mapError(mapper);

            assertEquals(ERR_VALUE.toUpperCase(), mapped.unwrapError().toString());
        }
    }

    @Nested
    class FlatMap {
        @Test
        void transformsAndFlattensValueForOk() {
            var result = Result.ok(3).flatMap(it -> Result.ok(it * 3));

            assertEquals(9, result.unwrap());
        }

        @Test
        void preservesErrorForErr() {
            var result = errResult().flatMap(it -> Result.ok(it * 3));

            assertTrue(result.isErr());
            assertEquals(ERR_VALUE, result.unwrapError());
        }

        @Test
        void propagatesErrorFromMapper() {
            var result = okResult().flatMap(_ -> Result.err("mapper failed"));

            assertTrue(result.isErr());
            assertEquals("mapper failed", result.unwrapError());
        }

        @Test
        void flattensNestedResultWithIdentityMapper() {
            Result<Result<Integer, String>, String> nested = Result.ok(okResult());

            var flattened = nested.flatMap(Function.identity());

            assertEquals(OK_VALUE, flattened.unwrap());
        }

        @Test
        void acceptsMapperWithBroaderInputAndNarrowerOutput() {
            Result<Integer, String> result = okResult();
            Function<Number, Result<CharSequence, String>> mapper =
                    value -> Result.ok(new StringBuilder(value.toString()));

            Result<CharSequence, String> mapped = result.flatMap(mapper);

            assertEquals("45", mapped.unwrap().toString());
        }
    }

    @Nested
    class Fold {
        @Test
        void mapsOkWithOkMapper() {
            var okCalled = new AtomicBoolean(false);
            var errCalled = new AtomicBoolean(false);

            var result = okResult().fold(value -> {
                okCalled.set(true);
                return value * 2;
            }, error -> {
                errCalled.set(true);
                return error.length();
            });

            assertEquals(OK_VALUE * 2, result);
            assertTrue(okCalled.get());
            assertFalse(errCalled.get());
        }

        @Test
        void mapsErrWithErrorMapper() {
            var okCalled = new AtomicBoolean(false);
            var errCalled = new AtomicBoolean(false);

            var result = errResult().fold(value -> {
                okCalled.set(true);
                return value * 2;
            }, error -> {
                errCalled.set(true);
                return error.length();
            });

            assertEquals(ERR_VALUE.length(), result);
            assertTrue(errCalled.get());
            assertFalse(okCalled.get());
        }

        @Test
        void acceptsMappersWithBroaderInputAndNarrowerOutput() {
            Result<Integer, String> result = okResult();
            Function<Number, StringBuilder> okMapper = value -> new StringBuilder(value.toString());
            Function<Object, CharSequence> errorMapper = value -> new StringBuilder(value.toString().toUpperCase());

            CharSequence folded = result.fold(okMapper, errorMapper);

            assertEquals("45", folded.toString());
        }
    }
}
