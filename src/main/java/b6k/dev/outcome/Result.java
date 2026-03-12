package b6k.dev.outcome;

import b6k.dev.outcome.error.Panic;

import java.util.Optional;
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
    T unwrapOrElse(Function<? super E, ? extends T> errorMapper);

    <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper);

    <E2> Result<T, E2> orElse(Function<? super E, ? extends Result<T, E2>> fallback);

    <X extends Throwable> T orThrow(Function<? super E, ? extends X> errorMapper) throws X;

    <R> R fold(Function<? super T, ? extends R> okMapper, Function<? super E, ? extends R> errorMapper);


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
        public T unwrapOrElse(Function<? super E, ? extends T> errorMapper) {
            return value;
        }

        @Override
        public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
            return tryOrPanic(() -> Result.ok(mapper.apply(this.value)), "mapper threw an exception");
        }

        @Override
        public <U> Result<T, U> mapError(Function<? super E, ? extends U> mapper) {
            return this.withErrorType();
        }

        @Override
        public <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper) {
            return tryOrPanic(() -> mapper.apply(value), "mapper threw an exception");
        }

        @Override
        public <E2> Result<T, E2> orElse(Function<? super E, ? extends Result<T, E2>> fallback) {
            return this.withErrorType();
        }

        @Override
        public <X extends Throwable> T orThrow(Function<? super E, ? extends X> errorMapper) {
            return this.value;
        }

        @Override
        public <R> R fold(Function<? super T, ? extends R> okMapper, Function<? super E, ? extends R> errorMapper) {
            return okMapper.apply(this.value);
        }

        @SuppressWarnings("unchecked")
        public <E2> Result<T, E2> withErrorType() {
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
        public T unwrapOrElse(Function<? super E, ? extends T> errorMapper) {
            return errorMapper.apply(this.error);
        }

        @Override
        public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
            return this.withValueType();
        }

        @Override
        public <U> Result<T, U> mapError(Function<? super E, ? extends U> mapper) {
            return tryOrPanic(() -> Result.err(mapper.apply(this.error)), "mapper threw an exception");
        }

        @Override
        public <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper) {
            return this.withValueType();
        }

        @Override
        public <E2> Result<T, E2> orElse(Function<? super E, ? extends Result<T, E2>> fallback) {
            return tryOrPanic(() -> fallback.apply(this.error), "fallback threw an exception");
        }

        @Override
        public <X extends Throwable> T orThrow(Function<? super E, ? extends X> errorMapper) throws X {
            throw errorMapper.apply(this.error);
        }

        @Override
        public <R> R fold(Function<? super T, ? extends R> okMapper, Function<? super E, ? extends R> errorMapper) {
            return errorMapper.apply(this.error);
        }

        @SuppressWarnings("unchecked")
        public <T2> Result<T2, E> withValueType() {
            return (Result<T2, E>) this;
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

    static <T, E> Result<T, E> fromNullable(T value, Supplier<? extends E> onNullSupplier) {
        return Optional.ofNullable(value)
                .<Result<T, E>>map(Result::ok)
                .orElseGet(() -> Result.err(onNullSupplier.get()));
    }

    static <T, E> Result<T, E> fromOptional(Optional<T> value, Supplier<? extends E> onEmptySupplier) {
        return value
                .<Result<T, E>>map(Result::ok)
                .orElseGet(() -> Result.err(onEmptySupplier.get()));
    }


}
