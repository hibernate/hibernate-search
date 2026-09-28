package org.hibernate.search.util.common.spi;

@FunctionalInterface
public interface ClosingOperator<T, E extends Throwable> {

	void close(T objectToClose) throws E;

}
