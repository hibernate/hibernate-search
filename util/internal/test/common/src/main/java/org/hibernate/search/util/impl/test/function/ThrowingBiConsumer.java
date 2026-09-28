package org.hibernate.search.util.impl.test.function;

@FunctionalInterface
public interface ThrowingBiConsumer<T, U, E extends Throwable> {
	void accept(T t, U u) throws E;
}
