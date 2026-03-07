package b6k.dev.outcome;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

public class ResultTest {

    @Test
    void isError() {
        var result = Result.err("Something is wrong");

        assertTrue(result.isErr());
        assertFalse(result.isOk());
    }

    @Test
    void isOk() {
        var result = Result.ok("All good");

        assertTrue(result.isOk());
        assertFalse(result.isErr());
    }

    @Nested
    class Unwrap {
        @Nested
        class Ok {
            @Test
            void returnValue() {
                var value = 45;
                var result = Result.ok(value);

                assertEquals(value, result.unwrap());
            }

            @Test
            void returnValueWithMessage() {
                var value = 45;
                var result = Result.ok(value);

                assertEquals(value, result.unwrap("blah, blah"));
            }
        }

        @Nested
        class Error {
            @Test
            void throwWithDefaultMessage() {
                assertThatThrownBy(() -> Result.err("oops").unwrap())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("Tried to unwrap an error");
            }

            @Test
            void throwWithMessage() {
                var message = "blah, blah";

                assertThatThrownBy(() -> Result.err("oops").unwrap(message))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage(message);
            }
        }


    }
}
