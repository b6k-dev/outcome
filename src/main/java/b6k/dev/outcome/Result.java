package b6k.dev.outcome;

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
    }

    default boolean isOk() {
        return this instanceof Result.Ok<T,E>;
    }

    default boolean isErr() {
        return this instanceof Result.Err<T,E>;
    }

    T unwrap();
    T unwrap(String message);

    static <T, E> Result<T, E> ok(T value) {
        return new Result.Ok<>(value);
    }

    static <T, E> Result<T, E> err(E error) {
        return new Result.Err<>(error);
    }
}
