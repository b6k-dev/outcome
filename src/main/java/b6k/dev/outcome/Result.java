package b6k.dev.outcome;

import b6k.dev.outcome.error.Panic;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static b6k.dev.outcome.error.Panic.tryOrPanic;

/**
 * Represents the outcome of an operation that can either succeed with a value or fail with an error.
 *
 * <p>A {@code Result} is either {@link Ok} containing a success value of type {@code T}, or {@link Err}
 * containing an error value of type {@code E}. It provides transformation and recovery methods that make
 * success and error flows explicit without relying on exceptions for expected failures.
 *
 * @param <T> success value type
 * @param <E> error value type
 */
public sealed interface Result<T, E> {
    /**
     * Transforms the success value.
     *
     * <p>On {@link Ok}, applies {@code mapper} to the wrapped value and returns a new {@code Ok}. On
     * {@link Err}, returns the original error unchanged. If {@code mapper} throws, this method panics.
     *
     * @param mapper function applied to the success value
     * @param <U> mapped success value type
     * @return a mapped result
     */
    <U> Result<U, E> map(Function<? super T, ? extends U> mapper);

    /**
     * Transforms the error value.
     *
     * <p>On {@link Err}, applies {@code mapper} to the wrapped error and returns a new {@code Err}. On
     * {@link Ok}, returns the original success value unchanged. If {@code mapper} throws, this method panics.
     *
     * @param mapper function applied to the error value
     * @param <U> mapped error value type
     * @return a mapped result
     */
    <U> Result<T, U> mapError(Function<? super E, ? extends U> mapper);

    /**
     * Returns {@code true} when this result is {@link Ok}.
     *
     * @return whether this result is successful
     */
    default boolean isOk() {
        return this instanceof Result.Ok<T, E>;
    }

    /**
     * Returns {@code true} when this result is {@link Err}.
     *
     * @return whether this result is an error
     */
    default boolean isErr() {
        return this instanceof Result.Err<T, E>;
    }

    /**
     * Returns the success value.
     *
     * <p>On {@link Ok}, returns the wrapped value. On {@link Err}, panics.
     *
     * @return the success value
     */
    T unwrap();

    /**
     * Returns the error value.
     *
     * <p>On {@link Err}, returns the wrapped error. On {@link Ok}, panics.
     *
     * @return the error value
     */
    E unwrapError();

    /**
     * Returns the success value or panics with a custom message.
     *
     * <p>On {@link Ok}, returns the wrapped value. On {@link Err}, panics with {@code message}.
     *
     * @param message panic message used when this result is an error
     * @return the success value
     */
    T unwrap(String message);

    /**
     * Returns the success value or a default value.
     *
     * <p>On {@link Ok}, returns the wrapped value. On {@link Err}, returns {@code defaultValue}.
     *
     * @param defaultValue fallback value for an error result
     * @return the success value or {@code defaultValue}
     */
    T unwrapOr(T defaultValue);

    /**
     * Returns the success value or computes a fallback value lazily.
     *
     * <p>On {@link Ok}, returns the wrapped value and does not call {@code supplier}. On {@link Err}, calls
     * {@code supplier} and returns its result. If {@code supplier} throws, this method panics.
     *
     * @param supplier fallback supplier used only for an error result
     * @return the success value or a supplied fallback
     */
    T unwrapOrElse(Supplier<? extends T> supplier);

    /**
     * Returns the success value or computes a fallback value from the error.
     *
     * <p>On {@link Ok}, returns the wrapped value and does not call {@code errorMapper}. On {@link Err}, calls
     * {@code errorMapper} with the wrapped error and returns its result. If {@code errorMapper} throws, this
     * method panics.
     *
     * @param errorMapper fallback mapper used only for an error result
     * @return the success value or a mapped fallback
     */
    T unwrapOrElse(Function<? super E, ? extends T> errorMapper);

    /**
     * Chains another result-producing operation.
     *
     * <p>On {@link Ok}, applies {@code mapper} and returns its result. On {@link Err}, returns the original
     * error unchanged. Use this when the mapping function already returns a {@code Result}. If {@code mapper}
     * throws or returns {@code null}, this method panics.
     *
     * @param mapper function applied to the success value
     * @param <U> mapped success value type
     * @return the next result in the chain
     */
    <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> mapper);

    /**
     * Recovers from an error by switching to another result.
     *
     * <p>On {@link Err}, applies {@code fallback} and returns its result. On {@link Ok}, returns the original
     * success value unchanged. If {@code fallback} throws or returns {@code null}, this method panics.
     *
     * @param fallback recovery function applied to the error value
     * @param <E2> mapped error type
     * @return the original success or a recovered result
     */
    <E2> Result<T, E2> orElse(Function<? super E, ? extends Result<T, E2>> fallback);

    /**
     * Observes the success value for side effects.
     *
     * <p>On {@link Ok}, calls {@code observer} with the wrapped value and returns this result. On {@link Err},
     * does nothing and returns this result. If {@code observer} throws, this method panics.
     *
     * @param observer callback invoked for a success value
     * @return this result
     */
    Result<T, E> peek(Consumer<? super T> observer);

    /**
     * Observes the error value for side effects.
     *
     * <p>On {@link Err}, calls {@code observer} with the wrapped error and returns this result. On {@link Ok},
     * does nothing and returns this result. If {@code observer} throws, this method panics.
     *
     * @param observer callback invoked for an error value
     * @return this result
     */
    Result<T, E> peekErr(Consumer<? super E> observer);

    /**
     * Runs a side effect regardless of whether this result is success or error.
     *
     * <p>Always calls {@code action} and then returns this result. If {@code action} throws, this method
     * panics.
     *
     * @param action side effect to run
     * @return this result
     */
    Result<T, E> tap(Runnable action);

    /**
     * Returns the success value or throws an exception derived from the error.
     *
     * <p>On {@link Ok}, returns the wrapped value. On {@link Err}, maps the error to an exception and throws it.
     * If {@code errorMapper} throws or returns {@code null}, this method panics.
     *
     * @param errorMapper maps the error value to the exception to throw
     * @param <X> exception type thrown for an error result
     * @return the success value
     * @throws X when this result is an error
     */
    <X extends Throwable> T orThrow(Function<? super E, ? extends X> errorMapper) throws X;

    /**
     * Collapses this result into a single value.
     *
     * <p>On {@link Ok}, applies {@code okMapper}. On {@link Err}, applies {@code errorMapper}. If the invoked
     * mapper throws, this method panics.
     *
     * @param okMapper function used for a success value
     * @param errorMapper function used for an error value
     * @param <R> folded result type
     * @return the value produced by the matching mapper
     */
    <R> R fold(Function<? super T, ? extends R> okMapper, Function<? super E, ? extends R> errorMapper);


    /**
     * Successful {@link Result} variant.
     *
     * @param value wrapped success value
     * @param <T> success value type
     * @param <E> error value type
     */
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

    /**
     * Error {@link Result} variant.
     *
     * @param error wrapped error value
     * @param <T> success value type
     * @param <E> error value type
     */
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

    /**
     * Creates a successful result.
     *
     * @param value success value
     * @param <T> success value type
     * @param <E> error value type
     * @return an {@link Ok} containing {@code value}
     */
    static <T, E> Result<T, E> ok(T value) {
        return new Result.Ok<>(value);
    }

    /**
     * Creates an error result.
     *
     * @param error error value
     * @param <T> success value type
     * @param <E> error value type
     * @return an {@link Err} containing {@code error}
     */
    static <T, E> Result<T, E> err(E error) {
        return new Result.Err<>(error);
    }

    /**
     * Executes {@code supplier} and captures either its value or thrown exception.
     *
     * <p>Returns {@link Ok} when {@code supplier} completes normally, or {@link Err} containing the thrown
     * {@link Throwable} when it fails.
     *
     * @param supplier operation to execute
     * @param <T> success value type
     * @return a result containing either the produced value or the thrown exception
     */
    static <T> Result<T, Throwable> trying(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        try {
            return Result.ok(supplier.get());
        } catch (Throwable e) {
            return Result.err(e);
        }
    }

    /**
     * Executes {@code supplier} and maps any thrown exception into an error value.
     *
     * <p>Returns {@link Ok} when {@code supplier} completes normally, or {@link Err} containing the mapped error
     * when it throws.
     *
     * @param supplier operation to execute
     * @param errorMapper maps a thrown exception into an error value
     * @param <T> success value type
     * @param <E> error value type
     * @return a result containing either the produced value or the mapped error
     */
    static <T, E> Result<T, E> trying(Supplier<T> supplier, Function<? super Throwable, ? extends E> errorMapper) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        Objects.requireNonNull(errorMapper, "errorMapper must not be null");
        return Result.trying(supplier)
                .mapError(errorMapper);
    }

    /**
     * Converts a nullable value into a result.
     *
     * <p>Returns {@link Ok} when {@code value} is non-null. Returns {@link Err} with a value produced by
     * {@code onNullSupplier} when {@code value} is {@code null}. If {@code onNullSupplier} throws, this method
     * panics.
     *
     * @param value value to wrap
     * @param onNullSupplier supplies the error value when {@code value} is {@code null}
     * @param <T> success value type
     * @param <E> error value type
     * @return a result representing the nullable value
     */
    static <T, E> Result<T, E> fromNullable(T value, Supplier<? extends E> onNullSupplier) {
        Objects.requireNonNull(onNullSupplier, "onNullSupplier must not be null");
        return Optional.ofNullable(value)
                .<Result<T, E>>map(Result::ok)
                .orElseGet(() -> Result.err(tryOrPanic(onNullSupplier::get, "onNullSupplier threw an exception")));
    }

    /**
     * Converts an {@link Optional} into a result.
     *
     * <p>Returns {@link Ok} when {@code value} is present. Returns {@link Err} with a value produced by
     * {@code onEmptySupplier} when {@code value} is empty. If {@code onEmptySupplier} throws, this method
     * panics.
     *
     * @param value optional value to wrap
     * @param onEmptySupplier supplies the error value when {@code value} is empty
     * @param <T> success value type
     * @param <E> error value type
     * @return a result representing the optional value
     */
    static <T, E> Result<T, E> fromOptional(Optional<T> value, Supplier<? extends E> onEmptySupplier) {
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(onEmptySupplier, "onEmptySupplier must not be null");
        return value
                .<Result<T, E>>map(Result::ok)
                .orElseGet(() -> Result.err(tryOrPanic(onEmptySupplier::get, "onEmptySupplier threw an exception")));
    }


}
