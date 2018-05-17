/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.common;

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
 * @author Yoann Rodiere
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
	public SuppressingCloser pushAll(AutoCloseable ... closeables) {
		return pushAll( AutoCloseable::close, closeables );
	}

	/**
	 * Close the given {@code closeables} <strong>immediately</strong>,
	 * swallowing any throwable in order to
	 * {@link Throwable#addSuppressed(Throwable) add it as suppressed} to the main throwable.
	 *<p>
	 * See also {@link #pushAll(ClosingOperator, Object[])}
	 * for when the objects to close do not implement {@link AutoCloseable}.
	 *
	 * @param closeables An iterable of {@link AutoCloseable}s to close.
	 * @return {@code this}, for method chaining.
	 */
	public SuppressingCloser pushAll(Iterable<? extends AutoCloseable> closeables) {
		return pushAll( AutoCloseable::close, closeables );
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
