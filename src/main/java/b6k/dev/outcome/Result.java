package b6k.dev.outcome;

import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface Result<T, E> {
    record Ok<T, E>(T value) implements Result<T, E> {
        @Override
        public T unwrap() {
            return value;
        }

        @Override
        public E unwrapError() {
           throw new IllegalStateException("Tried to unwrap error from an ok value");
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

        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return Result.ok(mapper.apply(this.value));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<T, U> mapError(Function<E, U> mapper) {
            return (Result<T, U>) this;
        }
    }

    record Err<T, E>(E error) implements Result<T, E> {
        @Override
        public T unwrap() {
            throw new IllegalStateException("Tried to unwrap an error");
        }

        @Override
        public E unwrapError() {
            return this.error;
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

        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return (Result<U, E>) this;
        }

        @Override
        public <U> Result<T, U> mapError(Function<E, U> mapper) {
            return Result.err(mapper.apply(this.error));
        }
    }

    default boolean isOk() {
        return this instanceof Result.Ok<T,E>;
    }

    default boolean isErr() {
        return this instanceof Result.Err<T,E>;
    }

    T unwrap();
    E unwrapError();
    T unwrap(String message);
    T unwrapOr(T defaultValue);
    T unwrapOrElse(Supplier<? extends T> supplier);
    <U> Result<U, E> map(Function<T, U> mapper);
    <U> Result<T, U> mapError(Function<E, U> mapper);

    static <T, E> Result<T, E> ok(T value) {
        return new Result.Ok<>(value);
    }

    static <T, E> Result<T, E> err(E error) {
        return new Result.Err<>(error);
    }
}
