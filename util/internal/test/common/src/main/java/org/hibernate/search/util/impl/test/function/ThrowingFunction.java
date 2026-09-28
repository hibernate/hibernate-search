package org.hibernate.search.util.impl.test.function;

@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Throwable> {
	R apply(T t) throws E;
}
