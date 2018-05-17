/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.common;

/**
 * A base class implementing the logic behind {@link Closer} and {@link SuppressingCloser}.
 *
 * @param <E> The supertype of exceptions this object can catch.
 *
 * @author Yoann Rodiere
 */
public abstract class AbstractCloser<S, E extends Exception> {

	/**
	 * If the given {@code objectToClose} is non-null,
	 * execute the given close {@code operator} <strong>immediately</strong> on {@code objectToClose},
	 * swallowing any throwable in order to
	 * {@link Throwable#addSuppressed(Throwable) add it as suppressed} to a previously caught throwable,
	 * or to re-throw it later.
	 * @param operator An operator to close {@code objectToClose}. Accepts lambdas
	 * such as {@code MyType::close}.
	 * @param objectToClose An object to close.
	 * @return {@code this}, for method chaining.
	 */
	public <T> S push(ClosingOperator<T, ? extends E> operator, T objectToClose) {
		try {
			if ( objectToClose != null ) {
				operator.close( objectToClose );
			}
		}
		catch (Throwable t) {
			getState().addThrowable( this, t );
		}
		return getSelf();
	}

	/**
	 * Execute the given close {@code operator} <strong>immediately</strong> on
	 * each element of the given iterable, swallowing any throwable in order to
	 * {@link Throwable#addSuppressed(Throwable) add it as suppressed} to a previously caught throwable,
	 * or to re-throw it later.
	 * @param operator An operator to close each element in {@code objectsToClose}. Accepts lambdas
	 * such as {@code MyType::close}.
	 * @param objectsToClose An iterable of objects to close.
	 * @return {@code this}, for method chaining.
	 */
	public <T> S pushAll(ClosingOperator<T, ? extends E> operator, Iterable<T> objectsToClose) {
		for ( T objectToClose : objectsToClose ) {
			push( operator, objectToClose );
		}
		return getSelf();
	}

	/**
	 * Execute the given close {@code operator} <strong>immediately</strong> on
	 * each element of the given array, swallowing any throwable in order to
	 * {@link Throwable#addSuppressed(Throwable) add it as suppressed} to a previously caught throwable,
	 * or to re-throw it later.
	 * @param operator An operator to close each element in {@code objectsToClose}. Accepts lambdas
	 * such as {@code MyType::close}.
	 * @param objectsToClose An array of objects to close.
	 * @return {@code this}, for method chaining.
	 */
	@SafeVarargs
	public final <T> S pushAll(ClosingOperator<T, ? extends E> operator, T ... objectsToClose) {
		for ( T objectToClose : objectsToClose ) {
			push( operator, objectToClose );
		}
		return getSelf();
	}

	abstract S getSelf();

	abstract State getState();

	/**
	 * This is implemented in a separate class so that multiple {@link Closer}s
	 * can share the same state, allowing them to elect a single "first throwable".
	 *
	 * @see Closer#split()
	 */
	static class State {
		AbstractCloser<?, ?> firstThrower;
		Throwable firstThrowable;

		void addThrowable(AbstractCloser<?, ?> source, Throwable throwable) {
			if ( firstThrowable == null ) {
				firstThrowable = throwable;
				firstThrower = source;
			}
			else {
				firstThrowable.addSuppressed( throwable );
			}
		}
	}

}
