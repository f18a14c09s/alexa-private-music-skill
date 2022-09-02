package f18a14c09s.util;

@FunctionalInterface
public interface ThrowingFunction<E, R, T extends Throwable> {
    R apply(E arg) throws T;
}
