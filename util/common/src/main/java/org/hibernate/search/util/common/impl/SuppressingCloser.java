/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.impl;

import java.util.function.Function;

import org.hibernate.search.util.common.spi.ClosingOperator;

/**
 * A helper for closing multiple resources and re-throwing a provided exception,
 * {@link Throwable#addSuppressed(Throwable) suppressing} any exceptions caught while closing.
 * <p>
 * This class is not thread safe.
 * <p>
 * This helper is mainly useful when implementing a {@code catch} block where resources must be closed,
 * to make sure that all resources are at least given the chance to close, even if closing one of them fails,
 * and that you can still re-throw the originally caught exception.
 * <p>
 * See the {@link AbstractCloser} superclass for a list of methods
 * allowing to close objects while catching exceptions.
 *
 */
public final class SuppressingCloser extends AbstractCloser<SuppressingCloser, Exception> {

	private final State state;

	public SuppressingCloser(Throwable mainThrowable) {
		state = new State();
		state.addThrowable( this, mainThrowable );
	}

	/**
	 * Close the given {@code closeable} <strong>immediately</strong>,
	 * swallowing any throwable in order to
	 * {@link Throwable#addSuppressed(Throwable) add it as suppressed} to the main throwable.
	 *<p>
	 * See also {@link #push(ClosingOperator, Object)}
	 * for when the object to close does not implement {@link AutoCloseable}.
	 *
	 * @param closeable An {@link AutoCloseable} to close.
	 * @return {@code this}, for method chaining.
	 */
	public SuppressingCloser push(AutoCloseable closeable) {
		return push( AutoCloseable::close, closeable );
	}

	/**
	 * Close the {@code closeable} extracted from {@code objectToExtractFrom} <strong>immediately</strong>,
	 * swallowing any throwable in order to
	 * {@link Throwable#addSuppressed(Throwable) add it as suppressed} to the main throwable.
	 *<p>
	 * See also {@link #push(ClosingOperator, Object)}
	 * for when the object to close does not implement {@link AutoCloseable}.
	 *
	 * @param objectToExtractFrom An object from which to extract the object to close.
	 * @param extract A function to extract an object to close from {@code objectToExtractFrom}. Accepts lambdas
	 * such as {@code MyType::get}.
	 * @return {@code this}, for method chaining.
	 */
	public <T> SuppressingCloser push(T objectToExtractFrom, Function<T, ? extends AutoCloseable> extract) {
		return push( AutoCloseable::close, objectToExtractFrom, extract );
	}

	/**
	 * Close the given {@code closeables} <strong>immediately</strong>,
	 * swallowing any throwable in order to
	 * {@link Throwable#addSuppressed(Throwable) add it as suppressed} to the main throwable.
	 *<p>
	 * See also {@link #pushAll(ClosingOperator, Object[])}
	 * for when the objects to close do not implement {@link AutoCloseable}.
	 *
	 * @param closeables An array of {@link AutoCloseable}s to close.
	 * @return {@code this}, for method chaining.
	 */
	public SuppressingCloser pushAll(AutoCloseable... closeables) {
		return pushAll( AutoCloseable::close, closeables );
	}

	/**
	 * Close the given {@code closeables} <strong>immediately</strong>,
	 * swallowing any throwable in order to
	 * {@link Throwable#addSuppressed(Throwable) add it as suppressed} to the main throwable.
	 *<p>
	 * See also {@link #pushAll(ClosingOperator, Iterable)}
	 * for when the objects to close do not implement {@link AutoCloseable}.
	 *
	 * @param closeables An iterable of {@link AutoCloseable}s to close.
	 * @return {@code this}, for method chaining.
	 */
	public SuppressingCloser pushAll(Iterable<? extends AutoCloseable> closeables) {
		return pushAll( AutoCloseable::close, closeables );
	}

	/**
	 * Close the {@link AutoCloseable} elements extracted from elements of {@code objectToExtractFrom}
	 * <strong>immediately</strong>, swallowing any throwable in order to
	 * {@link Throwable#addSuppressed(Throwable) add it as suppressed} to the main throwable.
	 *<p>
	 * See also {@link #pushAll(ClosingOperator, Iterable, Function)}
	 * for when the objects to close do not implement {@link AutoCloseable}.
	 *
	 * @param objectsToExtractFrom An iterable of objects from which to extract the objects to close.
	 * @param extract A function to extract an object to close from the elements of {@code objectsToExtractFrom}.
	 * Accepts lambdas such as {@code MyType::get}.
	 * @return {@code this}, for method chaining.
	 */
	public <U> SuppressingCloser pushAll(Iterable<? extends U> objectsToExtractFrom, Function<U, AutoCloseable> extract) {
		return pushAll( AutoCloseable::close, objectsToExtractFrom, extract );
	}

	@Override
	State getState() {
		return state;
	}

	@Override
	SuppressingCloser getSelf() {
		return this;
	}
}
