package b6k.dev.outcome.error;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Runtime exception used to signal misuse of the {@code Result} API or unexpected callback failures.
 *
 * <p>{@code Panic} is thrown when an operation that is expected to be safe cannot proceed, such as unwrapping
 * the wrong {@code Result} variant or when a user-provided callback throws inside helper methods like
 * {@code Result.map(...)}.
 */
public final class Panic extends RuntimeException {
    /**
     * Creates a panic that wraps an underlying cause.
     *
     * @param cause underlying cause of the panic
     */
    public Panic(Throwable cause) {
        super(Objects.requireNonNull(cause, "cause must not be null"));
    }

    /**
     * Creates a panic with a message.
     *
     * @param message panic message
     */
    public Panic(String message) {
        super(message);
    }

    /**
     * Creates a panic with both a message and an underlying cause.
     *
     * @param message panic message
     * @param cause underlying cause of the panic
     */
    public Panic(String message, Throwable cause) {
        super(message, Objects.requireNonNull(cause, "cause must not be null"));
    }

    /**
     * Executes {@code supplier} and converts thrown exceptions into a {@code Panic} with a custom message.
     *
     * <p>Returns the supplied value when execution succeeds. If {@code supplier} throws an {@link Exception}, this
     * method throws a new {@code Panic} wrapping that exception.
     *
     * @param supplier operation to execute
     * @param message panic message used when the supplier throws
     * @param <T> supplied value type
     * @return the supplied value
     * @throws Panic when {@code supplier} throws an exception
     */
    public static <T> T tryOrPanic(Supplier<T> supplier, String message) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new Panic(message, e);
        }
    }

    /**
     * Executes {@code supplier} and converts thrown exceptions into a {@code Panic}.
     *
     * <p>Returns the supplied value when execution succeeds. If {@code supplier} throws an {@link Exception}, this
     * method throws a new {@code Panic} wrapping that exception.
     *
     * @param supplier operation to execute
     * @param <T> supplied value type
     * @return the supplied value
     * @throws Panic when {@code supplier} throws an exception
     */
    public static <T> T tryOrPanic(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new Panic(e);
        }
    }
}
