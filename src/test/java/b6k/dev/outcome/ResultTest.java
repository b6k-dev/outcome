package b6k.dev.outcome;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
    }
}
