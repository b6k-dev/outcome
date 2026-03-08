package b6k.dev.outcome;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

public class ResultTest {

    @Nested
    class Ok {
        private static final Integer OK_VALUE = 45;
        private Result<Integer, String> okResult;

        @BeforeEach
        void setup() {
            okResult = Result.ok(OK_VALUE);
        }

        @Test
        void createOkWithValue() {
            var result = Result.ok(12);

            assertTrue(result.isOk());
            assertFalse(result.isErr());
        }

        @Nested
        class Unwrap {

            @Test
            void returnValue() {
                assertEquals(OK_VALUE, okResult.unwrap());
            }

            @Test
            void returnValueIgnoreMessage() {
                assertEquals(OK_VALUE, okResult.unwrap("blah, blah"));
            }

            @Nested
            class UnwrapOr {
                @Test
                void returnValueForOk() {
                    assertEquals(OK_VALUE, okResult.unwrapOr(23));
                }

            }

            @Nested
            class UnwrapOrElse {
                @Test
                void returnValueForOk() {
                    assertEquals(OK_VALUE, okResult.unwrapOrElse(() -> 44));
                }
            }
        }

        @Nested
        class Map {

            @Test
            void transformValue() {
                var result = Result.ok(3).map(it -> it * 3);

                assertEquals(9, result.unwrap());
            }

            @Test
            void acceptsMapperWithBroaderInputAndNarrowerOutput() {
                Result<Integer, String> result = Result.ok(OK_VALUE);
                Function<Number, StringBuilder> mapper = value -> new StringBuilder(value.toString());

                Result<CharSequence, String> mapped = result.map(mapper);

                assertEquals("45", mapped.unwrap().toString());
            }

            @Nested
            class MapError {
                @Test
                void preservesValue() {
                    var result = okResult.mapError(String::toUpperCase);

                    assertEquals(OK_VALUE, result.unwrap());
                }
            }
        }

    }

    @Nested
    class Err {
        private static final String ERR_VALUE = "oops";

        private Result<Integer, String> errResult;

        @BeforeEach
        void setup() {
            errResult = Result.err(ERR_VALUE);
        }

        @Test
        void createErrWithValue() {
            var result = Result.err("oops");


            assertTrue(result.isErr());
            assertFalse(result.isOk());
        }

        @Nested
        class Unwrap {
            @Test
            void throwWithDefaultMessage() {
                assertThatThrownBy(() -> errResult.unwrap()).isInstanceOf(IllegalStateException.class).hasMessage("Tried to unwrap an error");
            }

            @Test
            void throwWithMessage() {
                var message = "blah, blah";

                assertThatThrownBy(() -> errResult.unwrap(message)).isInstanceOf(IllegalStateException.class).hasMessage(message);
            }

            @Nested
            class UnwrapOr {

                @Test
                void returnDefaultForErr() {
                    var fallback = 23;
                    assertEquals(fallback, errResult.unwrapOr(fallback));
                }
            }

            @Nested
            class UnwrapOrElse {

                @Test
                void returnDefaultForErr() {
                    var fallback = 41;
                    assertEquals(fallback, errResult.unwrapOrElse(() -> 41));
                }
            }
        }

        @Nested
        class Map {
            @Test
            void preserveError() {
                var result = Result.<Integer, String>err("oops").map(it -> it * 3);

                assertTrue(result.isErr());
            }

            @Nested
            class MapError {
                @Test
                void transformsError() {
                    var result = errResult.mapError(String::toUpperCase);

                    assertEquals(ERR_VALUE.toUpperCase(), result.unwrapError());
                }

                @Test
                void acceptsMapperWithBroaderInputAndNarrowerOutput() {
                    Result<Integer, String> result = Result.err(ERR_VALUE);
                    Function<Object, StringBuilder> mapper = value -> new StringBuilder(value.toString().toUpperCase());

                    Result<Integer, CharSequence> mapped = result.mapError(mapper);

                    assertEquals(ERR_VALUE.toUpperCase(), mapped.unwrapError().toString());
                }
            }
        }
    }
}
