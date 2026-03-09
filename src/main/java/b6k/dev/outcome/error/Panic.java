package b6k.dev.outcome.error;

import java.util.Objects;
import java.util.function.Supplier;

public final class Panic extends RuntimeException {
    public Panic(Throwable cause) {
        super(Objects.requireNonNull(cause, "cause must not be null"));
    }

    public Panic(String message) {
        super(message);
    }

    public Panic(String message, Throwable cause) {
        super(message, Objects.requireNonNull(cause, "cause must not be null"));
    }

    public static <T> T tryOrPanic(Supplier<T> supplier, String message) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new Panic(message, e);
        }
    }

    public static <T> T tryOrPanic(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new Panic(e);
        }
    }
}
