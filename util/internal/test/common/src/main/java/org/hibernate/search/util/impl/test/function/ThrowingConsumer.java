package org.hibernate.search.util.impl.test.function;

@FunctionalInterface
public interface ThrowingConsumer<T, E extends Throwable> {
	void accept(T t) throws E;
}
