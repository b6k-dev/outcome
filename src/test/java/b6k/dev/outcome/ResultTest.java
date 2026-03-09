package b6k.dev.outcome;

import b6k.dev.outcome.error.Panic;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

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
}
