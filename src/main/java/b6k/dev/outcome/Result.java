package b6k.dev.outcome;

import b6k.dev.outcome.error.Panic;

import java.util.function.Function;
import java.util.function.Supplier;

import static b6k.dev.outcome.error.Panic.tryOrPanic;

public sealed interface Result<T, E> {
    <U> Result<U, E> map(Function<? super T, ? extends U> mapper);

    <U> Result<T, U> mapError(Function<? super E, ? extends U> mapper);

    default boolean isOk() {
        return this instanceof Result.Ok<T, E>;
    }

    default boolean isErr() {
        return this instanceof Result.Err<T, E>;
    }

    T unwrap();

    E unwrapError();

    T unwrap(String message);

    T unwrapOr(T defaultValue);

    T unwrapOrElse(Supplier<? extends T> supplier);

    <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper);

    <E2> Result<T, E2> orElse(Function<? super E, ? extends Result<T, E2>> fallback);

    record Ok<T, E>(T value) implements Result<T, E> {
        @Override
        public T unwrap() {
            return value;
        }

        @Override
        public E unwrapError() {
           throw new Panic("Tried to unwrap error from an ok value");
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
        public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
            return tryOrPanic(() -> Result.ok(mapper.apply(this.value)), "mapper threw an exception");
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<T, U> mapError(Function<? super E, ? extends U> mapper) {
            return (Result<T, U>) this;
        }

        @Override
        public <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper) {
            return tryOrPanic(() -> mapper.apply(value), "mapper threw an exception");
        }

        @Override
        @SuppressWarnings("unchecked")
        public <E2> Result<T, E2> orElse(Function<? super E, ? extends Result<T, E2>> fallback) {
            return (Result<T, E2>) this;
        }
    }

    record Err<T, E>(E error) implements Result<T, E> {
        @Override
        public T unwrap() {
            throw new Panic("Tried to unwrap an error");
        }

        @Override
        public E unwrapError() {
            return this.error;
        }

        @Override
        public T unwrap(String message) {
            throw new Panic(message);
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
        public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
            return (Result<U, E>) this;
        }

        @Override
        public <U> Result<T, U> mapError(Function<? super E, ? extends U> mapper) {
            return tryOrPanic(() -> Result.err(mapper.apply(this.error)), "mapper threw an exception");
        }

        @Override
        @SuppressWarnings("unchecked")
        public <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper) {
            return (Result<U, E>) this;
        }

        @Override
        public <E2> Result<T, E2> orElse(Function<? super E, ? extends Result<T, E2>> fallback) {
            return tryOrPanic(() -> fallback.apply(this.error), "fallback threw an exception");
        }
    }

    static <T, E> Result<T, E> ok(T value) {
        return new Result.Ok<>(value);
    }

    static <T, E> Result<T, E> err(E error) {
        return new Result.Err<>(error);
    }

    static <T> Result<T, Throwable> trying(Supplier<T> supplier) {
        try {
            return Result.ok(supplier.get());
        } catch (Throwable e) {
            return Result.err(e);
        }
    }

    static <T, E> Result<T, E> trying(Supplier<T> supplier, Function<? super Throwable, ? extends E> errorMapper) {
        return Result.trying(supplier)
                .mapError(errorMapper);
    }
}
