package b6k.dev.outcome;

import b6k.dev.outcome.error.Panic;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
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

    Result<T, E> peek(Consumer<? super T> observer);

    Result<T, E> peekErr(Consumer<? super E> observer);

    Result<T, E> tap(Runnable action);

    <X extends Throwable> T orThrow(Function<? super E, ? extends X> errorMapper) throws X;

    <R> R fold(Function<? super T, ? extends R> okMapper, Function<? super E, ? extends R> errorMapper);


    record Ok<T, E>(T value) implements Result<T, E> {
        public Ok {
            Objects.requireNonNull(value, "value must not be null");
        }

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
            Objects.requireNonNull(supplier, "supplier must not be null");
            return value;
        }

        @Override
        public T unwrapOrElse(Function<? super E, ? extends T> errorMapper) {
            Objects.requireNonNull(errorMapper, "errorMapper must not be null");
            return value;
        }

        @Override
        public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            return Result.ok(tryOrPanic(() -> mapper.apply(this.value), "mapper threw an exception"));
        }

        @Override
        public <U> Result<T, U> mapError(Function<? super E, ? extends U> mapper) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            return this.withErrorType();
        }

        @Override
        public <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            return Objects.requireNonNull(
                    tryOrPanic(() -> mapper.apply(value), "mapper threw an exception"),
                    "mapper result must not be null"
            );
        }

        @Override
        public <E2> Result<T, E2> orElse(Function<? super E, ? extends Result<T, E2>> fallback) {
            Objects.requireNonNull(fallback, "fallback must not be null");
            return this.withErrorType();
        }

        @Override
        public Result<T, E> peek(Consumer<? super T> observer) {
            Objects.requireNonNull(observer, "observer must not be null");
            tryOrPanic(() -> {
                observer.accept(this.value);
                return null;
            }, "observer threw an exception");
            return this;
        }

        @Override
        public Result<T, E> peekErr(Consumer<? super E> observer) {
            Objects.requireNonNull(observer, "observer must not be null");
            return this;
        }

        @Override
        public Result<T, E> tap(Runnable action) {
            Objects.requireNonNull(action, "action must not be null");
            tryOrPanic(() -> {
                action.run();
                return null;
            }, "action threw an exception");
            return this;
        }

        @Override
        public <X extends Throwable> T orThrow(Function<? super E, ? extends X> errorMapper) {
            Objects.requireNonNull(errorMapper, "errorMapper must not be null");
            return this.value;
        }

        @Override
        public <R> R fold(Function<? super T, ? extends R> okMapper, Function<? super E, ? extends R> errorMapper) {
            Objects.requireNonNull(okMapper, "okMapper must not be null");
            Objects.requireNonNull(errorMapper, "errorMapper must not be null");
            return tryOrPanic(() -> okMapper.apply(this.value), "okMapper threw an exception");
        }

        @SuppressWarnings("unchecked")
        public <E2> Result<T, E2> withErrorType() {
            return (Result<T, E2>) this;
        }
    }

    record Err<T, E>(E error) implements Result<T, E> {
        public Err {
            Objects.requireNonNull(error, "error must not be null");
        }

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
            Objects.requireNonNull(supplier, "supplier must not be null");
            return tryOrPanic(supplier::get, "supplier threw an exception");
        }

        @Override
        public T unwrapOrElse(Function<? super E, ? extends T> errorMapper) {
            Objects.requireNonNull(errorMapper, "errorMapper must not be null");
            return tryOrPanic(() -> errorMapper.apply(this.error), "errorMapper threw an exception");
        }

        @Override
        public <U> Result<U, E> map(Function<? super T, ? extends U> mapper) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            return this.withValueType();
        }

        @Override
        public <U> Result<T, U> mapError(Function<? super E, ? extends U> mapper) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            return Result.err(tryOrPanic(() -> mapper.apply(this.error), "mapper threw an exception"));
        }

        @Override
        public <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper) {
            Objects.requireNonNull(mapper, "mapper must not be null");
            return this.withValueType();
        }

        @Override
        public <E2> Result<T, E2> orElse(Function<? super E, ? extends Result<T, E2>> fallback) {
            Objects.requireNonNull(fallback, "fallback must not be null");
            return Objects.requireNonNull(
                    tryOrPanic(() -> fallback.apply(this.error), "fallback threw an exception"),
                    "fallback result must not be null"
            );
        }

        @Override
        public Result<T, E> peek(Consumer<? super T> observer) {
            Objects.requireNonNull(observer, "observer must not be null");
            return this;
        }

        @Override
        public Result<T, E> peekErr(Consumer<? super E> observer) {
            Objects.requireNonNull(observer, "observer must not be null");
            tryOrPanic(() -> {
                observer.accept(this.error);
                return null;
            }, "observer threw an exception");
            return this;
        }

        @Override
        public Result<T, E> tap(Runnable action) {
            Objects.requireNonNull(action, "action must not be null");
            tryOrPanic(() -> {
                action.run();
                return null;
            }, "action threw an exception");
            return this;
        }

        @Override
        public <X extends Throwable> T orThrow(Function<? super E, ? extends X> errorMapper) throws X {
            Objects.requireNonNull(errorMapper, "errorMapper must not be null");
            throw Objects.requireNonNull(
                    tryOrPanic(() -> errorMapper.apply(this.error), "errorMapper threw an exception"),
                    "errorMapper result must not be null"
            );
        }

        @Override
        public <R> R fold(Function<? super T, ? extends R> okMapper, Function<? super E, ? extends R> errorMapper) {
            Objects.requireNonNull(okMapper, "okMapper must not be null");
            Objects.requireNonNull(errorMapper, "errorMapper must not be null");
            return tryOrPanic(() -> errorMapper.apply(this.error), "errorMapper threw an exception");
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
        Objects.requireNonNull(supplier, "supplier must not be null");
        try {
            return Result.ok(supplier.get());
        } catch (Throwable e) {
            return Result.err(e);
        }
    }

    static <T, E> Result<T, E> trying(Supplier<T> supplier, Function<? super Throwable, ? extends E> errorMapper) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        Objects.requireNonNull(errorMapper, "errorMapper must not be null");
        return Result.trying(supplier)
                .mapError(errorMapper);
    }

    static <T, E> Result<T, E> fromNullable(T value, Supplier<? extends E> onNullSupplier) {
        Objects.requireNonNull(onNullSupplier, "onNullSupplier must not be null");
        return Optional.ofNullable(value)
                .<Result<T, E>>map(Result::ok)
                .orElseGet(() -> Result.err(tryOrPanic(onNullSupplier::get, "onNullSupplier threw an exception")));
    }

    static <T, E> Result<T, E> fromOptional(Optional<T> value, Supplier<? extends E> onEmptySupplier) {
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(onEmptySupplier, "onEmptySupplier must not be null");
        return value
                .<Result<T, E>>map(Result::ok)
                .orElseGet(() -> Result.err(tryOrPanic(onEmptySupplier::get, "onEmptySupplier threw an exception")));
    }


}
