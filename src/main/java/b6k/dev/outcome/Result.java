package b6k.dev.outcome;

import java.util.function.Supplier;

public sealed interface Result<T, E> {
    record Ok<T, E>(T value) implements Result<T, E> {
        @Override
        public T unwrap() {
            return value;
        }

        @Override
        public T unwrap(String message) {
            return value;
        }

        @Override
        public T unwrapOr(T defaultValue) {
            return value;
        }

        @Override
        public T unwrapOrElse(Supplier<? extends T> supplier) {
            return value;
        }
    }
    record Err<T, E>(E error) implements Result<T, E> {
        @Override
        public T unwrap() {
            throw new IllegalStateException("Tried to unwrap an error");
        }

        @Override
        public T unwrap(String message) {
            throw new IllegalStateException(message);
        }

        @Override
        public T unwrapOr(T defaultValue) {
            return defaultValue;
        }

        @Override
        public T unwrapOrElse(Supplier<? extends T> supplier) {
            return supplier.get();
        }
    }

    default boolean isOk() {
        return this instanceof Result.Ok<T,E>;
    }

    default boolean isErr() {
        return this instanceof Result.Err<T,E>;
    }

    T unwrap();
    T unwrap(String message);
    T unwrapOr(T defaultValue);
    T unwrapOrElse(Supplier<? extends T> supplier);

    static <T, E> Result<T, E> ok(T value) {
        return new Result.Ok<>(value);
    }

    static <T, E> Result<T, E> err(E error) {
        return new Result.Err<>(error);
    }
}
